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

        // בדיקה חדשה: תמיד בדוק קודם אם יש מהלך קריטי להגנה או התקפה
        int[] criticalMove = findCriticalMove();
        if (criticalMove != null) {
            return criticalMove;
        }

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

        // בדיקה סופית שהמהלך שנבחר לא מתעלם מאיום ברור
        move = validateMove(move);

        return move;
    }

    // פונקציה לאיתור מהלכים קריטיים שעלולים להיות חשובים יותר מהמצב הנוכחי
    private int[] findCriticalMove() {
        // 1. בדיקה אם אנחנו יכולים לנצח במהלך אחד
        List<int[]> availableMoves = getAvailableMoves();
        for (int[] move : availableMoves) {
            BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
            tempBoard.placePiece(move[0] * BOARD_SIZE + move[1], playerNumber);

            if (tempBoard.hasWinningLine(playerNumber)) {
                return move; // מהלך מנצח - תמיד בחר בו
            }
        }

        // 2. בדיקה אם היריב יכול לנצח במהלך הבא
        for (int[] move : availableMoves) {
            BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
            tempBoard.placePiece(move[0] * BOARD_SIZE + move[1], opponentNumber);

            if (tempBoard.hasWinningLine(opponentNumber)) {
                return move; // חסום מהלך מנצח של היריב
            }
        }

        // 3. בדיקת רצפים חמורים של היריב (3+ כלים פתוחים משני הצדדים)
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 3 && threat.openEnds.size() >= 2) {
                // בחר את האיבר הראשון ברשימת openEnds
                return threat.openEnds.get(0);
            }
        }

        // 4. בדיקה אם יש לנו הזדמנות ליצור רצף של 4 פתוח
        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 3 && threat.openEnds.size() >= 2) {
                return threat.openEnds.get(0); // נצל הזדמנות להתקפה חזקה
            }
        }

        return null; // אין מהלך קריטי כרגע
    }

    // פונקציה לוידוא שהמהלך שנבחר הגיוני מבחינת הגנה/התקפה
    private int[] validateMove(int[] proposedMove) {
        // בדיקה אם המהלך המוצע מתעלם ממהלך הגנתי קריטי
        int[] criticalMove = findCriticalMove();
        if (criticalMove != null) {
            // אם יש מהלך קריטי והוא שונה מהמהלך המוצע, השתמש במהלך הקריטי
            if (proposedMove[0] != criticalMove[0] || proposedMove[1] != criticalMove[1]) {
                return criticalMove;
            }
        }

        // בדיקה אם המהלך המוצע מתעלם מאיום ברור של 3+ כלים
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 3 && !threat.openEnds.isEmpty()) {
                boolean proposedMoveBlocksThreat = false;

                // בדיקה אם המהלך המוצע חוסם את האיום
                for (int[] blockPos : threat.openEnds) {
                    if (proposedMove[0] == blockPos[0] && proposedMove[1] == blockPos[1]) {
                        proposedMoveBlocksThreat = true;
                        break;
                    }
                }

                // אם המהלך המוצע לא חוסם את האיום, בדוק אם יש איום גדול יותר
                if (!proposedMoveBlocksThreat && threat.count >= 4) {
                    return threat.openEnds.get(0); // חסום רצף של 4 כלים
                }
            }
        }

        return proposedMove; // המהלך המוצע בסדר
    }

    /**
     * ביצוע סיבוב של רביע בלוח
     * @return מערך עם [רביע (0-3), כיוון (0=נגד השעון, 1=עם השעון)]
     */
    public int[] makeRotation() {
        updateThreats();

        // בדיקה חדשה - האם יש סיבוב שיכול למנוע מהיריב לנצח במהלך הבא
        int[] emergencyRotation = findEmergencyRotation();
        if (emergencyRotation != null) {
            return emergencyRotation;
        }

        // בדיקת סיבובים אסטרטגיים
        int[] winningRotation = findWinningRotation();
        if (winningRotation != null) {
            // בדיקה שהסיבוב לא יוצר רצף מנצח ליריב
            BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
            tempBoard.rotateQuadrant(winningRotation[0], winningRotation[1] == 1);

            if (!tempBoard.hasWinningLine(opponentNumber)) {
                return winningRotation;
            }
        }

        int[] blockingRotation = findBlockingRotation();
        if (blockingRotation != null) {
            return blockingRotation;
        }

        return findStrategicRotation();
    }

    // פונקציה לחיפוש סיבוב שימנע הפסד במהלך הבא
    private int[] findEmergencyRotation() {
        // בדיקה אם ללא סיבוב היריב יכול לנצח במהלך הבא
        List<int[]> availableMoves = getAvailableMoves();

        // אם יש לפחות משבצת אחת פנויה, בדוק אם היריב יכול לנצח
        if (!availableMoves.isEmpty()) {
            BitBoardRepresentation noRotationBoard = cloneBoard(model.getBoard());

            // בדיקת כל המהלכים האפשריים של היריב
            for (int[] move : availableMoves) {
                BitBoardRepresentation afterOpponentMove = cloneBoard(noRotationBoard);
                afterOpponentMove.placePiece(move[0] * BOARD_SIZE + move[1], opponentNumber);

                if (afterOpponentMove.hasWinningLine(opponentNumber)) {
                    // היריב יכול לנצח במהלך הבא! יש לחפש סיבוב שימנע זאת

                    for (int quadrant = 0; quadrant < 4; quadrant++) {
                        for (int direction = 0; direction < 2; direction++) {
                            boolean clockwise = (direction == 1);

                            BitBoardRepresentation rotatedBoard = cloneBoard(model.getBoard());
                            rotatedBoard.rotateQuadrant(quadrant, clockwise);

                            boolean opponentCanStillWin = false;

                            // בדיקה אם אחרי הסיבוב היריב עדיין יכול לנצח
                            for (int[] opMove : getAvailableMoves(rotatedBoard)) {
                                BitBoardRepresentation afterRotationAndMove = cloneBoard(rotatedBoard);
                                afterRotationAndMove.placePiece(opMove[0] * BOARD_SIZE + opMove[1], opponentNumber);

                                if (afterRotationAndMove.hasWinningLine(opponentNumber)) {
                                    opponentCanStillWin = true;
                                    break;
                                }
                            }

                            // אם סיבוב זה מונע ניצחון של היריב, השתמש בו
                            if (!opponentCanStillWin) {
                                return new int[]{quadrant, direction};
                            }
                        }
                    }

                    break; // נמצא מהלך מנצח ליריב אבל לא נמצא סיבוב שמונע אותו
                }
            }
        }

        return null;
    }

    // פונקציית עזר לקבלת מהלכים אפשריים מלוח ספציפי
    private List<int[]> getAvailableMoves(BitBoardRepresentation board) {
        List<int[]> moves = new ArrayList<>();

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board.getPieceAt(i, j) == -1) {
                    moves.add(new int[]{i, j});
                }
            }
        }

        return moves;
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
        // בדיקת קריטית - אם יש רצף של היריב שיכול להוביל לניצחון, עבור מיד למצב הגנה
        boolean criticalThreatExists = checkForCriticalThreats();
        if (criticalThreatExists) {
            currentState = AIState.DEFENSE;
            return;
        }

        // עדיפות 1: איתור ניצחון מיידי שלנו
        if (hasWinningMove(playerNumber)) {
            currentState = AIState.OFFENSE;
            return;
        }

        // עדיפות 2: חסימת ניצחון של היריב
        if (hasWinningMove(opponentNumber)) {
            currentState = AIState.DEFENSE;
            return;
        }

        // עדיפות חדשה 3: חסימת רצפים מסוכנים של 3+ כלים פתוחים משני הצדדים
        if (hasOpenEndedSequence(opponentNumber, 3)) {
            currentState = AIState.DEFENSE;
            return;
        }

        // עדיפות 4: יצירת רצף מסוכן שלנו של 3+ פתוח משני הצדדים
        if (hasOpenEndedSequence(playerNumber, 3)) {
            currentState = AIState.OFFENSE;
            return;
        }

        // עדיפות 5: בתחילת המשחק, התמקד במרכז או בפינות
        if (turnCount <= 3) {
            currentState = isCenterAvailable() ?
                    AIState.CONTROL_CENTER : AIState.CONTROL_CORNERS;
            return;
        }

        // עדיפות 6: בדוק אם יש הזדמנות לבניית דפוס
        if (hasPatternOpportunity()) {
            currentState = AIState.BUILD_PATTERN;
            return;
        }

        // עדיפות 7: בדוק הזדמנות התקפית
        if (hasOffensiveOpportunity()) {
            currentState = AIState.OFFENSE;
            return;
        }

        // ברירת מחדל - בחר באופן אקראי בין מרכז ופינות
        currentState = random.nextBoolean() ?
                AIState.CONTROL_CENTER : AIState.CONTROL_CORNERS;
    }

    // פונקציה לבדיקת רצפים מסוכנים במיוחד
    private boolean checkForCriticalThreats() {
        // בדיקה אם יש רצפים מסוכנים של היריב
        for (PatternThreat threat : currentThreats) {
            // איתור רצפים של 3+ כלים עם אפשרות להשלים ל-5
            if (threat.player == opponentNumber && threat.count >= 3) {
                // בדיקה אם הרצף פתוח משני צדדים או יותר
                if (threat.openEnds.size() >= 2) {
                    return true;
                }

                // או אם יש כבר 4 כלים עם צד פתוח אחד
                if (threat.count >= 4 && threat.openEnds.size() >= 1) {
                    return true;
                }
            }
        }

        // בדיקת סימולציה - אם יש מהלך של היריב שיוצר רצף מסוכן
        List<int[]> availableMoves = getAvailableMoves();
        for (int[] move : availableMoves) {
            BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
            tempBoard.placePiece(move[0] * BOARD_SIZE + move[1], opponentNumber);

            List<PatternThreat> simulatedThreats = findThreatsOnBoard(tempBoard);
            for (PatternThreat threat : simulatedThreats) {
                // אם נוצר רצף של 4 עם צד פתוח או 3 עם שני צדדים פתוחים
                if (threat.player == opponentNumber) {
                    if ((threat.count >= 4 && threat.openEnds.size() >= 1) ||
                            (threat.count >= 3 && threat.openEnds.size() >= 2)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // פונקציה לבדיקת רצף פתוח משני הצדדים של שחקן מסוים
    private boolean hasOpenEndedSequence(int player, int minCount) {
        for (PatternThreat threat : currentThreats) {
            if (threat.player == player && threat.count >= minCount && threat.openEnds.size() >= 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * בדיקה אם יש מהלך מנצח לשחקן
     */
    private boolean hasWinningMove(int player) {
        // בדיקה ישירה - האם מהלך אחד יכול להוביל לניצחון
        List<int[]> availableMoves = getAvailableMoves();
        for (int[] move : availableMoves) {
            BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
            tempBoard.placePiece(move[0] * BOARD_SIZE + move[1], player);

            if (tempBoard.hasWinningLine(player)) {
                return true;
            }
        }

        // בדיקת איומים גם של רצפים כמעט מלאים
        for (PatternThreat threat : currentThreats) {
            if (threat.player == player) {
                // אם יש רצף של 4 כלים והוא פתוח לפחות מצד אחד
                if (threat.count >= 4 && !threat.openEnds.isEmpty()) {
                    return true;
                }

                // בדיקת רצפים של 3 פתוחים משני הצדדים - גם מסוכנים מאוד
                if (threat.count >= 3 && threat.openEnds.size() >= 2) {
                    // בדיקה שזה רצף רציף ללא חורים
                    boolean contiguous = true;
                    for (int i = 1; i < threat.positions.size() && contiguous; i++) {
                        // בדיקה שהמיקומים צמודים (בהתאם לכיוון)
                        int[] prev = threat.positions.get(i-1);
                        int[] curr = threat.positions.get(i);

                        int[] dir = DIRECTIONS[threat.direction];
                        if (prev[0] + dir[0] != curr[0] || prev[1] + dir[1] != curr[1]) {
                            contiguous = false;
                        }
                    }

                    if (contiguous) {
                        return true; // רצף רציף פתוח משני הצדדים - מאוד מסוכן
                    }
                }
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

        // שיטה לבדיקת רצפים - נבדוק חלון נע בגודל להכיל עד 7 משבצות (WIN_LENGTH + 2)

        // ראשית - בדיקה לאורך הקו הרגיל (5 משבצות)
        int[] counts = {0, 0}; // ספירת כלים לכל שחקן
        List<int[]> positions = new ArrayList<>();
        List<int[]> emptyPositions = new ArrayList<>();

        for (int i = 0; i < WIN_LENGTH; i++) {
            int row = startRow + i * rowDelta;
            int col = startCol + i * colDelta;

            if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
                int piece = getPieceAt(board, row, col);

                if (piece == 0 || piece == 1) {
                    counts[piece]++;
                    positions.add(new int[]{row, col});
                } else { // משבצת ריקה
                    emptyPositions.add(new int[]{row, col});
                }
            }
        }

        // בדיקת משבצות נוספות לפני ואחרי הרצף לזיהוי פתיחות
        int prevRow = startRow - rowDelta;
        int prevCol = startCol - colDelta;
        boolean hasOpenStart = false;

        if (prevRow >= 0 && prevRow < BOARD_SIZE && prevCol >= 0 && prevCol < BOARD_SIZE) {
            if (getPieceAt(board, prevRow, prevCol) == -1) {
                hasOpenStart = true;
                emptyPositions.add(new int[]{prevRow, prevCol});
            }
        }

        int nextRow = startRow + WIN_LENGTH * rowDelta;
        int nextCol = startCol + WIN_LENGTH * colDelta;
        boolean hasOpenEnd = false;

        if (nextRow >= 0 && nextRow < BOARD_SIZE && nextCol >= 0 && nextCol < BOARD_SIZE) {
            if (getPieceAt(board, nextRow, nextCol) == -1) {
                hasOpenEnd = true;
                emptyPositions.add(new int[]{nextRow, nextCol});
            }
        }

        // עיבוד הרצפים הרגילים
        for (int player = 0; player < 2; player++) {
            if (counts[player] >= 2 && counts[1-player] == 0) {
                PatternThreat threat = new PatternThreat(player, counts[player]);
                threat.direction = direction;

                for (int[] pos : positions) {
                    if (getPieceAt(board, pos[0], pos[1]) == player) {
                        threat.positions.add(pos);
                    }
                }

                threat.openEnds.addAll(emptyPositions);

                // הגברת הציון עבור רצפים פתוחים משני הצדדים
                switch(threat.count) {
                    case 4:
                        threat.score = LINE_4_SCORE * (hasOpenStart && hasOpenEnd ? 4 : 1);
                        break;
                    case 3:
                        threat.score = LINE_3_SCORE * (hasOpenStart && hasOpenEnd ? 3 : 1);
                        break;
                    case 2:
                        threat.score = LINE_2_SCORE * (hasOpenStart && hasOpenEnd ? 2 : 1);
                        break;
                    default:
                        threat.score = 0;
                }

                // בדיקה אם משבצות הפתח רצופות או מפוצלות
                if (emptyPositions.size() >= 2) {
                    boolean contiguousEmptySpaces = false;

                    for (int i = 0; i < emptyPositions.size() - 1; i++) {
                        int[] pos1 = emptyPositions.get(i);
                        int[] pos2 = emptyPositions.get(i + 1);

                        if (Math.abs(pos1[0] - pos2[0]) == Math.abs(rowDelta) &&
                                Math.abs(pos1[1] - pos2[1]) == Math.abs(colDelta)) {
                            contiguousEmptySpaces = true;
                            break;
                        }
                    }

                    if (contiguousEmptySpaces) {
                        // רצפים עם שני מקומות פנויים רצופים פחות מסוכנים
                        threat.score = (int)(threat.score * 0.8);
                    }
                }

                threats.add(threat);
            }
        }

        // חיפוש מורחב - בדיקה של חלונות בגודל 7 לזיהוי רצפים ארוכים יותר או עם חללים
        for (int windowStart = -1; windowStart <= 1; windowStart++) {
            int windowStartRow = startRow + windowStart * rowDelta;
            int windowStartCol = startCol + windowStart * colDelta;

            // בדיקת תקינות גבולות החלון
            boolean windowValid = true;
            for (int i = 0; i < WIN_LENGTH + 2 && windowValid; i++) {
                int r = windowStartRow + i * rowDelta;
                int c = windowStartCol + i * colDelta;

                if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                    windowValid = false;
                }
            }

            if (!windowValid) continue;

            // בדיקת החלון המורחב
            int[] extCounts = {0, 0};
            List<int[]> extPositions = new ArrayList<>();
            List<int[]> extEmptyPositions = new ArrayList<>();

            for (int i = 0; i < WIN_LENGTH + 2; i++) {
                int r = windowStartRow + i * rowDelta;
                int c = windowStartCol + i * colDelta;

                int piece = getPieceAt(board, r, c);
                if (piece == 0 || piece == 1) {
                    extCounts[piece]++;
                    extPositions.add(new int[]{r, c});
                } else { // משבצת ריקה
                    extEmptyPositions.add(new int[]{r, c});
                }
            }

            // רק אם יש רצף של שחקן אחד ללא כלים של השחקן השני
            for (int player = 0; player < 2; player++) {
                if (extCounts[player] >= 3 && extCounts[1-player] == 0) {
                    PatternThreat extThreat = new PatternThreat(player, extCounts[player]);
                    extThreat.direction = direction;

                    for (int[] pos : extPositions) {
                        if (getPieceAt(board, pos[0], pos[1]) == player) {
                            extThreat.positions.add(pos);
                        }
                    }

                    extThreat.openEnds.addAll(extEmptyPositions);

                    // חישוב ציון מוגבר לרצפים ארוכים/מסוכנים
                    if (extThreat.count >= 4) {
                        extThreat.score = LINE_4_SCORE * 2;
                    } else if (extThreat.count == 3 && extThreat.openEnds.size() >= 2) {
                        extThreat.score = LINE_3_SCORE * 2;
                    } else {
                        extThreat.score = extThreat.count * 50;
                    }

                    // בונוס למיקומים אסטרטגיים
                    boolean nearCenter = false;
                    for (int[] pos : extThreat.positions) {
                        if ((pos[0] >= 1 && pos[0] <= 4) && (pos[1] >= 1 && pos[1] <= 4)) {
                            nearCenter = true;
                            break;
                        }
                    }

                    if (nearCenter) {
                        extThreat.score = (int)(extThreat.score * 1.2);
                    }

                    // הוספה רק אם התבנית לא נכללת כבר
                    boolean alreadyExists = false;
                    for (PatternThreat existing : threats) {
                        if (existing.player == player &&
                                existing.direction == direction &&
                                hasSignificantOverlap(existing.positions, extThreat.positions)) {
                            alreadyExists = true;

                            // אם התבנית החדשה טובה יותר, עדכן את הקיימת
                            if (extThreat.score > existing.score) {
                                existing.score = extThreat.score;
                                existing.openEnds.clear();
                                existing.openEnds.addAll(extThreat.openEnds);
                            }
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        threats.add(extThreat);
                    }
                }
            }
        }
    }

    // פונקציית עזר לבדיקת חפיפה בין שתי רשימות מיקומים
    private boolean hasSignificantOverlap(List<int[]> list1, List<int[]> list2) {
        int overlap = 0;
        for (int[] pos1 : list1) {
            for (int[] pos2 : list2) {
                if (pos1[0] == pos2[0] && pos1[1] == pos2[1]) {
                    overlap++;
                    if (overlap >= Math.min(list1.size(), list2.size()) / 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * חישוב הציון של איום
     */
    private void calculateThreatScore(PatternThreat threat) {
        // ציון בסיסי לפי מספר הכלים ברצף
        switch (threat.count) {
            case 4:
                threat.score = LINE_4_SCORE * 2; // הגדלת הציון לרצף של 4
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

        // בונוס משמעותי אם יש שני סופים פתוחים
        if (threat.openEnds.size() >= 2) {
            threat.score *= 2.0; // הכפלת הציון לרצף פתוח משני הצדדים
        } else {
            // בונוס רגיל לפי מספר הסופים הפתוחים
            threat.score *= (1 + 0.2 * threat.openEnds.size());
        }

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
                return threat.openEnds.get(0); // זה מהלך מנצח שלנו
            }
        }

        // חיפוש רצף של היריב עם 4 כלים - חייבים לחסום
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 4 && !threat.openEnds.isEmpty()) {
                return threat.openEnds.get(0); // חסום ניצחון של היריב
            }
        }

        // חיפוש רצפים פתוחים משני הצדדים עם 3 כלים או יותר
        List<MoveEvaluation> openEndedMoves = new ArrayList<>();
        for (PatternThreat threat : currentThreats) {
            // בדיקה מיוחדת לרצפים פתוחים משני הצדדים
            if (threat.player == playerNumber && threat.count >= 3 && threat.openEnds.size() >= 2) {
                for (int[] pos : threat.openEnds) {
                    // ניסיון לראות אם המהלך יוצר רצף מנצח
                    BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
                    tempBoard.placePiece(pos[0] * BOARD_SIZE + pos[1], playerNumber);

                    boolean createsWin = tempBoard.hasWinningLine(playerNumber);
                    int score = threat.score * 2; // ציון כפול לרצפים פתוחים

                    if (createsWin) {
                        score *= 10; // בונוס גדול אם יוצר ניצחון
                    }

                    openEndedMoves.add(new MoveEvaluation(pos, score));
                }
            }
        }

        if (!openEndedMoves.isEmpty()) {
            sortMoveEvaluations(openEndedMoves);
            return openEndedMoves.get(0).move;
        }

        // חיפוש רצפים של היריב עם 3 כלים הפתוחים משני הצדדים - חשוב לחסום
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 3 && threat.openEnds.size() >= 2) {
                return threat.openEnds.get(0); // חסום רצף פתוח משני הצדדים
            }
        }

        // אחרת, חיפוש מהלכים התקפיים רגילים
        List<MoveEvaluation> potentialMoves = new ArrayList<>();

        for (PatternThreat threat : currentThreats) {
            if (threat.player == playerNumber && threat.count >= 2 && !threat.openEnds.isEmpty()) {
                for (int[] movePos : threat.openEnds) {
                    int moveScore = evaluateOffensivePosition(movePos[0], movePos[1]);
                    potentialMoves.add(new MoveEvaluation(movePos, moveScore));
                }
            }
        }

        if (!potentialMoves.isEmpty()) {
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
        BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
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

                    int piece = getPieceAt(tempBoard, r, c);
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
        // 1. בדיקה אם היריב יכול לנצח במהלך הבא
        List<int[]> availableMoves = getAvailableMoves();
        for (int[] move : availableMoves) {
            BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
            tempBoard.placePiece(move[0] * BOARD_SIZE + move[1], opponentNumber);

            if (tempBoard.hasWinningLine(opponentNumber)) {
                return move; // חייבים לחסום מהלך מנצח
            }
        }

        // 2. מציאת רצפים מסוכנים - רצפים פתוחים משני הצדדים
        List<MoveEvaluation> defensiveMoves = new ArrayList<>();

        // מציאת רצפים פתוחים משני הצדדים
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.openEnds.size() >= 2) {
                // הערכת רצף פתוח לפי מספר הכלים
                int baseThreatScore;
                switch (threat.count) {
                    case 4:
                        baseThreatScore = LINE_4_SCORE * 4; // מאוד מסוכן
                        break;
                    case 3:
                        baseThreatScore = LINE_3_SCORE * 3; // מסוכן מאוד
                        break;
                    case 2:
                        baseThreatScore = LINE_2_SCORE * 2;
                        break;
                    default:
                        baseThreatScore = 0;
                }

                // הוספת כל נקודות החסימה האפשריות
                for (int[] blockPos : threat.openEnds) {
                    int blockScore = baseThreatScore;

                    // בדיקה אם החסימה הזו מונעת איומים נוספים
                    BitBoardRepresentation afterBlockBoard = cloneBoard(model.getBoard());
                    afterBlockBoard.placePiece(blockPos[0] * BOARD_SIZE + blockPos[1], playerNumber);

                    // בדיקה אם עדיין יש איומים לאחר החסימה
                    List<PatternThreat> remainingThreats = findThreatsOnBoard(afterBlockBoard);
                    boolean threatStillExists = false;

                    for (PatternThreat remainingThreat : remainingThreats) {
                        if (remainingThreat.player == opponentNumber &&
                                remainingThreat.count >= threat.count &&
                                hasSimilarDirection(remainingThreat, threat)) {
                            threatStillExists = true;
                            break;
                        }
                    }

                    // בונוס אם החסימה הזו באמת אפקטיבית
                    if (!threatStillExists) {
                        blockScore *= 1.5;
                    }

                    // בדיקה אם החסימה יוצרת גם הזדמנות התקפית לנו
                    int offensiveValue = evaluateOffensivePosition(blockPos[0], blockPos[1]);
                    blockScore += offensiveValue / 2; // כמחצית מהערך ההתקפי

                    defensiveMoves.add(new MoveEvaluation(blockPos, blockScore));
                }
            }
        }

        // 3. חיפוש רצפים עם 3+ כלים שפתוחים מצד אחד
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 3 && threat.openEnds.size() >= 1) {
                int baseThreatScore = threat.count * 100; // ציון בסיסי לפי מספר הכלים

                for (int[] blockPos : threat.openEnds) {
                    int blockScore = baseThreatScore;

                    // בדיקה אם גם מקדם אותנו
                    int offensiveValue = evaluateOffensivePosition(blockPos[0], blockPos[1]);
                    blockScore += offensiveValue / 3; // שליש מהערך ההתקפי

                    defensiveMoves.add(new MoveEvaluation(blockPos, blockScore));
                }
            }
        }

        // 4. בדיקת סימולציה - מה קורה אם היריב ישים כלי במקומות שונים
        for (int[] move : availableMoves) {
            BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
            tempBoard.placePiece(move[0] * BOARD_SIZE + move[1], opponentNumber);

            List<PatternThreat> simulatedThreats = findThreatsOnBoard(tempBoard);
            boolean createsDangerousPattern = false;

            for (PatternThreat simulatedThreat : simulatedThreats) {
                // בדיקה אם נוצר רצף מסוכן של 3+ פתוח או 4 עם פתח אחד
                if (simulatedThreat.player == opponentNumber) {
                    if ((simulatedThreat.count >= 3 && simulatedThreat.openEnds.size() >= 2) ||
                            (simulatedThreat.count >= 4 && simulatedThreat.openEnds.size() >= 1)) {
                        createsDangerousPattern = true;
                        break;
                    }
                }
            }

            if (createsDangerousPattern) {
                // אם זה יוצר דפוס מסוכן, הוסף אותו לרשימת מהלכים להגנה
                int preemptiveBlockScore = 500; // ציון קבוע לחסימה מונעת
                defensiveMoves.add(new MoveEvaluation(move, preemptiveBlockScore));
            }
        }

        if (!defensiveMoves.isEmpty()) {
            sortMoveEvaluations(defensiveMoves);
            return defensiveMoves.get(0).move;
        }

        // אם אין איומים מיידיים, נסה מהלך היברידי (הגנה + התקפה)
        return getHybridMove();
    }

    // פונקציית עזר לבדיקה אם שני איומים הם באותו כיוון
    private boolean hasSimilarDirection(PatternThreat threat1, PatternThreat threat2) {
        return threat1.direction == threat2.direction;
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
    // 9. אסטרטגיות לסיבוב
    // ========================

    /**
     * מציאת סיבוב שיוביל לניצחון
     */
    private int[] findWinningRotation() {
        // בדיקת כל הסיבובים האפשריים
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                // יצירת לוח זמני לבדיקת הסיבוב
                BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
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

                BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
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
     * מציאת סיבוב שיחסום ניצחון של היריב
     */
    private int[] findBlockingRotation() {
        // בדוק אם סיבובים מסויימים יגרמו ליריב לנצח
        List<int[]> dangerousRotations = new ArrayList<>();

        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = (direction == 1);

                BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
                tempBoard.rotateQuadrant(quadrant, clockwise);

                if (tempBoard.hasWinningLine(opponentNumber)) {
                    dangerousRotations.add(new int[]{quadrant, direction});
                }
            }
        }

        // אם יש סיבובים מסוכנים, מצא סיבוב בטוח
        if (!dangerousRotations.isEmpty()) {
            // בדוק את כל הסיבובים שאינם מסוכנים
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                for (int direction = 0; direction < 2; direction++) {
                    boolean isInDangerList = false;

                    // בדוק אם זה סיבוב מסוכן
                    for (int[] dangerous : dangerousRotations) {
                        if (dangerous[0] == quadrant && dangerous[1] == direction) {
                            isInDangerList = true;
                            break;
                        }
                    }

                    // אם זה לא סיבוב מסוכן, השתמש בו
                    if (!isInDangerList) {
                        return new int[]{quadrant, direction};
                    }
                }
            }
        }

        // בדיקת איומים של 4 כלים
        for (PatternThreat threat : currentThreats) {
            if (threat.player == opponentNumber && threat.count >= 4) {
                // חיפוש סיבוב שמשבש את האיום
                for (int quadrant = 0; quadrant < 4; quadrant++) {
                    for (int direction = 0; direction < 2; direction++) {
                        boolean clockwise = (direction == 1);

                        BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
                        tempBoard.rotateQuadrant(quadrant, clockwise);

                        // בדיקה אם האיום נשבש אחרי הסיבוב
                        boolean threatStillExists = false;
                        for (int[] pos : threat.positions) {
                            // בדוק אם האיום נשבש
                            boolean stillPartOfThreat = false;
                            for (int[] dir : DIRECTIONS) {
                                int count = 0;
                                for (int i = -4; i <= 4; i++) {
                                    int r = pos[0] + i * dir[0];
                                    int c = pos[1] + i * dir[1];

                                    if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                                        if (getPieceAt(tempBoard, r, c) == opponentNumber) {
                                            count++;
                                            if (count >= 4) {
                                                stillPartOfThreat = true;
                                                break;
                                            }
                                        } else {
                                            count = 0; // איפוס כשיש הפרעה
                                        }
                                    }
                                }
                                if (stillPartOfThreat) break;
                            }

                            if (stillPartOfThreat) {
                                threatStillExists = true;
                                break;
                            }
                        }

                        if (!threatStillExists && !tempBoard.hasWinningLine(opponentNumber)) {
                            return new int[]{quadrant, direction};
                        }
                    }
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
                BitBoardRepresentation tempBoard = cloneBoard(model.getBoard());
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
                int piece = getPieceAt(board, pos[0], pos[1]);

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
            int piece = getPieceAt(board, pos[0], pos[1]);
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

    // ========================
    // 10. פונקציות עזר
    // ========================

    /**
     * שכפול לוח
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
                if (getPieceAt(board, startRow + r, startCol + c) != -1) {
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
    private int getPieceAt(BitBoardRepresentation board, int row, int col) {
        return board.getPieceAt(row, col);
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