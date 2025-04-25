package com.example.rongame.ai;

import com.example.rongame.model.BitBoardRepresentation;
import com.example.rongame.model.PentagoModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * בינה מלאכותית
 */
public class PentagoAI {

    // ========================
    // 1. קבועים והגדרות
    // ========================

    /**
     * מצבי המשחק האפשריים של ה-AI
     */
    public enum AIState {
        OFFENSE,         // מצב התקפה - יצירת רצף או השלמת רצף
        DEFENSE,         // מצב הגנה - חסימת האויב
        CONTROL_CENTER,  // מצב שליטה במרכז - תפיסת עמדות מרכזיות
        CONTROL_CORNERS, // מצב שליטה בפינות - תפיסת פינות אסטרטגיות
        BUILD_PATTERN    // מצב בניית דפוס אסטרטגי
    }

    // קבועים למשחק
    private static final int WIN_LENGTH = 5;
    private static final int BOARD_SIZE = 6;
    private static final int QUADRANT_SIZE = 3;

    // משקלים להערכת עמדה
    private static final int LINE_4_SCORE = 1000;
    private static final int LINE_3_SCORE = 100;
    private static final int LINE_2_SCORE = 10;
    private static final int BLOCK_SCORE = 150;
    private static final int CENTER_SCORE = 8;
    private static final int CORNER_SCORE = 5;
    private static final int EDGE_SCORE = 2;

    // כיווני בדיקה (שורה, עמודה, אלכסון ימין, אלכסון שמאל)
    private static final int[][] DIRECTIONS = {
            {0, 1}, {1, 0}, {1, 1}, {1, -1}
    };

    // מיקומים מרכזיים על הלוח
    private static final int[][] CENTER_POSITIONS = {
            {2, 2}, {2, 3}, {3, 2}, {3, 3},
            {1, 1}, {1, 4}, {4, 1}, {4, 4},
            {1, 2}, {1, 3}, {2, 1}, {3, 1}, {4, 2}, {4, 3}, {2, 4}, {3, 4}
    };

    // מיקומי פינות
    private static final int[][] CORNER_POSITIONS = {
            {0, 0}, {0, 5}, {5, 0}, {5, 5}
    };

    // מיקומים בקרבת פינות
    private static final int[][] NEAR_CORNER_POSITIONS = {
            {0, 1}, {1, 0}, {1, 1}, {0, 4}, {1, 5}, {1, 4},
            {4, 0}, {5, 1}, {4, 1}, {4, 5}, {5, 4}, {4, 4}
    };

    // מרכז הלוח - 4 משבצות מרכזיות
    private static final int[][] CENTER_SQUARES = {
            {2, 2}, {2, 3}, {3, 2}, {3, 3}
    };

    // אזורים חשובים במשחק
    private static final int[][] IMPORTANT_AREAS = {
            {1, 1}, {1, 4}, {4, 1}, {4, 4}, // מרכזי הרביעים
            {2, 2}, {2, 3}, {3, 2}, {3, 3}  // מרכז הלוח
    };

    // ========================
    // 2. מחלקות פנימיות
    // ========================

    /**
     * מחלקה המייצגת תבנית או איום על הלוח
     */
    private class PatternThreat {
        int player;            // מי יוצר את האיום (0 או 1)
        int count;             // כמה כלים יש ברצף
        List<int[]> positions; // מיקומי הכלים ברצף
        List<int[]> openEnds;  // מיקומים פנויים להשלמת הרצף
        int direction;         // כיוון הרצף (שורה=0, עמודה=1, אלכסון=2, אלכסון נגדי=3)
        int score;             // ציון האיום

        PatternThreat(int player, int count) {
            this.player = player;
            this.count = count;
            this.positions = new ArrayList<>();
            this.openEnds = new ArrayList<>();
            this.score = 0;
        }
    }

    /**
     * מחלקה המייצגת מהלך אפשרי עם הערכת ערך
     */
    private class MoveEvaluation implements Comparable<MoveEvaluation> {
        int[] move;
        int score;

        MoveEvaluation(int[] move, int score) {
            this.move = move;
            this.score = score;
        }

        @Override
        public int compareTo(MoveEvaluation other) {
            return Integer.compare(other.score, this.score); // סידור בסדר יורד
        }
    }

    // ========================
    // 3. שדות המחלקה
    // ========================

    // מודל המשחק ומצב נוכחי
    private PentagoModel model;
    private AIState currentState;

    // שחקנים
    private int playerNumber;
    private int opponentNumber;

    // ניהול משחק
    private Random random;
    private int turnCount;

    // מידע על הלוח
    private int[][] positionWeights;
    private Map<String, List<int[]>> strategicPatterns;
    private List<PatternThreat> currentThreats;

    // ========================
    // 4. בנאי ואתחול
    // ========================

    /**
     * בנאי למחלקת PentagoAI
     * @param model מודל המשחק
     */
    public PentagoAI(PentagoModel model) {
        this.model = model;
        this.playerNumber = 1; // ברירת מחדל - שחקן 1 (אדום)
        this.opponentNumber = 0;
        this.random = new Random();
        this.currentState = AIState.CONTROL_CENTER; // מצב התחלתי
        this.turnCount = 0;
        this.currentThreats = new ArrayList<>();

        initializePositionWeights();
        initializeStrategicPatterns();
    }

    /**
     * אתחול מטריצת משקלים לעמדות על הלוח
     */
    private void initializePositionWeights() {
        positionWeights = new int[BOARD_SIZE][BOARD_SIZE];

        // מרכז הלוח - בעל הערך הגבוה ביותר
        positionWeights[2][2] = CENTER_SCORE + 2;
        positionWeights[2][3] = CENTER_SCORE + 2;
        positionWeights[3][2] = CENTER_SCORE + 2;
        positionWeights[3][3] = CENTER_SCORE + 2;

        // משקלים גבוהים למרכזי הרביעים
        positionWeights[1][1] = CENTER_SCORE;
        positionWeights[1][4] = CENTER_SCORE;
        positionWeights[4][1] = CENTER_SCORE;
        positionWeights[4][4] = CENTER_SCORE;

        // פינות
        positionWeights[0][0] = CORNER_SCORE;
        positionWeights[0][5] = CORNER_SCORE;
        positionWeights[5][0] = CORNER_SCORE;
        positionWeights[5][5] = CORNER_SCORE;

        // נקודות חיבור בין רביעים
        positionWeights[2][0] = EDGE_SCORE + 2;
        positionWeights[3][0] = EDGE_SCORE + 2;
        positionWeights[2][5] = EDGE_SCORE + 2;
        positionWeights[3][5] = EDGE_SCORE + 2;
        positionWeights[0][2] = EDGE_SCORE + 2;
        positionWeights[0][3] = EDGE_SCORE + 2;
        positionWeights[5][2] = EDGE_SCORE + 2;
        positionWeights[5][3] = EDGE_SCORE + 2;

        // שאר הקצוות
        for (int i = 1; i < 5; i++) {
            if (positionWeights[0][i] == 0) positionWeights[0][i] = EDGE_SCORE;
            if (positionWeights[5][i] == 0) positionWeights[5][i] = EDGE_SCORE;
            if (positionWeights[i][0] == 0) positionWeights[i][0] = EDGE_SCORE;
            if (positionWeights[i][5] == 0) positionWeights[i][5] = EDGE_SCORE;
        }

        // מילוי שאר הלוח בערכים הדרגתיים
        for (int i = 1; i < 5; i++) {
            for (int j = 1; j < 5; j++) {
                if (positionWeights[i][j] == 0) {
                    // מרחק ממרכז הלוח
                    double distanceFromCenter = Math.sqrt(Math.pow((i - 2.5), 2) + Math.pow((j - 2.5), 2));
                    positionWeights[i][j] = (int)(6 - distanceFromCenter);
                }
            }
        }
    }

    /**
     * אתחול דפוסים אסטרטגיים
     */
    private void initializeStrategicPatterns() {
        strategicPatterns = new HashMap<>();

        // דפוס אלכסוני ראשי
        strategicPatterns.put("mainDiagonal", createPositionList(
                new int[][]{{0, 0}, {1, 1}, {2, 2}, {3, 3}, {4, 4}, {5, 5}}
        ));

        // דפוס אלכסון משני
        strategicPatterns.put("antiDiagonal", createPositionList(
                new int[][]{{0, 5}, {1, 4}, {2, 3}, {3, 2}, {4, 1}, {5, 0}}
        ));

        // דפוס "X"
        strategicPatterns.put("xPattern", createPositionList(
                new int[][]{{1, 1}, {2, 2}, {3, 3}, {4, 4}, {1, 4}, {2, 3}, {3, 2}, {4, 1}}
        ));
    }

    /**
     * יוצר רשימת מיקומים מערך דו-מימדי
     */
    private List<int[]> createPositionList(int[][] positions) {
        List<int[]> result = new ArrayList<>();
        for (int[] pos : positions) {
            result.add(pos);
        }
        return result;
    }

    // ========================
    // 5. ממשק ציבורי
    // ========================

    /**
     * ביצוע מהלך של הנחת כלי על הלוח
     * @return מערך עם [שורה, עמודה] של המהלך
     */
    public int[] makeMove() {
        turnCount++;
        updateThreats();
        determineState();

        int[] move;

        // בחירת המהלך המתאים למצב הנוכחי
        switch (currentState) {
            case OFFENSE:
                move = getOffensiveMove();
                break;
            case DEFENSE:
                move = getDefensiveMove();
                break;
            case CONTROL_CENTER:
                move = getCenterControlMove();
                break;
            case CONTROL_CORNERS:
                move = getCornerControlMove();
                break;
            case BUILD_PATTERN:
                move = getPatternBuildingMove();
                break;
            default:
                move = getStrategicMove();
                break;
        }

        return move;
    }

    /**
     * ביצוע סיבוב של רביע בלוח
     * @return מערך עם [רביע (0-3), כיוון (0=נגד השעון, 1=עם השעון)]
     */
    public int[] makeRotation() {
        updateThreats();

        // בדיקת סיבובים אסטרטגיים
        int[] winningRotation = findWinningRotation();
        if (winningRotation != null) {
            return winningRotation;
        }

        int[] blockingRotation = findBlockingRotation();
        if (blockingRotation != null) {
            return blockingRotation;
        }

        return findStrategicRotation();
    }

    /**
     * הגדרת מספר השחקן
     * @param playerNumber מספר השחקן (0 או 1)
     */
    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
        this.opponentNumber = 1 - playerNumber;
    }

    /**
     * איפוס מונה טורנים (למשחק חדש)
     */
    public void resetTurnCount() {
        this.turnCount = 0;
        this.currentThreats.clear();
    }

    /**
     * עדכון התייחסות למודל חדש
     * @param model מודל המשחק
     */
    public void setModel(PentagoModel model) {
        this.model = model;
    }

    // ========================
    // 6. ניהול מצבי FSM
    // ========================

    /**
     * עדכון מצב ה-FSM בהתבסס על מצב הלוח הנוכחי
     */
    private void determineState() {
        // בדיקת איומים וקביעת מצב בהתאם לעדיפויות

        // עדיפות 1: איתור ניצחון מיידי
        if (hasWinningMove(playerNumber)) {
            currentState = AIState.OFFENSE;
            return;
        }

        // עדיפות 2: חסימת ניצחון של היריב
        if (hasWinningMove(opponentNumber)) {
            currentState = AIState.DEFENSE;
            return;
        }

        // עדיפות 3: בתחילת המשחק, התמקד במרכז או בפינות
        if (turnCount <= 3) {
            currentState = isCenterAvailable() ?
                    AIState.CONTROL_CENTER : AIState.CONTROL_CORNERS;
            return;
        }

        // עדיפות 4: בדוק אם יש הזדמנות לבניית דפוס
        if (hasPatternOpportunity()) {
            currentState = AIState.BUILD_PATTERN;
            return;
        }

        // עדיפות 5: בדוק הזדמנות התקפית
        if (hasOffensiveOpportunity()) {
            currentState = AIState.OFFENSE;
            return;
        }

        // עדיפות 6: במשחק מתקדם, בדוק דפוסים פתוחים
        if (turnCount > 8 && hasOpenPatterns()) {
            currentState = AIState.OFFENSE;
            return;
        }

        // ברירת מחדל - בחר באופן אקראי בין מרכז ופינות
        currentState = random.nextBoolean() ?
                AIState.CONTROL_CENTER : AIState.CONTROL_CORNERS;
    }

    /**
     * בדיקה אם יש מהלך מנצח לשחקן
     */
    private boolean hasWinningMove(int player) {
        for (PatternThreat threat : currentThreats) {
            if (threat.player == player && threat.count >= 4 && !threat.openEnds.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * בדיקה אם משבצות המרכז פנויות
     */
    private boolean isCenterAvailable() {
        for (int[] pos : CENTER_SQUARES) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * בדיקה אם יש הזדמנות לבניית דפוס
     */
    private boolean hasPatternOpportunity() {
        for (List<int[]> pattern : strategicPatterns.values()) {
            int[] counts = countPiecesInPattern(pattern);
            int playerPieces = counts[0];
            int opponentPieces = counts[1];

            // אם יש לנו כבר נוכחות בדפוס והאויב לא חוסם יותר מדי
            if (playerPieces >= 2 && opponentPieces <= pattern.size() / 4) {
                return true;
            }
        }
        return false;
    }

    /**
     * בדיקה אם יש הזדמנות התקפית
     */
    private boolean hasOffensiveOpportunity() {
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 3) {
                return true;
            }
        }
        return false;
    }

    /**
     * בדיקה אם יש דפוסים פתוחים שאפשר להרחיב
     */
    private boolean hasOpenPatterns() {
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 2 && !threat.openEnds.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ========================
    // 7. ניתוח לוח וזיהוי איומים
    // ========================

    /**
     * עדכון רשימת האיומים/הזדמנויות על הלוח
     */
    private void updateThreats() {
        currentThreats = findThreatsOnBoard(model.getBoard());
    }

    /**
     * מציאת איומים על לוח
     */
    private List<PatternThreat> findThreatsOnBoard(BitBoardRepresentation board) {
        List<PatternThreat> threats = new ArrayList<>();

        // בדיקת כל הכיוונים האפשריים (שורות, עמודות, אלכסונים)
        for (int dirIndex = 0; dirIndex < DIRECTIONS.length; dirIndex++) {
            int rowDelta = DIRECTIONS[dirIndex][0];
            int colDelta = DIRECTIONS[dirIndex][1];

            // קביעת טווח ההתחלה המתאים לכיוון
            int rowLimit = (rowDelta == 0) ? BOARD_SIZE : BOARD_SIZE - WIN_LENGTH + 1;
            int colLimit = (colDelta == 0) ? BOARD_SIZE : BOARD_SIZE - WIN_LENGTH + 1;
            int colStart = (colDelta < 0) ? WIN_LENGTH - 1 : 0;
            int colEnd = (colDelta < 0) ? BOARD_SIZE : colLimit;

            // בדיקת כל הקווים האפשריים בכיוון זה
            for (int row = 0; row < rowLimit; row++) {
                for (int col = colStart; col < colEnd; col++) {
                    checkLineForThreats(threats, board, row, col, rowDelta, colDelta, dirIndex);
                }
            }
        }

        // חישוב הציון לכל איום
        for (PatternThreat threat : threats) {
            calculateThreatScore(threat);
        }

        return threats;
    }

    /**
     * בדיקת קו (שורה/עמודה/אלכסון) ואיתור איומים
     */
    private void checkLineForThreats(
            List<PatternThreat> threats,
            BitBoardRepresentation board,
            int startRow, int startCol,
            int rowDelta, int colDelta,
            int direction) {

        int[] counts = {0, 0}; // ספירת כלים לכל שחקן
        List<int[]> positions = new ArrayList<>();
        List<int[]> emptyPositions = new ArrayList<>();

        // בדיקת קו באורך WIN_LENGTH
        for (int i = 0; i < WIN_LENGTH; i++) {
            int row = startRow + i * rowDelta;
            int col = startCol + i * colDelta;

            int piece = getPieceAtFromBoard(board, row, col);

            if (piece == 0 || piece == 1) {
                counts[piece]++;
                positions.add(new int[]{row, col});
            } else { // משבצת ריקה
                emptyPositions.add(new int[]{row, col});
            }
        }

        // יצירת איום לשחקן שיש לו כלים בקו (אם יש מספיק כלים)
        for (int player = 0; player < 2; player++) {
            if (counts[player] >= 2 && counts[1-player] == 0) {
                PatternThreat threat = new PatternThreat(player, counts[player]);
                threat.direction = direction;

                // הוספת המיקומים לאיום
                for (int[] pos : positions) {
                    if (getPieceAtFromBoard(board, pos[0], pos[1]) == player) {
                        threat.positions.add(pos);
                    }
                }

                // הוספת המיקומים הפנויים
                threat.openEnds.addAll(emptyPositions);

                // הוספת האיום לרשימה
                threats.add(threat);
            }
        }
    }

    /**
     * חישוב הציון של איום
     */
    private void calculateThreatScore(PatternThreat threat) {
        // ציון בסיסי לפי מספר הכלים ברצף
        switch (threat.count) {
            case 4:
                threat.score = LINE_4_SCORE;
                break;
            case 3:
                threat.score = LINE_3_SCORE;
                break;
            case 2:
                threat.score = LINE_2_SCORE;
                break;
            default:
                threat.score = 0;
        }

        // בונוס אם האיום שייך לשחקן ה-AI
        if (threat.player == playerNumber) {
            threat.score *= 1.2; // 20% יותר ערך לאיומים שלנו
        }

        // בונוס לפי מספר הסופים הפתוחים (יותר אפשרויות להשלמה)
        threat.score *= (1 + 0.2 * threat.openEnds.size());

        // בונוס לפי מיקום האיום (אלכסונים מרכזיים מקבלים בונוס)
        if ((threat.direction == 2 || threat.direction == 3) &&
                isPatternNearCenter(threat.positions)) {
            threat.score *= 1.3;
        }
    }

    /**
     * בדיקה אם דפוס קרוב למרכז הלוח
     */
    private boolean isPatternNearCenter(List<int[]> positions) {
        for (int[] pos : positions) {
            if ((pos[0] == 2 || pos[0] == 3) && (pos[1] == 2 || pos[1] == 3)) {
                return true;
            }
        }
        return false;
    }

    // ========================
    // 8. אסטרטגיות למהלכים
    // ========================

    /**
     * בחירת מהלך במצב התקפה
     */
    private int[] getOffensiveMove() {
        // ניסיון למצוא מהלך מנצח
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 4 && !threat.openEnds.isEmpty()) {
                return threat.openEnds.get(0);
            }
        }

        // אחרת, חפש את המהלך ההתקפי הטוב ביותר
        List<MoveEvaluation> potentialMoves = new ArrayList<>();

        // הערכת מהלכים התקפיים לפי איומים קיימים
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 2 && !threat.openEnds.isEmpty()) {
                for (int[] movePos : threat.openEnds) {
                    int moveScore = evaluateOffensivePosition(movePos[0], movePos[1]);
                    potentialMoves.add(new MoveEvaluation(movePos, moveScore));
                }
            }
        }

        if (!potentialMoves.isEmpty()) {
            // מיון המהלכים לפי ציון
            addRandomnessToPotentialMoves(potentialMoves);
            return potentialMoves.get(0).move;
        }

        // אם אין מהלכים התקפיים מובהקים, חזור למהלך אסטרטגי
        return getStrategicMove();
    }

    /**
     * הערכת מהלך התקפי במיקום מסוים
     */
    private int evaluateOffensivePosition(int row, int col) {
        int score = positionWeights[row][col]; // ציון בסיסי לפי משקל המיקום

        // בדיקה כמה איומים המהלך מחזק או יוצר
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber) {
                for (int[] pos : threat.openEnds) {
                    if (pos[0] == row && pos[1] == col) {
                        score += threat.score / 2; // חצי מהציון של האיום
                    }
                }
            }
        }

        // בדיקת יצירת fork (כמה דרכים לניצחון במקביל)
        score += calculateForkPotential(row, col, playerNumber);

        return score;
    }

    /**
     * חישוב פוטנציאל ליצירת fork (מספר דרכים לניצחון)
     */
    private int calculateForkPotential(int row, int col, int player) {
        BitBoardRepresentation tempBoard = cloneCurrentBoard();
        tempBoard.placePiece(row * BOARD_SIZE + col, player);

        int forkCount = 0;

        // בדיקת כל הכיוונים
        for (int[] dir : DIRECTIONS) {
            int rowDelta = dir[0];
            int colDelta = dir[1];

            for (int i = -4; i <= 0; i++) {
                int count = 0;
                int emptyCount = 0;

                for (int j = 0; j < WIN_LENGTH; j++) {
                    int r = row + (i + j) * rowDelta;
                    int c = col + (i + j) * colDelta;

                    if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                        // מחוץ לגבולות הלוח
                        count = 0;
                        break;
                    }

                    int piece = getPieceAtFromBoard(tempBoard, r, c);
                    if (piece == player) {
                        count++;
                    } else if (piece == -1) {
                        emptyCount++;
                    } else {
                        // כלי של היריב
                        count = 0;
                        break;
                    }
                }

                // אם יש לפחות 3 כלים שלנו ולפחות 2 משבצות ריקות, זה פוטנציאל fork
                if (count >= 3 && emptyCount >= 2) {
                    forkCount++;
                }
            }
        }

        return forkCount * 50; // 50 נקודות לכל דרך ניצחון פוטנציאלית
    }

    /**
     * בחירת מהלך במצב הגנה
     */
    private int[] getDefensiveMove() {
        List<MoveEvaluation> defensiveMoves = new ArrayList<>();

        // חיפוש איומים של היריב
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 3 && !threat.openEnds.isEmpty()) {
                for (int[] blockPos : threat.openEnds) {
                    // הערכת מהלך הגנתי
                    int blockScore = evaluateDefensivePosition(blockPos[0], blockPos[1]);
                    defensiveMoves.add(new MoveEvaluation(blockPos, blockScore));
                }
            }
        }

        if (!defensiveMoves.isEmpty()) {
            // מיון מהלכים הגנתיים
            sortMoveEvaluations(defensiveMoves);

            // במקרה של שוויון, בחר את המהלך שגם מקדם אותנו
            if (defensiveMoves.size() > 1 &&
                    Math.abs(defensiveMoves.get(0).score - defensiveMoves.get(1).score) < 50) {

                if (evaluateOffensivePosition(defensiveMoves.get(1).move[0],
                        defensiveMoves.get(1).move[1]) >
                        evaluateOffensivePosition(defensiveMoves.get(0).move[0],
                                defensiveMoves.get(0).move[1])) {
                    return defensiveMoves.get(1).move;
                }
            }
            return defensiveMoves.get(0).move;
        }

        // אם אין איומים מיידיים, נסה מהלך היברידי (הגנה + התקפה)
        return getHybridMove();
    }

    /**
     * הערכת מהלך הגנתי במיקום מסוים
     */
    private int evaluateDefensivePosition(int row, int col) {
        int score = BLOCK_SCORE; // ציון בסיסי לחסימה

        // בדיקה כמה איומים המהלך חוסם
        int blockedThreats = 0;
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber) {
                for (int[] pos : threat.openEnds) {
                    if (pos[0] == row && pos[1] == col) {
                        score += threat.count * 50; // ציון לפי מספר הכלים באיום
                        blockedThreats++;
                    }
                }
            }
        }

        // בונוס אם חוסמים יותר מאיום אחד בו-זמנית
        if (blockedThreats > 1) {
            score *= (1.0 + 0.5 * (blockedThreats - 1));
        }

        // בדיקה אם המהלך גם מקדם אותנו
        score += evaluateOffensivePosition(row, col) / 3; // שליש מהציון ההתקפי

        return score;
    }

    /**
     * בחירת מהלך היברידי (הגנה + התקפה)
     */
    private int[] getHybridMove() {
        List<int[]> availableMoves = getAvailableMoves();
        int[] bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (int[] move : availableMoves) {
            int row = move[0];
            int col = move[1];

            // שילוב של ציון הגנתי והתקפי
            int defensiveValue = evaluateDefensivePosition(row, col);
            int offensiveValue = evaluateOffensivePosition(row, col);

            // נרמול הערכים
            int totalScore = (defensiveValue + offensiveValue) / 2;

            // הוספת משקל המיקום
            totalScore += positionWeights[row][col];

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestMove = move;
            }
        }

        if (bestMove != null) {
            return bestMove;
        }

        // אם אין מהלך היברידי טוב, חזור למהלך אסטרטגי
        return getStrategicMove();
    }

    /**
     * בחירת מהלך לשליטה במרכז
     */
    private int[] getCenterControlMove() {
        List<int[]> centerPositions = new ArrayList<>();

        // בדיקת אילו מיקומים פנויים במרכז
        for (int[] pos : CENTER_POSITIONS) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                centerPositions.add(pos);
            }
        }

        if (!centerPositions.isEmpty()) {
            // מיון המיקומים לפי ערך אסטרטגי
            sortPositionsByWeight(centerPositions);

            // הוספת אקראיות קלה למהלכים דומים
            if (centerPositions.size() > 1 &&
                    positionWeights[centerPositions.get(0)[0]][centerPositions.get(0)[1]] ==
                            positionWeights[centerPositions.get(1)[0]][centerPositions.get(1)[1]]) {

                if (random.nextBoolean()) {
                    return centerPositions.get(1);
                }
            }

            return centerPositions.get(0);
        }

        // אם אין מיקומים פנויים במרכז, בחר מהלך אסטרטגי אחר
        return getStrategicMove();
    }

    /**
     * בחירת מהלך לשליטה בפינות
     */
    private int[] getCornerControlMove() {
        List<MoveEvaluation> cornerMoves = new ArrayList<>();

        // בדיקת אילו פינות פנויות
        for (int[] pos : CORNER_POSITIONS) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                int score = evaluateCornerPosition(pos[0], pos[1]);
                cornerMoves.add(new MoveEvaluation(pos, score));
            }
        }

        // מיקומים נוספים בקרבת פינות
        for (int[] pos : NEAR_CORNER_POSITIONS) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                int score = evaluateCornerPosition(pos[0], pos[1]);
                cornerMoves.add(new MoveEvaluation(pos, score));
            }
        }

        if (!cornerMoves.isEmpty()) {
            sortMoveEvaluations(cornerMoves);
            return cornerMoves.get(0).move;
        }

        // אם אין פינות פנויות, בחר מהלך אסטרטגי
        return getStrategicMove();
    }

    /**
     * הערכת מיקום פינתי
     */
    private int evaluateCornerPosition(int row, int col) {
        int score = positionWeights[row][col];

        // בדיקה אם המיקום הוא פינה ממש
        boolean isExactCorner = (row == 0 || row == 5) && (col == 0 || col == 5);
        if (isExactCorner) {
            score += 10;

            // בדיקה אם יש אלכסון פוטנציאלי מהפינה
            int diagRow = (row == 0) ? 1 : 4;
            int diagCol = (col == 0) ? 1 : 4;

            if (getPieceAt(diagRow, diagCol) == playerNumber) {
                score += 20; // בונוס אם כבר יש לנו כלי שיוצר אלכסון
            }
        }

        // בדיקה אם המיקום מתחבר לכלים שלנו
        int adjacentFriendly = countAdjacentPieces(row, col, playerNumber);
        score += adjacentFriendly * 15;

        return score;
    }

    /**
     * בחירת מהלך לבניית דפוס
     */
    private int[] getPatternBuildingMove() {
        List<MoveEvaluation> patternMoves = new ArrayList<>();

        // בדיקת כל הדפוסים האסטרטגיים
        for (Map.Entry<String, List<int[]>> entry : strategicPatterns.entrySet()) {
            List<int[]> pattern = entry.getValue();

            // בדיקה כמה כלים כבר יש בדפוס
            int patternScore = evaluatePattern(pattern);

            if (patternScore > 0) {
                // חיפוש משבצות פנויות בדפוס
                for (int[] pos : pattern) {
                    if (getPieceAt(pos[0], pos[1]) == -1) {
                        int moveScore = patternScore + positionWeights[pos[0]][pos[1]];

                        // בדיקה אם המהלך יוצר גם איומים אחרים
                        moveScore += evaluateOffensivePosition(pos[0], pos[1]) / 2;

                        patternMoves.add(new MoveEvaluation(pos, moveScore));
                    }
                }
            }
        }

        if (!patternMoves.isEmpty()) {
            sortMoveEvaluations(patternMoves);

            // גיוון - לפעמים בחר מהלך אקראי מתוך ה-3 הכי טובים
            if (random.nextInt(10) < 3 && patternMoves.size() >= 3) {
                return patternMoves.get(random.nextInt(3)).move;
            }

            return patternMoves.get(0).move;
        }

        // אם אין מהלכי דפוס טובים, חזור למהלך אסטרטגי
        return getStrategicMove();
    }

    /**
     * הערכת דפוס אסטרטגי
     */
    private int evaluatePattern(List<int[]> pattern) {
        int[] counts = countPiecesInPattern(pattern);
        int playerPieces = counts[0];
        int opponentPieces = counts[1];
        int emptySpaces = counts[2];

        // אם יש יותר מדי כלים של היריב, הדפוס לא שימושי
        if (opponentPieces > pattern.size() / 3) {
            return 0;
        }

        // חישוב ציון בסיסי - כמה הדפוס כבר מפותח
        int score = playerPieces * 20;

        // בונוס אם יש הרבה מקום להתפתח
        score += emptySpaces * 5;

        // בונוס אם יש כבר יותר מכלי אחד בדפוס
        if (playerPieces > 1) {
            score += playerPieces * 10;
        }

        return score;
    }

    /**
     * ספירת כלים בדפוס
     */
    private int[] countPiecesInPattern(List<int[]> pattern) {
        int playerPieces = 0;
        int opponentPieces = 0;
        int emptySpaces = 0;

        for (int[] pos : pattern) {
            int piece = getPieceAt(pos[0], pos[1]);
            if (piece == playerNumber) {
                playerPieces++;
            } else if (piece == opponentNumber) {
                opponentPieces++;
            } else {
                emptySpaces++;
            }
        }

        return new int[] {playerPieces, opponentPieces, emptySpaces};
    }

    /**
     * בחירת מהלך אסטרטגי כללי
     */
    private int[] getStrategicMove() {
        List<int[]> availableMoves = getAvailableMoves();

        if (availableMoves.isEmpty()) {
            // לא אמור לקרות
            return new int[]{0, 0};
        }

        int[] bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (int[] move : availableMoves) {
            int row = move[0];
            int col = move[1];

            // שילוב של ציונים
            int score = positionWeights[row][col] * 2; // משקל בסיסי של המיקום

            // בדיקת פוטנציאל התקפי
            score += evaluateOffensivePosition(row, col);

            // בדיקת פוטנציאל הגנתי
            score += evaluateDefensivePosition(row, col) / 2;

            // בדיקה אם המהלך יכול להשתלב בדפוס אסטרטגי
            score += evaluatePositionInPatterns(row, col);

            // בונוס למהלכים שמתחברים לכלים קיימים שלנו
            int adjacentFriendly = countAdjacentPieces(row, col, playerNumber);
            score += adjacentFriendly * 10;

            // הוספת רעש אקראי קטן
            score += random.nextInt(5);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    /**
     * הערכת מיקום ביחס לדפוסים אסטרטגיים
     */
    private int evaluatePositionInPatterns(int row, int col) {
        int score = 0;

        for (List<int[]> pattern : strategicPatterns.values()) {
            for (int[] pos : pattern) {
                if (pos[0] == row && pos[1] == col) {
                    score += 15;
                    break;
                }
            }
        }

        return score;
    }

    // ========================
    // 9. אסטרטגיות לסיבוב - משופר
    // ========================

    /**
     * מציאת סיבוב שיוביל לניצחון - משופר
     */
    private int[] findWinningRotation() {
        // בדיקת כל הסיבובים האפשריים
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                // יצירת לוח זמני לבדיקת הסיבוב
                BitBoardRepresentation tempBoard = cloneCurrentBoard();
                tempBoard.rotateQuadrant(quadrant, clockwise);

                // בדיקה אם הסיבוב מוביל לניצחון מיידי
                if (tempBoard.hasWinningLine(playerNumber)) {
                    return new int[]{quadrant, direction};
                }
            }
        }

        // בדיקת סיבובים שיוצרים איום מיידי (4 ברצף עם משבצת פנויה)
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                BitBoardRepresentation tempBoard = cloneCurrentBoard();
                tempBoard.rotateQuadrant(quadrant, clockwise);

                List<PatternThreat> threats = findThreatsOnBoard(tempBoard);
                for (PatternThreat threat : threats) {
                    // אם הסיבוב יוצר מצב של 4 ברצף עבורנו וגם משבצת פנויה להשלמה
                    if (threat.player == playerNumber && threat.count >= 4 && !threat.openEnds.isEmpty()) {
                        return new int[]{quadrant, direction};
                    }
                }
            }
        }

        return null;
    }

    /**
     * מציאת סיבוב שיחסום ניצחון של היריב - משופר
     */
    private int[] findBlockingRotation() {
        // בדיקה אם היריב יש לו ניצחון פוטנציאלי במצב הנוכחי
        boolean opponentHasPotentialWin = false;

        // בדיקת כל הסיבובים האפשריים של היריב בתור הבא
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                BitBoardRepresentation simulatedBoard = cloneCurrentBoard();
                simulatedBoard.rotateQuadrant(quadrant, clockwise);

                if (simulatedBoard.hasWinningLine(opponentNumber)) {
                    opponentHasPotentialWin = true;
                    break;
                }
            }
            if (opponentHasPotentialWin) break;
        }

        // אם היריב יכול לנצח, חפש סיבוב שיחסום אותו
        if (opponentHasPotentialWin) {
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                for (int direction = 0; direction < 2; direction++) {
                    boolean clockwise = (direction == 1);

                    BitBoardRepresentation tempBoard = cloneCurrentBoard();
                    tempBoard.rotateQuadrant(quadrant, clockwise);

                    boolean opponentCanWinAfterOurRotation = false;

                    // בדיקת כל האפשרויות של היריב לאחר הסיבוב שלנו
                    for (int oppQuadrant = 0; oppQuadrant < 4; oppQuadrant++) {
                        for (int oppDirection = 0; oppDirection < 2; oppDirection++) {
                            boolean oppClockwise = (oppDirection == 1);

                            BitBoardRepresentation oppBoard = cloneBoard(tempBoard);
                            oppBoard.rotateQuadrant(oppQuadrant, oppClockwise);

                            if (oppBoard.hasWinningLine(opponentNumber)) {
                                opponentCanWinAfterOurRotation = true;
                                break;
                            }
                        }
                        if (opponentCanWinAfterOurRotation) break;
                    }

                    // אם הסיבוב שלנו מונע ניצחון של היריב
                    if (!opponentCanWinAfterOurRotation) {
                        return new int[]{quadrant, direction};
                    }
                }
            }
        }

        // בדיקת מצב בו היריב יש לו 4 ברצף
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 4) {
                // חייב למצוא סיבוב שיהרוס את הרצף
                for (int quadrant = 0; quadrant < 4; quadrant++) {
                    for (int direction = 0; direction < 2; direction++) {
                        boolean clockwise = (direction == 1);

                        BitBoardRepresentation tempBoard = cloneCurrentBoard();
                        tempBoard.rotateQuadrant(quadrant, clockwise);

                        // בדיקה אם הסיבוב שובר את הרצף המסוכן
                        boolean threatStillExists = false;
                        for (PatternThreat afterThreat : findThreatsOnBoard(tempBoard)) {
                            if (afterThreat.player == opponentNumber &&
                                    afterThreat.count >= 4 &&
                                    threatsAreRelated(threat, afterThreat)) {
                                threatStillExists = true;
                                break;
                            }
                        }

                        if (!threatStillExists) {
                            return new int[]{quadrant, direction};
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * בדיקה אם שני איומים קשורים (מדובר באותו רצף)
     */
    private boolean threatsAreRelated(PatternThreat threat1, PatternThreat threat2) {
        // בדיקה אם יש לפחות 2 כלים משותפים לשני האיומים
        int commonPositions = 0;
        for (int[] pos1 : threat1.positions) {
            for (int[] pos2 : threat2.positions) {
                if (pos1[0] == pos2[0] && pos1[1] == pos2[1]) {
                    commonPositions++;
                    if (commonPositions >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * מציאת סיבוב אסטרטגי אופטימלי - משופר
     */
    private int[] findStrategicRotation() {
        int bestScore = Integer.MIN_VALUE;
        int[] bestRotation = null;

        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                // יצירת לוח זמני לבדיקת הסיבוב
                BitBoardRepresentation tempBoard = cloneCurrentBoard();
                tempBoard.rotateQuadrant(quadrant, clockwise);

                // הערכת מצב הלוח לאחר הסיבוב
                int score = evaluateBoardAfterRotation(tempBoard, quadrant, clockwise);

                if (score > bestScore) {
                    bestScore = score;
                    bestRotation = new int[]{quadrant, direction};
                }
            }
        }

        // אם לא נמצא סיבוב מועדף, בחר אקראי
        if (bestRotation == null) {
            return new int[]{random.nextInt(4), random.nextInt(2)};
        }

        return bestRotation;
    }

    /**
     * הערכת מצב הלוח לאחר סיבוב - משופר
     */
    private int evaluateBoardAfterRotation(BitBoardRepresentation board, int quadrant, boolean clockwise) {
        int score = 0;

        // בדיקת איומים על הלוח אחרי הסיבוב
        List<PatternThreat> afterThreats = findThreatsOnBoard(board);

        // ספירת איומים של כל שחקן
        int ourThreats = 0;
        int opponentThreats = 0;
        int ourOpenLines = 0;
        int opponentOpenLines = 0;

        for (PatternThreat threat : afterThreats) {
            if (threat.player == playerNumber) {
                ourThreats++;

                // בונוס משמעותי לאיומים גדולים
                if (threat.count == 3) {
                    score += 100;
                    ourOpenLines += threat.openEnds.size();
                } else if (threat.count == 4) {
                    score += 500;
                    ourOpenLines += threat.openEnds.size() * 3; // משקל גבוה יותר למשבצות פנויות ב-4 ברצף
                }
            } else {
                opponentThreats++;

                // הורדת ציון לאיומים של היריב
                if (threat.count == 3) {
                    score -= 150;
                    opponentOpenLines += threat.openEnds.size();
                } else if (threat.count == 4) {
                    score -= 600;
                    opponentOpenLines += threat.openEnds.size() * 3;
                }
            }
        }

        // בדיקת שינוי במספר האיומים לפני ואחרי הסיבוב
        List<PatternThreat> beforeThreats = currentThreats;
        int ourThreatsBefore = 0;
        int opponentThreatsBefore = 0;

        for (PatternThreat threat : beforeThreats) {
            if (threat.player == playerNumber && threat.count >= 3) {
                ourThreatsBefore++;
            } else if (threat.player == opponentNumber && threat.count >= 3) {
                opponentThreatsBefore++;
            }
        }

        // בונוס אם הסיבוב מגדיל את מספר האיומים שלנו
        if (ourThreats > ourThreatsBefore) {
            score += (ourThreats - ourThreatsBefore) * 80;
        }

        // בונוס אם הסיבוב מקטין את מספר האיומים של היריב
        if (opponentThreats < opponentThreatsBefore) {
            score += (opponentThreatsBefore - opponentThreats) * 100;
        }

        // הערכת כמה הסיבוב יוצר לנו יתרון
        score += (ourOpenLines - opponentOpenLines) * 15;

        // בדיקת יצירת דפוסים אסטרטגיים
        for (List<int[]> pattern : strategicPatterns.values()) {
            int patternScore = evaluatePatternOnBoard(board, pattern);
            score += patternScore;
        }

        // בדיקת סיבוב שמשבש את מרכז הלוח אם יש שם יתרון ליריב
        if (opponentHasCenterAdvantage()) {
            boolean centerDisrupted = isCenterDisruptedByRotation(quadrant);
            if (centerDisrupted) {
                score += 70;
            }
        }

        // בונוס לסיבוב שמשפר את המצב באזורים חשובים
        score += evaluateImportantAreasAfterRotation(board, quadrant);

        // הוספת אקראיות קלה למנוע סיבובים קבועים
        score += random.nextInt(20);

        return score;
    }

    /**
     * בדיקה אם ליריב יש יתרון במרכז
     */
    private boolean opponentHasCenterAdvantage() {
        int ourCenterPieces = 0;
        int opponentCenterPieces = 0;

        // בדיקת 4 המשבצות במרכז
        for (int[] pos : CENTER_SQUARES) {
            int piece = getPieceAt(pos[0], pos[1]);
            if (piece == playerNumber) {
                ourCenterPieces++;
            } else if (piece == opponentNumber) {
                opponentCenterPieces++;
            }
        }

        return opponentCenterPieces > ourCenterPieces;
    }

    /**
     * בדיקה אם סיבוב משבש את מרכז הלוח
     */
    private boolean isCenterDisruptedByRotation(int quadrant) {
        // מרכז הלוח מושפע מסיבוב של כל הרביעים
        return true;
    }

    /**
     * הערכת אזורים חשובים לאחר סיבוב
     */
    private int evaluateImportantAreasAfterRotation(BitBoardRepresentation board, int quadrant) {
        int score = 0;

        for (int[] pos : IMPORTANT_AREAS) {
            // בדיקה אם האזור נמצא ברביע המסובב
            int posQuadrant = getQuadrantForPosition(pos[0], pos[1]);

            if (posQuadrant == quadrant) {
                int piece = getPieceAtFromBoard(board, pos[0], pos[1]);

                if (piece == playerNumber) {
                    // בונוס אם הכלי שלנו הגיע לאזור חשוב
                    score += 25;
                } else if (piece == opponentNumber) {
                    // פחות טוב אם כלי של היריב הגיע לאזור חשוב
                    score -= 25;
                }
            }
        }

        return score;
    }

    /**
     * קביעת הרביע של מיקום
     */
    private int getQuadrantForPosition(int row, int col) {
        int quadRow = row / 3;
        int quadCol = col / 3;
        return quadRow * 2 + quadCol;
    }

    /**
     * הערכת דפוס אסטרטגי על לוח זמני
     */
    private int evaluatePatternOnBoard(BitBoardRepresentation board, List<int[]> pattern) {
        int playerPieces = 0;
        int opponentPieces = 0;
        int emptySpaces = 0;

        for (int[] pos : pattern) {
            int piece = getPieceAtFromBoard(board, pos[0], pos[1]);
            if (piece == playerNumber) {
                playerPieces++;
            } else if (piece == opponentNumber) {
                opponentPieces++;
            } else {
                emptySpaces++;
            }
        }

        // אם יש יותר מדי כלים של היריב, הדפוס לא שימושי
        if (opponentPieces > pattern.size() / 3) {
            return 0;
        }

        // חישוב ציון בסיסי - כמה הדפוס כבר מפותח
        int score = playerPieces * 15;

        // בונוס אם יש הרבה מקום להתפתח
        score += emptySpaces * 3;

        // בונוס אם יש כבר יותר מכלי אחד בדפוס
        if (playerPieces > 1) {
            score += playerPieces * 10;
        }

        return score;
    }

    /**
     * שכפול לוח (מספק גמישות יותר מהפונקציה הקיימת)
     */
    private BitBoardRepresentation cloneBoard(BitBoardRepresentation original) {
        BitBoardRepresentation clone = new BitBoardRepresentation();

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                int piece = original.getPieceAt(i, j);
                if (piece != -1) {
                    clone.placePiece(i * BOARD_SIZE + j, piece);
                }
            }
        }

        return clone;
    }

    // ========================
    // 10. פונקציות עזר
    // ========================

    /**
     * הוספת אקראיות קלה למהלכים דומים
     */
    private void addRandomnessToPotentialMoves(List<MoveEvaluation> moves) {
        sortMoveEvaluations(moves);

        // הוספת אקראיות למהלכים דומים
        for (int i = 0; i < moves.size() - 1; i++) {
            if (Math.abs(moves.get(i).score - moves.get(i + 1).score) < 20) {
                if (random.nextBoolean()) {
                    // החלפה אקראית בין שני מהלכים דומים
                    MoveEvaluation temp = moves.get(i);
                    moves.set(i, moves.get(i + 1));
                    moves.set(i + 1, temp);
                }
            }
        }
    }

    /**
     * מיון הערכות מהלכים לפי ציון בסדר יורד
     */
    private void sortMoveEvaluations(List<MoveEvaluation> moves) {
        // מיון באמצעות bubble sort (פשוט ויעיל לרשימות קטנות)
        int n = moves.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (moves.get(j).score < moves.get(j + 1).score) {
                    // החלפה
                    MoveEvaluation temp = moves.get(j);
                    moves.set(j, moves.get(j + 1));
                    moves.set(j + 1, temp);
                }
            }
        }
    }

    /**
     * מיון מיקומים לפי המשקל שלהם בסדר יורד
     */
    private void sortPositionsByWeight(List<int[]> positions) {
        // מיון באמצעות bubble sort
        int n = positions.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                int weight1 = positionWeights[positions.get(j)[0]][positions.get(j)[1]];
                int weight2 = positionWeights[positions.get(j + 1)[0]][positions.get(j + 1)[1]];

                if (weight1 < weight2) {
                    // החלפה
                    int[] temp = positions.get(j);
                    positions.set(j, positions.get(j + 1));
                    positions.set(j + 1, temp);
                }
            }
        }
    }

    /**
     * בדיקה אם קיים רצף מנצח על לוח
     */
    private boolean checkWinningLine(BitBoardRepresentation board, int player) {
        return board.hasWinningLine(player);
    }

    /**
     * ספירת כלים בסמיכות לנקודה
     */
    private int countAdjacentPieces(int row, int col, int player) {
        int count = 0;

        // בדיקת 8 השכנים
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                int r = row + dr;
                int c = col + dc;

                if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                    if (getPieceAt(r, c) == player) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * ספירת כלים ברביע
     */
    private int countPiecesInQuadrant(BitBoardRepresentation board, int quadrant) {
        int startRow = (quadrant / 2) * QUADRANT_SIZE;
        int startCol = (quadrant % 2) * QUADRANT_SIZE;
        int count = 0;

        for (int r = 0; r < QUADRANT_SIZE; r++) {
            for (int c = 0; c < QUADRANT_SIZE; c++) {
                if (getPieceAtFromBoard(board, startRow + r, startCol + c) != -1) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * קבלת כלי במיקום מסוים בלוח
     */
    private int getPieceAt(int row, int col) {
        return model.getBoard().getPieceAt(row, col);
    }

    /**
     * קבלת כלי במיקום מסוים בלוח זמני
     */
    private int getPieceAtFromBoard(BitBoardRepresentation board, int row, int col) {
        return board.getPieceAt(row, col);
    }

    /**
     * יצירת עותק של הלוח הנוכחי
     */
    private BitBoardRepresentation cloneCurrentBoard() {
        BitBoardRepresentation original = model.getBoard();
        BitBoardRepresentation clone = new BitBoardRepresentation();

        // העתקת מצב הלוח
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                int piece = original.getPieceAt(i, j);
                if (piece != -1) {
                    clone.placePiece(i * BOARD_SIZE + j, piece);
                }
            }
        }

        return clone;
    }

    /**
     * קבלת כל המהלכים האפשריים
     */
    private List<int[]> getAvailableMoves() {
        List<int[]> moves = new ArrayList<>();

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (getPieceAt(i, j) == -1) {
                    moves.add(new int[]{i, j});
                }
            }
        }

        return moves;
    }
}