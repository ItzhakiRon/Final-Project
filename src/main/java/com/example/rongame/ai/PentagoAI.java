package com.example.rongame.ai;

import com.example.rongame.model.BitBoardRepresentation;
import com.example.rongame.model.PentagoModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * בינה מלאכותית מתקדמת למשחק פנטגו
 * מבוססת על מכונת מצבים סופית (FSM), זיהוי תבניות וחישוב היוריסטי
 */
public class PentagoAI {

    //
    // 1. קבועים והגדרות
    //

    // קבועים למצבי המשחק
    public enum AIState {
        OFFENSE,         // מצב התקפה - יצירת רצף או השלמת רצף
        DEFENSE,         // מצב הגנה - חסימת האויב
        CONTROL_CENTER,  // מצב שליטה במרכז - תפיסת עמדות מרכזיות
        CONTROL_CORNERS, // מצב שליטה בפינות - תפיסת פינות אסטרטגיות
        BUILD_PATTERN    // מצב בניית דפוס אסטרטגי
    }

    // מספר המשבצות בשורה לניצחון
    private static final int WIN_LENGTH = 5;

    // משקלים להערכת עמדה
    private static final int WIN_SCORE = 10000;
    private static final int LINE_4_SCORE = 1000;
    private static final int LINE_3_SCORE = 100;
    private static final int LINE_2_SCORE = 10;
    private static final int BLOCK_SCORE = 150;
    private static final int CENTER_SCORE = 8;
    private static final int CORNER_SCORE = 5;
    private static final int EDGE_SCORE = 2;

    //
    // 2. שדות המחלקה
    //

    // הפנייה למודל המשחק
    private PentagoModel model;

    // מצב נוכחי של ה-AI
    private AIState currentState;

    // מספר השחקן (0 או 1)
    private int playerNumber;

    // היריב של ה-AI
    private int opponentNumber;

    // משתנה לאקראיות
    private Random random;

    // מונה תורים למעקב אחר התקדמות המשחק
    private int turnCount;

    // מטריצת משקלים לעמדות על הלוח
    private int[][] positionWeights;

    // מפת אסטרטגיות משחק - דפוסים אסטרטגיים
    private Map<String, List<int[]>> strategicPatterns;

    // תבניות מנצחות פוטנציאליות שזוהו
    private List<PatternThreat> currentThreats;

    //
    // 3. מחלקות פנימיות
    //

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

    //
    // 4. בנאי ואתחול
    //

    /**
     * בנאי למחלקת PentagoAI
     * @param model מודל המשחק
     */
    public PentagoAI(PentagoModel model) {
        this.playerNumber = 1; // ברירת מחדל - שחקן 1 (לבן)
        this.opponentNumber = 0;
        this.random = new Random();
        this.model = model;
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
        positionWeights = new int[6][6];

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
        List<int[]> mainDiagonal = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            mainDiagonal.add(new int[]{i, i});
        }
        strategicPatterns.put("mainDiagonal", mainDiagonal);

        // דפוס אלכסון משני
        List<int[]> antiDiagonal = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            antiDiagonal.add(new int[]{i, 5-i});
        }
        strategicPatterns.put("antiDiagonal", antiDiagonal);

        // דפוס "X"
        List<int[]> xPattern = new ArrayList<>();
        xPattern.add(new int[]{1, 1});
        xPattern.add(new int[]{2, 2});
        xPattern.add(new int[]{3, 3});
        xPattern.add(new int[]{4, 4});
        xPattern.add(new int[]{1, 4});
        xPattern.add(new int[]{2, 3});
        xPattern.add(new int[]{3, 2});
        xPattern.add(new int[]{4, 1});
        strategicPatterns.put("xPattern", xPattern);
    }

    //
    // 5. ממשק ציבורי
    //

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

        // בדיקה אם יש סיבוב מנצח
        int[] winningRotation = findWinningRotation();
        if (winningRotation != null) {
            return winningRotation;
        }

        // בדיקה אם יש סיבוב שחוסם ניצחון של היריב
        int[] blockingRotation = findBlockingRotation();
        if (blockingRotation != null) {
            return blockingRotation;
        }

        // אם אין סיבוב קריטי, בחר סיבוב אסטרטגי
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

    //
    // 6. ניהול מצבי FSM
    //

    /**
     * עדכון מצב ה-FSM בהתבסס על מצב הלוח הנוכחי
     */
    private void determineState() {
        // אם יש אפשרות לניצחון מיידי, עוברים למצב התקפה
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 4 && !threat.openEnds.isEmpty()) {
                currentState = AIState.OFFENSE;
                return;
            }
        }

        // אם היריב קרוב לניצחון, עוברים למצב הגנה
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 3 && !threat.openEnds.isEmpty()) {
                currentState = AIState.DEFENSE;
                return;
            }
        }

        // בתחילת המשחק - התמקד במרכז
        if (turnCount <= 3) {
            if (isCenterAvailable()) {
                currentState = AIState.CONTROL_CENTER;
                return;
            } else {
                currentState = AIState.CONTROL_CORNERS;
                return;
            }
        }

        // בדיקת הזדמנות לבניית דפוסים
        if (hasPatternOpportunity()) {
            currentState = AIState.BUILD_PATTERN;
            return;
        }

        // בדיקת הזדמנות התקפית
        if (hasOffensiveOpportunity()) {
            currentState = AIState.OFFENSE;
            return;
        }

        // לאחר כ-8 מהלכים, כבר יש דפוסים על הלוח
        if (turnCount > 8) {
            // בדיקה אם יש דפוס פתוח שאפשר להרחיב
            if (hasOpenPatterns()) {
                currentState = AIState.OFFENSE;
                return;
            }
        }

        // ברירת מחדל - אם אין מצב ברור, בחר בין שליטה במרכז ובפינות
        if (random.nextBoolean()) {
            currentState = AIState.CONTROL_CENTER;
        } else {
            currentState = AIState.CONTROL_CORNERS;
        }
    }

    //
    // 7. מציאת ועדכון איומים על הלוח
    //

    /**
     * עדכון רשימת האיומים/הזדמנויות על הלוח
     */
    private void updateThreats() {
        currentThreats.clear();

        // בדיקת שורות
        for (int row = 0; row < 6; row++) {
            for (int startCol = 0; startCol <= 6 - WIN_LENGTH; startCol++) {
                checkLineForThreats(row, startCol, 0, 1);
            }
        }

        // בדיקת עמודות
        for (int col = 0; col < 6; col++) {
            for (int startRow = 0; startRow <= 6 - WIN_LENGTH; startRow++) {
                checkLineForThreats(startRow, col, 1, 0);
            }
        }

        // בדיקת אלכסונים (שמאל-למעלה לימין-למטה)
        for (int row = 0; row <= 6 - WIN_LENGTH; row++) {
            for (int col = 0; col <= 6 - WIN_LENGTH; col++) {
                checkLineForThreats(row, col, 1, 1);
            }
        }

        // בדיקת אלכסונים (ימין-למעלה לשמאל-למטה)
        for (int row = 0; row <= 6 - WIN_LENGTH; row++) {
            for (int col = WIN_LENGTH - 1; col < 6; col++) {
                checkLineForThreats(row, col, 1, -1);
            }
        }

        // חישוב הציון לכל איום
        for (PatternThreat threat : currentThreats) {
            calculateThreatScore(threat);
        }
    }

    /**
     * בדיקת קו (שורה/עמודה/אלכסון) ואיתור איומים
     */
    private void checkLineForThreats(int startRow, int startCol, int rowDelta, int colDelta) {
        int[] counts = new int[2]; // ספירת כלים לכל שחקן
        List<int[]> positions = new ArrayList<>();
        List<int[]> emptyPositions = new ArrayList<>();

        // בדיקת קו באורך WIN_LENGTH
        for (int i = 0; i < WIN_LENGTH; i++) {
            int row = startRow + i * rowDelta;
            int col = startCol + i * colDelta;

            int piece = getPieceAt(row, col);

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

                // מספר הכיוון (שורה=0, עמודה=1, אלכסון=2, אלכסון נגדי=3)
                if (rowDelta == 0) threat.direction = 0;
                else if (colDelta == 0) threat.direction = 1;
                else if (colDelta > 0) threat.direction = 2;
                else threat.direction = 3;

                // הוספת המיקומים לאיום
                for (int[] pos : positions) {
                    if (getPieceAt(pos[0], pos[1]) == player) {
                        threat.positions.add(pos);
                    }
                }

                // הוספת המיקומים הפנויים
                threat.openEnds.addAll(emptyPositions);

                // הוספת האיום לרשימה
                currentThreats.add(threat);
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

    //
    // 8. אסטרטגיות למהלכים
    //

    /**
     * בחירת מהלך במצב התקפה
     */
    private int[] getOffensiveMove() {
        List<MoveEvaluation> potentialMoves = new ArrayList<>();

        // חיפוש השלמה לרצף של 4
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 4 && !threat.openEnds.isEmpty()) {
                // החזרת מהלך מנצח מיידית
                return threat.openEnds.get(0);
            }
        }

        // חיפוש השלמה לרצף של 3
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count == 3 && !threat.openEnds.isEmpty()) {
                for (int[] movePos : threat.openEnds) {
                    potentialMoves.add(new MoveEvaluation(movePos, threat.score));
                }
            }
        }

        // אם יש מהלכים התקפיים, בחר את הטוב ביותר
        if (!potentialMoves.isEmpty()) {
            // הוספת אקראיות קלה למהלכים עם ציון דומה
            potentialMoves.sort((a, b) -> {
                if (Math.abs(a.score - b.score) < 20) {
                    return random.nextInt(3) - 1;
                }
                return Integer.compare(b.score, a.score);
            });
            return potentialMoves.get(0).move;
        }

        // אם אין מהלכים התקפיים ברורים, נסה לחזק דפוס קיים
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count == 2 && !threat.openEnds.isEmpty()) {
                for (int[] movePos : threat.openEnds) {
                    // עדיפות למהלכים שמחזקים יותר מאיום אחד
                    int moveScore = evaluateOffensivePosition(movePos[0], movePos[1]);
                    potentialMoves.add(new MoveEvaluation(movePos, moveScore));
                }
            }
        }

        if (!potentialMoves.isEmpty()) {
            potentialMoves.sort((a, b) -> Integer.compare(b.score, a.score));
            return potentialMoves.get(0).move;
        }

        // אם עדיין אין מהלכים ברורים, חזור למהלך אסטרטגי
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
        tempBoard.placePiece(row * 6 + col, player);

        int forkCount = 0;

        // בדיקת שורות, עמודות ואלכסונים
        int[][] directions = {
                {0, 1}, {1, 0}, {1, 1}, {1, -1}
        };

        for (int[] dir : directions) {
            int rowDelta = dir[0];
            int colDelta = dir[1];

            for (int i = -4; i <= 0; i++) {
                int count = 0;
                int emptyCount = 0;

                for (int j = 0; j < WIN_LENGTH; j++) {
                    int r = row + (i + j) * rowDelta;
                    int c = col + (i + j) * colDelta;

                    if (r < 0 || r >= 6 || c < 0 || c >= 6) {
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
            defensiveMoves.sort((a, b) -> Integer.compare(b.score, a.score));
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

        // מרכז הלוח
        int[][] centralPositions = {
                {2, 2}, {2, 3}, {3, 2}, {3, 3},
                {1, 1}, {1, 4}, {4, 1}, {4, 4},
                {1, 2}, {1, 3}, {2, 1}, {3, 1}, {4, 2}, {4, 3}, {2, 4}, {3, 4}
        };

        // בדיקת אילו מיקומים פנויים במרכז
        for (int[] pos : centralPositions) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                centerPositions.add(pos);
            }
        }

        if (!centerPositions.isEmpty()) {
            // מיון המיקומים לפי ערך אסטרטגי
            centerPositions.sort((a, b) -> Integer.compare(
                    positionWeights[b[0]][b[1]], positionWeights[a[0]][a[1]]));

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

        // פינות הלוח
        int[][] cornerPositions = {
                {0, 0}, {0, 5}, {5, 0}, {5, 5}
        };

        // בדיקת אילו פינות פנויות
        for (int[] pos : cornerPositions) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                int score = evaluateCornerPosition(pos[0], pos[1]);
                cornerMoves.add(new MoveEvaluation(pos, score));
            }
        }

        // מיקומים נוספים בקרבת פינות
        int[][] nearCornerPositions = {
                {0, 1}, {1, 0}, {1, 1}, {0, 4}, {1, 5}, {1, 4},
                {4, 0}, {5, 1}, {4, 1}, {4, 5}, {5, 4}, {4, 4}
        };

        // בדיקת אילו מיקומים פנויים בקרבת פינות
        for (int[] pos : nearCornerPositions) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                int score = evaluateCornerPosition(pos[0], pos[1]);
                cornerMoves.add(new MoveEvaluation(pos, score));
            }
        }

        if (!cornerMoves.isEmpty()) {
            cornerMoves.sort((a, b) -> Integer.compare(b.score, a.score));
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
            patternMoves.sort((a, b) -> Integer.compare(b.score, a.score));

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
            for (List<int[]> pattern : strategicPatterns.values()) {
                for (int[] pos : pattern) {
                    if (pos[0] == row && pos[1] == col) {
                        score += 15;
                        break;
                    }
                }
            }

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

    //
    // 9. אסטרטגיות לסיבוב
    //

    /**
     * מציאת סיבוב שיוביל לניצחון
     */
    private int[] findWinningRotation() {
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                // יצירת לוח זמני לבדיקת הסיבוב
                BitBoardRepresentation tempBoard = cloneCurrentBoard();
                tempBoard.rotateQuadrant(quadrant, clockwise);

                // בדיקה אם הסיבוב מוביל לניצחון
                if (checkWinningLine(tempBoard, playerNumber)) {
                    return new int[]{quadrant, direction};
                }
            }
        }

        return null;
    }

    /**
     * מציאת סיבוב שיחסום ניצחון של היריב
     */
    private int[] findBlockingRotation() {
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                // יצירת לוח זמני לבדיקת הסיבוב
                BitBoardRepresentation tempBoard = cloneCurrentBoard();
                tempBoard.rotateQuadrant(quadrant, clockwise);

                // בדיקה אם הסיבוב מונע ניצחון של היריב
                if (!checkWinningLine(tempBoard, opponentNumber)) {
                    return new int[]{quadrant, direction};
                }
            }
        }

        return null;
    }

    /**
     * מציאת סיבוב אסטרטגי אופטימלי
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
     * הערכת מצב הלוח לאחר סיבוב
     */
    private int evaluateBoardAfterRotation(BitBoardRepresentation board, int quadrant, boolean clockwise) {
        int score = 0;

        // בדיקת איומים שלנו
        List<PatternThreat> afterThreats = findThreatsOnBoard(board);

        // ספירת איומים של כל שחקן
        int ourThreats = 0;
        int opponentThreats = 0;
        int ourOpenLines = 0;

        for (PatternThreat threat : afterThreats) {
            if (threat.player == playerNumber) {
                ourThreats++;
                if (threat.count >= 3) {
                    score += threat.count * 25;
                }
                ourOpenLines += threat.openEnds.size();
            } else {
                opponentThreats++;
                if (threat.count >= 3) {
                    score -= threat.count * 30;
                }
            }
        }

        // הערכת כמה הסיבוב יוצר לנו יתרון
        score += (ourThreats - opponentThreats) * 20;
        score += ourOpenLines * 5;

        // בונוס לסיבוב שמשנה רביע עם הרבה כלים (מייצר יותר הזדמנויות)
        int piecesInQuadrant = countPiecesInQuadrant(board, quadrant);
        score += piecesInQuadrant * 5;

        // בדיקה אם הסיבוב יוצר דפוסים אסטרטגיים
        for (List<int[]> pattern : strategicPatterns.values()) {
            int patternScore = evaluatePatternOnBoard(board, pattern);
            score += patternScore / 2;
        }

        // הוספת אקראיות קלה
        score += random.nextInt(10);

        return score;
    }

    /**
     * מציאת איומים על לוח זמני
     */
    private List<PatternThreat> findThreatsOnBoard(BitBoardRepresentation board) {
        List<PatternThreat> threats = new ArrayList<>();

        // בדיקת שורות
        for (int row = 0; row < 6; row++) {
            for (int startCol = 0; startCol <= 6 - WIN_LENGTH; startCol++) {
                checkLineForThreatsOnBoard(threats, board, row, startCol, 0, 1);
            }
        }

        // בדיקת עמודות
        for (int col = 0; col < 6; col++) {
            for (int startRow = 0; startRow <= 6 - WIN_LENGTH; startRow++) {
                checkLineForThreatsOnBoard(threats, board, startRow, col, 1, 0);
            }
        }

        // בדיקת אלכסונים (שמאל-למעלה לימין-למטה)
        for (int row = 0; row <= 6 - WIN_LENGTH; row++) {
            for (int col = 0; col <= 6 - WIN_LENGTH; col++) {
                checkLineForThreatsOnBoard(threats, board, row, col, 1, 1);
            }
        }

        // בדיקת אלכסונים (ימין-למעלה לשמאל-למטה)
        for (int row = 0; row <= 6 - WIN_LENGTH; row++) {
            for (int col = WIN_LENGTH - 1; col < 6; col++) {
                checkLineForThreatsOnBoard(threats, board, row, col, 1, -1);
            }
        }

        return threats;
    }

    /**
     * בדיקת קו (שורה/עמודה/אלכסון) על לוח זמני ואיתור איומים
     */
    private void checkLineForThreatsOnBoard(List<PatternThreat> threats, BitBoardRepresentation board,
                                            int startRow, int startCol, int rowDelta, int colDelta) {
        int[] counts = new int[2]; // ספירת כלים לכל שחקן
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

                // מספר הכיוון (שורה=0, עמודה=1, אלכסון=2, אלכסון נגדי=3)
                if (rowDelta == 0) threat.direction = 0;
                else if (colDelta == 0) threat.direction = 1;
                else if (colDelta > 0) threat.direction = 2;
                else threat.direction = 3;

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

    //
    // 10. פונקציות עזר
    //

    /**
     * בדיקה אם משבצות המרכז פנויות
     */
    private boolean isCenterAvailable() {
        int[][] centerPositions = {
                {2, 2}, {2, 3}, {3, 2}, {3, 3}
        };

        for (int[] pos : centerPositions) {
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
            int playerPieces = 0;
            int opponentPieces = 0;

            for (int[] pos : pattern) {
                int piece = getPieceAt(pos[0], pos[1]);
                if (piece == playerNumber) {
                    playerPieces++;
                } else if (piece == opponentNumber) {
                    opponentPieces++;
                }
            }

            // אם כבר יש לנו כלים בדפוס והיריב לא חסם יותר מדי
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

                if (r >= 0 && r < 6 && c >= 0 && c < 6) {
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
        int startRow = (quadrant / 2) * 3;
        int startCol = (quadrant % 2) * 3;
        int count = 0;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
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
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                int piece = original.getPieceAt(i, j);
                if (piece != -1) {
                    clone.placePiece(i * 6 + j, piece);
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

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (getPieceAt(i, j) == -1) {
                    moves.add(new int[]{i, j});
                }
            }
        }

        return moves;
    }
}