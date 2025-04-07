package com.example.rongame.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// מחלקה של בינה מלאכותית מבוססת FSM
public class PentagoAI {

    // קבועים למצבי המשחק
    public enum AIState {
        OFFENSE,    // מצב התקפה - יצירת רצף או השלמת רצף
        DEFENSE,    // מצב הגנה - חסימת האויב
        STRATEGIC,  // מצב אסטרטגי - תפיסת עמדות חשובות
        RANDOM      // מצב אקראי - כשאין אסטרטגיה ברורה
    }

    // לוח המשחק
    private int[][] board;

    // מצב נוכחי של ה-AI
    private AIState currentState;

    // מספר השחקן (0 או 1)
    private int playerNumber;

    // רמת הקושי (0-10, כאשר 10 הוא הקשה ביותר)
    private int difficultyLevel;

    // משתנה לאקראיות
    private Random random;

    /**
     * בנאי
     * @param difficulty רמת קושי בין 0-10
     */
    public PentagoAI(int difficulty) {
        this.difficultyLevel = Math.max(0, Math.min(10, difficulty));
        this.playerNumber = 1; // ברירת מחדל - שחקן 1 (אדום)
        this.random = new Random();
        this.board = new int[6][6];
        this.currentState = AIState.STRATEGIC; // מצב התחלתי

        // אתחול הלוח לריק (-1)
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                this.board[i][j] = -1;
            }
        }
    }

    /**
     * עדכון הלוח לאחר מהלך של השחקן
     * @param board מצב הלוח הנוכחי
     */
    public void updateBoard(int[][] board) {
        this.board = new int[6][6];
        for (int i = 0; i < 6; i++) {
            System.arraycopy(board[i], 0, this.board[i], 0, 6);
        }
        determineState();
    }

    /**
     * ביצוע מהלך של הנחת כלי על הלוח
     * @return מערך עם [שורה, עמודה] של המהלך
     */
    public int[] makeMove() {
        int[] move;

        // קבלת המהלך המתאים למצב הנוכחי
        switch (currentState) {
            case OFFENSE:
                move = getOffensiveMove();
                break;
            case DEFENSE:
                move = getDefensiveMove();
                break;
            case STRATEGIC:
                move = getStrategicMove();
                break;
            case RANDOM:
            default:
                move = getRandomMove();
                break;
        }

        return move;
    }

    /**
     * ביצוע סיבוב של רביע בלוח
     * @return מערך עם [רביע, כיוון] כאשר כיוון מיוצג כ-1 עבור כיוון השעון ו-0 עבור נגד כיוון השעון
     */
    public int[] makeRotation() {
        int quadrant;
        boolean clockwise;

        // בדיקה אם יש מהלך סיבוב התקפי
        int[] offensiveRotation = findOffensiveRotation();
        if (offensiveRotation != null) {
            quadrant = offensiveRotation[0];
            clockwise = offensiveRotation[1] == 1;
        }
        // בדיקה אם יש מהלך סיבוב הגנתי
        else {
            int[] defensiveRotation = findDefensiveRotation();
            if (defensiveRotation != null) {
                quadrant = defensiveRotation[0];
                clockwise = defensiveRotation[1] == 1;
            }
            // אם אין מהלכי התקפה או הגנה, בחירת סיבוב אקראי
            else {
                quadrant = random.nextInt(4);
                clockwise = random.nextBoolean();
            }
        }

        return new int[]{quadrant, clockwise ? 1 : 0};
    }

    // עדכון מצב ה-FSM בהתבסס על מצב הלוח הנוכחי
    private void determineState() {
        // אם יש אפשרות לניצחון מיידי, עוברים למצב התקפה
        if (hasWinningMove(playerNumber)) {
            currentState = AIState.OFFENSE;
            return;
        }

        // אם היריב קרוב לניצחון, עוברים למצב הגנה
        if (hasWinningMove(1 - playerNumber)) {
            currentState = AIState.DEFENSE;
            return;
        }

        // בדיקה אם יש עמדות אסטרטגיות פנויות חשובות
        if (hasStrategicPositions()) {
            currentState = AIState.STRATEGIC;
            return;
        }

        // הכנסת אקראיות למצב על פי רמת הקושי
        if (random.nextInt(10) > difficultyLevel) {
            currentState = AIState.RANDOM;
            return;
        }

        // בהעדר שיקול אחר, בחירה בין מצב אסטרטגי או התקפי
        currentState = (random.nextBoolean()) ? AIState.STRATEGIC : AIState.OFFENSE;
    }

    //  חיפוש המהלך הטוב ביותר במצב התקפה
    private int[] getOffensiveMove() {
        // חיפוש מהלכים שמובילים לרצף של 4
        List<int[]> potentialWins = findPotentialLinesWith(playerNumber, 3);

        if (!potentialWins.isEmpty()) {
            // החזרת המהלך הטוב ביותר מבין האופציות
            return potentialWins.get(random.nextInt(potentialWins.size()));
        }

        // חיפוש מהלכים שמובילים לרצף של 3
        List<int[]> potentialLines = findPotentialLinesWith(playerNumber, 2);

        if (!potentialLines.isEmpty()) {
            return potentialLines.get(random.nextInt(potentialLines.size()));
        }

        // אם אין אפשרויות התקפיות ברורות, חזרה למהלך אסטרטגי
        return getStrategicMove();
    }

    //  חיפוש המהלך הטוב ביותר במצב הגנה
    private int[] getDefensiveMove() {
        // חיפוש מהלכים שחוסמים רצף של 4 של היריב
        List<int[]> criticalBlocks = findPotentialLinesWith(1 - playerNumber, 3);

        if (!criticalBlocks.isEmpty()) {
            // החזרת המהלך הטוב ביותר מבין האופציות
            return criticalBlocks.get(random.nextInt(criticalBlocks.size()));
        }

        // חיפוש מהלכים שחוסמים רצף של 3 של היריב
        List<int[]> potentialBlocks = findPotentialLinesWith(1 - playerNumber, 2);

        if (!potentialBlocks.isEmpty()) {
            return potentialBlocks.get(random.nextInt(potentialBlocks.size()));
        }

        // אם אין מהלכי הגנה דחופים, ניתן לבצע מהלך אסטרטגי
        return getStrategicMove();
    }

    //  חיפוש המהלך הטוב ביותר במצב אסטרטגי
    private int[] getStrategicMove() {
        List<int[]> strategicPositions = new ArrayList<>();

        // עדיפות למרכז הלוח - תצורה 2x2 באמצע הלוח
        int[][] centerPositions = {{2, 2}, {2, 3}, {3, 2}, {3, 3}};
        for (int[] pos : centerPositions) {
            if (board[pos[0]][pos[1]] == -1) {
                strategicPositions.add(pos);
            }
        }

        // אם יש עמדות אסטרטגיות באמצע הלוח
        if (!strategicPositions.isEmpty() && random.nextInt(10) < 7) {
            return strategicPositions.get(random.nextInt(strategicPositions.size()));
        }

        // עמדות אסטרטגיות במרכז כל רביע
        int[][] quadrantCenters = {{1, 1}, {1, 4}, {4, 1}, {4, 4}};
        strategicPositions.clear();

        for (int[] pos : quadrantCenters) {
            if (board[pos[0]][pos[1]] == -1) {
                strategicPositions.add(pos);
            }
        }

        if (!strategicPositions.isEmpty() && random.nextInt(10) < 5) {
            return strategicPositions.get(random.nextInt(strategicPositions.size()));
        }

        // אם הכל תפוס או על פי הסתברות מסוימת, בחר מהלך אקראי
        return getRandomMove();
    }

    //  בחירת מהלך אקראי
    private int[] getRandomMove() {
        List<int[]> availableMoves = new ArrayList<>();

        // איסוף כל המהלכים האפשריים
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (board[i][j] == -1) {
                    availableMoves.add(new int[]{i, j});
                }
            }
        }

        if (!availableMoves.isEmpty()) {
            return availableMoves.get(random.nextInt(availableMoves.size()));
        }

        // אם אין מהלכים אפשריים (לא אמור לקרות במשחק רגיל)
        return new int[]{0, 0};
    }

    // בדיקה אם יש עמדות אסטרטגיות פנויות משמעותיות
    private boolean hasStrategicPositions() {
        // בדיקת המרכז
        int[][] centerPositions = {{2, 2}, {2, 3}, {3, 2}, {3, 3}};
        for (int[] pos : centerPositions) {
            if (board[pos[0]][pos[1]] == -1) {
                return true;
            }
        }

        // בדיקת מרכזי הרביעים
        int[][] quadrantCenters = {{1, 1}, {1, 4}, {4, 1}, {4, 4}};
        for (int[] pos : quadrantCenters) {
            if (board[pos[0]][pos[1]] == -1) {
                return true;
            }
        }

        return false;
    }

   // בדיקה אם יש מהלך שיכול להוביל לניצחון מיידי
    private boolean hasWinningMove(int player) {
        List<int[]> potentialWins = findPotentialLinesWith(player, 4);
        return !potentialWins.isEmpty();
    }

    //  מציאת מהלכים שיכולים להשלים רצף בגודל מסוים
    private List<int[]> findPotentialLinesWith(int player, int lineSize) {
        List<int[]> potentialMoves = new ArrayList<>();

        // בדיקת שורות
        for (int i = 0; i < 6; i++) {
            checkPotentialLine(potentialMoves, i, 0, 0, 1, player, lineSize);
        }

        // בדיקת עמודות
        for (int j = 0; j < 6; j++) {
            checkPotentialLine(potentialMoves, 0, j, 1, 0, player, lineSize);
        }

        // בדיקת אלכסונים (צד שמאל לימין)
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                checkPotentialLine(potentialMoves, i, j, 1, 1, player, lineSize);
            }
        }

        // בדיקת אלכסונים (צד ימין לשמאל)
        for (int i = 0; i <= 1; i++) {
            for (int j = 4; j <= 5; j++) {
                checkPotentialLine(potentialMoves, i, j, 1, -1, player, lineSize);
            }
        }

        return potentialMoves;
    }

    //  בדיקה ספציפית של קו פוטנציאלי (שורה, עמודה או אלכסון)
    private void checkPotentialLine(List<int[]> moves, int startRow, int startCol, int rowInc, int colInc, int player, int lineSize) {
        int emptyCount = 0;
        int playerCount = 0;
        int[] emptyPos = null;

        // בדיקת 5 משבצות ברצף
        for (int k = 0; k < 5; k++) {
            int r = startRow + k * rowInc;
            int c = startCol + k * colInc;

            // ודא שהקואורדינטות בגבולות הלוח
            if (r < 0 || r >= 6 || c < 0 || c >= 6) {
                return;
            }

            if (board[r][c] == -1) {
                emptyCount++;
                emptyPos = new int[]{r, c};
            } else if (board[r][c] == player) {
                playerCount++;
            } else {
                // אם יש כלי של היריב, הקו הזה לא רלוונטי
                return;
            }
        }

        // אם יש בדיוק משבצת ריקה אחת והשאר הן כלי השחקן בכמות הנדרשת
        if (emptyCount == 1 && playerCount == lineSize - 1) {
            moves.add(emptyPos);
        }
    }

    //  חיפוש סיבוב רביע שיוביל לניצחון
    private int[] findOffensiveRotation() {
        // תחילה, ננסה מהלכים בכל הרביעים ובשני הכיוונים
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = direction == 1;

                // יצירת עותק של הלוח לבדיקת המהלך
                int[][] tempBoard = copyBoard();

                // סימולציה של סיבוב הרביע
                rotateQuadrant(tempBoard, quadrant, clockwise);

                // בדיקה אם הסיבוב יוצר ניצחון
                if (hasWinningLine(tempBoard, playerNumber)) {
                    return new int[]{quadrant, direction};
                }
            }
        }

        return null;
    }

    //  חיפוש סיבוב רביע שימנע ניצחון של היריב
    private int[] findDefensiveRotation() {
        // בדיקת כל הרביעים וכיווני הסיבוב
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = direction == 1;

                // יצירת עותק של הלוח לבדיקת המהלך
                int[][] tempBoard = copyBoard();

                // סימולציה של סיבוב הרביע
                rotateQuadrant(tempBoard, quadrant, clockwise);

                // בדיקה אם הסיבוב מונע ניצחון של היריב
                if (!hasWinningLine(tempBoard, 1 - playerNumber) && hasThreatsCount(tempBoard, 1 - playerNumber) < hasThreatsCount(board, 1 - playerNumber)) {
                    return new int[]{quadrant, direction};
                }
            }
        }

        return null;
    }

    // בדיקה אם יש רצף של 5 בלוח
    private boolean hasWinningLine(int[][] boardToCheck, int player) {
        // בדיקת שורות
        for (int i = 0; i < 6; i++) {
            if (checkLine(boardToCheck, i, 0, 0, 1, player, 5)) {
                return true;
            }
        }

        // בדיקת עמודות
        for (int j = 0; j < 6; j++) {
            if (checkLine(boardToCheck, 0, j, 1, 0, player, 5)) {
                return true;
            }
        }

        // בדיקת אלכסונים (שמאל לימין)
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                if (checkLine(boardToCheck, i, j, 1, 1, player, 5)) {
                    return true;
                }
            }
        }

        // בדיקת אלכסונים (ימין לשמאל)
        for (int i = 0; i <= 1; i++) {
            for (int j = 4; j <= 5; j++) {
                if (checkLine(boardToCheck, i, j, 1, -1, player, 5)) {
                    return true;
                }
            }
        }

        return false;
    }

    //  ספירת איומים ברמת 4 כלים ברצף
    private int hasThreatsCount(int[][] boardToCheck, int player) {
        int threats = 0;

        // בדיקת שורות
        for (int i = 0; i < 6; i++) {
            threats += countThreatsInLine(boardToCheck, i, 0, 0, 1, player);
        }

        // בדיקת עמודות
        for (int j = 0; j < 6; j++) {
            threats += countThreatsInLine(boardToCheck, 0, j, 1, 0, player);
        }

        // בדיקת אלכסונים (שמאל לימין)
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                threats += countThreatsInLine(boardToCheck, i, j, 1, 1, player);
            }
        }

        // בדיקת אלכסונים (ימין לשמאל)
        for (int i = 0; i <= 1; i++) {
            for (int j = 4; j <= 5; j++) {
                threats += countThreatsInLine(boardToCheck, i, j, 1, -1, player);
            }
        }

        return threats;
    }

    // ספירת איומים בקו ספציפי
    private int countThreatsInLine(int[][] boardToCheck, int startRow, int startCol, int rowInc, int colInc, int player) {
        int playerCount = 0;
        int emptyCount = 0;

        // בדיקת 5 משבצות ברצף
        for (int k = 0; k < 5; k++) {
            int r = startRow + k * rowInc;
            int c = startCol + k * colInc;

            // ודא שהקואורדינטות בגבולות הלוח
            if (r < 0 || r >= 6 || c < 0 || c >= 6) {
                return 0;
            }

            if (boardToCheck[r][c] == player) {
                playerCount++;
            } else if (boardToCheck[r][c] == -1) {
                emptyCount++;
            } else {
                // אם יש כלי של היריב, הקו הזה לא רלוונטי
                return 0;
            }
        }

        // מחזיר 1 אם יש 4 כלים של השחקן ומשבצת ריקה אחת
        return (playerCount == 4 && emptyCount == 1) ? 1 : 0;
    }

    // בדיקה אם יש רצף של כלים מסוימים בקו (שורה/עמודה/אלכסון)
    private boolean checkLine(int[][] boardToCheck, int startRow, int startCol, int rowInc, int colInc, int player, int length) {
        int count = 0;

        for (int k = 0; k < length; k++) {
            int r = startRow + k * rowInc;
            int c = startCol + k * colInc;

            // ודא שהקואורדינטות בגבולות הלוח
            if (r < 0 || r >= 6 || c < 0 || c >= 6) {
                return false;
            }

            if (boardToCheck[r][c] == player) {
                count++;
            } else {
                return false;
            }
        }

        return count == length;
    }

    // יצירת עותק של הלוח הנוכחי
    private int[][] copyBoard() {
        int[][] copy = new int[6][6];
        for (int i = 0; i < 6; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, 6);
        }
        return copy;
    }

    //  סיבוב רביע בלוח
    private void rotateQuadrant(int[][] boardToRotate, int quadrant, boolean clockwise) {
        // המרת מספר רביע לשורה ועמודת התחלה
        int startRow = (quadrant / 2) * 3;
        int startCol = (quadrant % 2) * 3;

        // יצירת מטריצה זמנית לשמירת הערכים
        int[][] temp = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                temp[i][j] = boardToRotate[startRow + i][startCol + j];
            }
        }

        // סיבוב המטריצה
        if (clockwise) {
            // סיבוב עם כיוון השעון
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    boardToRotate[startRow + i][startCol + j] = temp[2 - j][i];
                }
            }
        } else {
            // סיבוב נגד כיוון השעון
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    boardToRotate[startRow + i][startCol + j] = temp[j][2 - i];
                }
            }
        }
    }

    // הגדרת מספר השחקן
    public void setPlayerNumber(int player) {
        this.playerNumber = player;
    }

    /*
    // הגדרת רמת הקושי
    public void setDifficulty(int difficulty) {
        this.difficultyLevel = Math.max(0, Math.min(10, difficulty));
    }

    // קבלת מצב ה-AI הנוכחי
    public AIState getCurrentState() {
        return currentState;
    }
    */
}