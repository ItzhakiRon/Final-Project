package com.example.rongame.ai;

import com.example.rongame.model.BitBoardRepresentation;
import com.example.rongame.model.PentagoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// מחלקה של בינה מלאכותית מבוססת FSM
public class PentagoAI {

    // קבועים למצבי המשחק
    public enum AIState {
        OFFENSE,       // מצב התקפה - יצירת רצף או השלמת רצף
        DEFENSE,       // מצב הגנה - חסימת האויב
        CONTROL_CENTER, // מצב שליטה במרכז - תפיסת עמדות חשובות במרכז
        CONTROL_EDGES, // מצב שליטה בקצוות - תפיסת עמדות בקצוות הלוח
        BLOCK_CORNERS, // מצב חסימת פינות - חסימת פינות אסטרטגיות
        BUILD_PATTERN, // מצב בניית דפוס - יצירת דפוסים אסטרטגיים מתקדמים
        CONTROL_ROTATION // מצב שליטה בסיבוב - הכנה לסיבוב מועיל
    }

    // התייחסות למודל המשחק
    private PentagoModel model;

    // מצב נוכחי של ה-AI
    private AIState currentState;

    // מספר השחקן (0 או 1)
    private int playerNumber;

    // רמת הקושי (0-10, כאשר 10 הוא הקשה ביותר)
    private int difficultyLevel;

    // משתנה לאקראיות
    private Random random;

    // מונה טורנים למעקב אחר התקדמות המשחק
    private int turnCount;

    /**
     * בנאי
     * @param difficulty רמת קושי בין 0-10
     * @param model מודל המשחק
     */
    public PentagoAI(int difficulty, PentagoModel model) {
        this.difficultyLevel = Math.max(0, Math.min(10, difficulty));
        this.playerNumber = 1; // ברירת מחדל - שחקן 1 (אדום)
        this.random = new Random();
        this.model = model;
        this.currentState = AIState.CONTROL_CENTER; // מצב התחלתי
        this.turnCount = 0;
    }

    /**
     * עדכון התייחסות למודל חדש
     * @param model מודל המשחק
     */
    public void setModel(PentagoModel model) {
        this.model = model;
        determineState();
    }

    /**
     * ביצוע מהלך של הנחת כלי על הלוח
     * @return מערך עם [שורה, עמודה] של המהלך
     */
    public int[] makeMove() {
        int[] move;
        turnCount++; // הגדלת מונה התורים בכל מהלך

        // קביעת מצב AI לפני קבלת החלטה
        determineState();

        // קבלת המהלך המתאים למצב הנוכחי
        switch (currentState) {
            case OFFENSE:
                move = getOffensiveMove();
                break;
            case DEFENSE:
                move = getDefensiveMove();
                break;
            case CONTROL_CENTER:
                move = getStrategicMove();
                break;
            case CONTROL_EDGES:
                move = getEdgeControlMove();
                break;
            case BLOCK_CORNERS:
                move = getCornerBlockMove();
                break;
            case BUILD_PATTERN:
                move = getPatternMove();
                break;
            case CONTROL_ROTATION:
                move = getRotationControlMove();
                break;
            default:
                // מהלך אקראי רק כברירת מחדל אם משהו השתבש
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
            // אם אין מהלכי התקפה או הגנה, בדיקת סיבוב אסטרטגי
            else {
                int[] strategicRotation = findStrategicRotation();
                if (strategicRotation != null) {
                    quadrant = strategicRotation[0];
                    clockwise = strategicRotation[1] == 1;
                }
                // אם אין מהלכים מיוחדים, בחירת סיבוב אקראי
                else {
                    quadrant = random.nextInt(4);
                    clockwise = random.nextBoolean();
                }
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

        // בתחילת המשחק (2-4 תורים ראשונים) - התמקד בבניית דפוסים אסטרטגיים
        if (turnCount <= 4) {
            if (hasPatternOpportunity()) {
                currentState = AIState.BUILD_PATTERN;
                return;
            }
        }

        // בדיקה אם יש עמדות אסטרטגיות במרכז שכדאי לתפוס
        if (hasCentralPositionsAvailable()) {
            currentState = AIState.CONTROL_CENTER;
            return;
        }

        // בדיקה אם יש הזדמנות לחסום פינות חשובות
        if (hasCornersThreat()) {
            currentState = AIState.BLOCK_CORNERS;
            return;
        }

        // בדיקה אם יש הזדמנות לבנות דפוס אסטרטגי
        if (hasPatternOpportunity()) {
            currentState = AIState.BUILD_PATTERN;
            return;
        }

        // בדיקה אם כדאי להתמקד בקצוות הלוח
        if (shouldControlEdges()) {
            currentState = AIState.CONTROL_EDGES;
            return;
        }

        // בשלבים מתקדמים יותר של המשחק, התכונן לסיבובים אסטרטגיים
        if (turnCount > 10) {
            currentState = AIState.CONTROL_ROTATION;
            return;
        }

        // בהעדר שיקול אחר, בחירה באסטרטגיה התקפית בהתאם לרמת הקושי
        int strategyChoice = random.nextInt(10);
        if (strategyChoice < difficultyLevel) {
            currentState = AIState.BUILD_PATTERN; // בחר אסטרטגיה מתקדמת ברמות קושי גבוהות
        } else {
            currentState = AIState.CONTROL_CENTER; // ברירת מחדל - אסטרטגיה בסיסית
        }
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

    //  חיפוש המהלך הטוב ביותר במצב אסטרטגי (שליטה במרכז)
    private int[] getStrategicMove() {
        List<int[]> strategicPositions = new ArrayList<>();

        // עדיפות למרכז הלוח - תצורה 2x2 באמצע הלוח
        int[][] centerPositions = {{2, 2}, {2, 3}, {3, 2}, {3, 3}};
        for (int[] pos : centerPositions) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
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
            if (getPieceAt(pos[0], pos[1]) == -1) {
                strategicPositions.add(pos);
            }
        }

        if (!strategicPositions.isEmpty() && random.nextInt(10) < 5) {
            return strategicPositions.get(random.nextInt(strategicPositions.size()));
        }

        // אם הכל תפוס או על פי הסתברות מסוימת, בחר מהלך בקצה
        return getEdgeControlMove();
    }

    // חדש: חיפוש מהלך לשליטה בקצוות הלוח
    private int[] getEdgeControlMove() {
        List<int[]> edgePositions = new ArrayList<>();

        // שורות קצה (שורה 0 ושורה 5, לא כולל פינות)
        for (int j = 1; j < 5; j++) {
            if (getPieceAt(0, j) == -1) edgePositions.add(new int[]{0, j});
            if (getPieceAt(5, j) == -1) edgePositions.add(new int[]{5, j});
        }

        // עמודות קצה (עמודה 0 ועמודה 5, לא כולל פינות)
        for (int i = 1; i < 5; i++) {
            if (getPieceAt(i, 0) == -1) edgePositions.add(new int[]{i, 0});
            if (getPieceAt(i, 5) == -1) edgePositions.add(new int[]{i, 5});
        }

        if (!edgePositions.isEmpty()) {
            return edgePositions.get(random.nextInt(edgePositions.size()));
        }

        // אם אין עמדות קצה פנויות, בדוק עמדות פינה
        List<int[]> cornerPositions = new ArrayList<>();
        int[][] corners = {{0, 0}, {0, 5}, {5, 0}, {5, 5}};

        for (int[] pos : corners) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                cornerPositions.add(pos);
            }
        }

        if (!cornerPositions.isEmpty()) {
            return cornerPositions.get(random.nextInt(cornerPositions.size()));
        }

        // אם אין גם קצוות וגם פינות פנויות, חזרה למהלך אקראי
        return getRandomMove();
    }

    // חדש: חיפוש מהלך לחסימת פינות אסטרטגיות
    private int[] getCornerBlockMove() {
        List<int[]> cornerBlocks = new ArrayList<>();

        // רשימת המיקומים שחוסמים גישה לפינות
        int[][] cornerBlockPositions = {
                {0, 1}, {1, 0}, {1, 1},       // חסימת פינה צפון-מערבית
                {0, 4}, {1, 4}, {1, 5},       // חסימת פינה צפון-מזרחית
                {4, 0}, {4, 1}, {5, 1},       // חסימת פינה דרום-מערבית
                {4, 5}, {4, 4}, {5, 4}        // חסימת פינה דרום-מזרחית
        };

        // בדיקה אם היריב כבר תפס פינה
        int[][] corners = {{0, 0}, {0, 5}, {5, 0}, {5, 5}};
        boolean[] enemyInCorner = new boolean[4]; // [NW, NE, SW, SE]

        for (int i = 0; i < 4; i++) {
            enemyInCorner[i] = (getPieceAt(corners[i][0], corners[i][1]) == 1 - playerNumber);
        }

        // אם היריב תפס פינה, תעדיף לחסום התקדמות מהפינה הזו
        for (int i = 0; i < 4; i++) {
            if (enemyInCorner[i]) {
                int startIdx = i * 3; // כל פינה יש 3 עמדות חסימה
                for (int j = 0; j < 3; j++) {
                    int[] pos = cornerBlockPositions[startIdx + j];
                    if (getPieceAt(pos[0], pos[1]) == -1) {
                        cornerBlocks.add(pos);
                    }
                }
            }
        }

        // אם מצאנו עמדות חסימה למיקומים שבהם היריב כבר נמצא, נבחר מהן
        if (!cornerBlocks.isEmpty()) {
            return cornerBlocks.get(random.nextInt(cornerBlocks.size()));
        }

        // אחרת, נבדוק כל עמדות החסימה האפשריות
        cornerBlocks.clear();
        for (int[] pos : cornerBlockPositions) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                cornerBlocks.add(pos);
            }
        }

        if (!cornerBlocks.isEmpty()) {
            return cornerBlocks.get(random.nextInt(cornerBlocks.size()));
        }

        // אם אין עמדות חסימת פינות פנויות, נחזור למהלך אסטרטגי
        return getStrategicMove();
    }

    // חדש: חיפוש מהלך לבניית דפוס אסטרטגי
    private int[] getPatternMove() {
        // דפוס אלכסוני - בניית "X" באמצע הלוח
        if (turnCount <= 4) { // משתמש בדפוס זה בטורנים מוקדמים
            int[][] diagonalPattern = {{1, 1}, {2, 2}, {3, 3}, {4, 4}, {1, 4}, {2, 3}, {3, 2}, {4, 1}};
            List<int[]> availableDiagonals = new ArrayList<>();

            // בדיקת עמדות פנויות בדפוס האלכסוני
            for (int[] pos : diagonalPattern) {
                if (getPieceAt(pos[0], pos[1]) == -1) {
                    availableDiagonals.add(pos);
                }
            }

            // אם יש עמדות פנויות בדפוס, בחר אחת באקראי
            if (!availableDiagonals.isEmpty()) {
                return availableDiagonals.get(random.nextInt(availableDiagonals.size()));
            }
        }

        // דפוס "משולש" - בניית משולש במרכז
        if (turnCount > 4 && turnCount <= 8) {
            int[][] trianglePattern = {{2, 1}, {2, 4}, {3, 1}, {3, 4}, {1, 2}, {1, 3}, {4, 2}, {4, 3}};
            List<int[]> availableTriangles = new ArrayList<>();

            // בדיקת עמדות פנויות בדפוס המשולשי
            for (int[] pos : trianglePattern) {
                if (getPieceAt(pos[0], pos[1]) == -1) {
                    availableTriangles.add(pos);
                }
            }

            if (!availableTriangles.isEmpty()) {
                return availableTriangles.get(random.nextInt(availableTriangles.size()));
            }
        }

        // דפוס "גשר" - חיבור בין שני חלקי הלוח
        int[][] bridgePattern = {{2, 0}, {2, 5}, {3, 0}, {3, 5}, {0, 2}, {0, 3}, {5, 2}, {5, 3}};
        List<int[]> availableBridges = new ArrayList<>();

        // בדיקת עמדות פנויות בדפוס הגשר
        for (int[] pos : bridgePattern) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                availableBridges.add(pos);
            }
        }

        if (!availableBridges.isEmpty()) {
            return availableBridges.get(random.nextInt(availableBridges.size()));
        }

        // אם אין אפשרויות לדפוסים, חזור למהלך אסטרטגי
        return getStrategicMove();
    }

    // חדש: חיפוש מהלך שמכין לסיבוב אסטרטגי
    private int[] getRotationControlMove() {
        // זיהוי רביעים שהם המועמדים הטובים ביותר לסיבוב
        List<Integer> quadrantsWithMostPieces = new ArrayList<>();
        int[] piecesInQuadrant = new int[4];

        // ספירת כלים בכל רביע
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                int piece = getPieceAt(i, j);
                if (piece != -1) {
                    int quadrant = (i / 3) * 2 + (j / 3); // חישוב מספר הרביע (0-3)
                    piecesInQuadrant[quadrant]++;
                }
            }
        }

        // מציאת הרביע עם מספר הכלים הגבוה ביותר
        int maxPieces = -1;
        for (int q = 0; q < 4; q++) {
            if (piecesInQuadrant[q] > maxPieces) {
                maxPieces = piecesInQuadrant[q];
                quadrantsWithMostPieces.clear();
                quadrantsWithMostPieces.add(q);
            } else if (piecesInQuadrant[q] == maxPieces) {
                quadrantsWithMostPieces.add(q);
            }
        }

        // מציאת משבצת פנויה באחד הרביעים המועדפים
        if (!quadrantsWithMostPieces.isEmpty()) {
            int targetQuadrant = quadrantsWithMostPieces.get(random.nextInt(quadrantsWithMostPieces.size()));
            int startRow = (targetQuadrant / 2) * 3;
            int startCol = (targetQuadrant % 2) * 3;

            List<int[]> emptyPositions = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (getPieceAt(startRow + i, startCol + j) == -1) {
                        emptyPositions.add(new int[] {startRow + i, startCol + j});
                    }
                }
            }

            if (!emptyPositions.isEmpty()) {
                return emptyPositions.get(random.nextInt(emptyPositions.size()));
            }
        }

        // אם אין משבצות פנויות ברביעים המועדפים, נחזור למהלך אסטרטגי
        return getStrategicMove();
    }

    //  בחירת מהלך אקראי - כברירת מחדל אחרונה
    private int[] getRandomMove() {
        List<int[]> availableMoves = new ArrayList<>();
        BitBoardRepresentation board = model.getBoard();

        // איסוף כל המהלכים האפשריים
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                int position = i * 6 + j;
                if (board.isPositionEmpty(position)) {
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

    // בדיקה אם יש עמדות אסטרטגיות פנויות במרכז הלוח
    private boolean hasCentralPositionsAvailable() {
        // בדיקת המרכז
        int[][] centerPositions = {{2, 2}, {2, 3}, {3, 2}, {3, 3}};
        for (int[] pos : centerPositions) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                return true;
            }
        }

        // בדיקת מרכזי הרביעים
        int[][] quadrantCenters = {{1, 1}, {1, 4}, {4, 1}, {4, 4}};
        for (int[] pos : quadrantCenters) {
            if (getPieceAt(pos[0], pos[1]) == -1) {
                return true;
            }
        }

        return false;
    }

    // חדש: בדיקה אם יש איום על פינות חשובות שכדאי לחסום
    private boolean hasCornersThreat() {
        int[][] corners = {{0, 0}, {0, 5}, {5, 0}, {5, 5}};

        // בדיקה אם היריב כבר תפס פינה
        for (int[] corner : corners) {
            if (getPieceAt(corner[0], corner[1]) == 1 - playerNumber) {
                // אם היריב תפס פינה, כדאי לחסום התקדמות ממנה
                return true;
            }
        }

        // בדיקה אם יש איום על פינה (היריב קרוב לפינה)
        int[][] cornerApproaches = {
                {0, 1}, {1, 0}, {1, 1},       // גישה לפינה צפון-מערבית
                {0, 4}, {1, 4}, {1, 5},       // גישה לפינה צפון-מזרחית
                {4, 0}, {4, 1}, {5, 1},       // גישה לפינה דרום-מערבית
                {4, 5}, {4, 4}, {5, 4}        // גישה לפינה דרום-מזרחית
        };

        for (int[] pos : cornerApproaches) {
            if (getPieceAt(pos[0], pos[1]) == 1 - playerNumber) {
                return true;
            }
        }

        return false;
    }

    // חדש: בדיקה אם יש הזדמנות לבניית דפוס אסטרטגי
    private boolean hasPatternOpportunity() {
        // בדיקת האם כבר יש התחלה של דפוס אלכסוני
        if (turnCount <= 4) {
            int[][] diagonalPositions = {{1, 1}, {2, 2}, {3, 3}, {4, 4}, {1, 4}, {2, 3}, {3, 2}, {4, 1}};
            int countPlayerPieces = 0;

            for (int[] pos : diagonalPositions) {
                if (getPieceAt(pos[0], pos[1]) == playerNumber) {
                    countPlayerPieces++;
                }
            }

            // אם כבר יש לפחות אבן אחת בדפוס, זו הזדמנות טובה
            return countPlayerPieces > 0;
        }

        // בדיקת האם כבר התחלנו דפוס משולש
        if (turnCount > 4 && turnCount <= 8) {
            int[][] trianglePositions = {{2, 1}, {2, 4}, {3, 1}, {3, 4}, {1, 2}, {1, 3}, {4, 2}, {4, 3}};
            int countPlayerPieces = 0;

            for (int[] pos : trianglePositions) {
                if (getPieceAt(pos[0], pos[1]) == playerNumber) {
                    countPlayerPieces++;
                }
            }

            return countPlayerPieces > 0;
        }

        return false;
    }

    // חדש: בדיקה אם כדאי להתמקד בשליטה בקצוות
    private boolean shouldControlEdges() {
        // בדיקת אחוז כלים בקצוות
        int edgePieces = 0;
        int totalEdgePositions = 16; // סה"כ 16 משבצות בקצוות (ללא פינות)

        // בדיקת שורות קצה
        for (int j = 1; j < 5; j++) {
            if (getPieceAt(0, j) != -1) edgePieces++;
            if (getPieceAt(5, j) != -1) edgePieces++;
        }

        // בדיקת עמודות קצה
        for (int i = 1; i < 5; i++) {
            if (getPieceAt(i, 0) != -1) edgePieces++;
            if (getPieceAt(i, 5) != -1) edgePieces++;
        }

        // אם פחות מ-50% מהקצוות תפוסים, שווה להתמקד בהם
        return (edgePieces < totalEdgePositions / 2);
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

            int piece = getPieceAt(r, c);
            if (piece == -1) {
                emptyCount++;
                emptyPos = new int[]{r, c};
            } else if (piece == player) {
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

                // יצירת מודל זמני לבדיקת המהלך
                PentagoModel tempModel = createTempModel();
                BitBoardRepresentation tempBoard = tempModel.getBoard();

                // סימולציה של סיבוב הרביע
                tempBoard.rotateQuadrant(quadrant, clockwise);

                // בדיקה אם הסיבוב יוצר ניצחון
                if (tempBoard.hasWinningLine(playerNumber)) {
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

                // יצירת מודל זמני לבדיקת המהלך
                PentagoModel tempModel = createTempModel();
                BitBoardRepresentation tempBoard = tempModel.getBoard();

                // סימולציה של סיבוב הרביע
                tempBoard.rotateQuadrant(quadrant, clockwise);

                // בדיקה אם הסיבוב מונע ניצחון של היריב
                if (!tempBoard.hasWinningLine(1 - playerNumber) &&
                        countThreats(tempBoard, 1 - playerNumber) < countThreats(model.getBoard(), 1 - playerNumber)) {
                    return new int[]{quadrant, direction};
                }
            }
        }

        return null;
    }

    // חדש: חיפוש סיבוב רביע אסטרטגי
    private int[] findStrategicRotation() {
        int bestScore = -1000;
        int[] bestRotation = null;

        // בדיקת כל הרביעים וכיווני הסיבוב
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int direction = 0; direction < 2; direction++) {
                boolean clockwise = direction == 1;

                // יצירת מודל זמני לבדיקת המהלך
                PentagoModel tempModel = createTempModel();
                BitBoardRepresentation tempBoard = tempModel.getBoard();

                // סימולציה של סיבוב הרביע
                tempBoard.rotateQuadrant(quadrant, clockwise);

                // חישוב הערכה של מצב הלוח לאחר הסיבוב
                int score = evaluatePositionAfterRotation(tempBoard);

                if (score > bestScore) {
                    bestScore = score;
                    bestRotation = new int[]{quadrant, direction};
                }
            }
        }

        return bestRotation;
    }

    // חדש: הערכת מצב הלוח לאחר סיבוב
    private int evaluatePositionAfterRotation(BitBoardRepresentation board) {
        int score = 0;

        // בונוס למספר האיומים שלנו (רצף של 3 + משבצת ריקה)
        score += 3 * countThreats(board, playerNumber);

        // מינוס למספר האיומים של היריב
        score -= 5 * countThreats(board, 1 - playerNumber);

        // בדיקת עמדות אסטרטגיות
        int[][] strategicSpots = {
                {2, 2}, {2, 3}, {3, 2}, {3, 3}, // מרכז הלוח
                {1, 1}, {1, 4}, {4, 1}, {4, 4}  // מרכזי רביעים
        };

        for (int[] pos : strategicSpots) {
            int piece = board.getPieceAt(pos[0], pos[1]);
            if (piece == playerNumber) {
                score += 2;
            } else if (piece == 1 - playerNumber) {
                score -= 1;
            }
        }

        // הוספת מרכיב אקראי קטן למניעת צפיות מוחלטת
        score += random.nextInt(3);

        return score;
    }

    // יצירת מודל זמני לסימולציות
    private PentagoModel createTempModel() {
        PentagoModel tempModel = new PentagoModel();
        BitBoardRepresentation originalBoard = model.getBoard();
        BitBoardRepresentation tempBoard = tempModel.getBoard();

        // העתקת מצב הלוח
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                int piece = originalBoard.getPieceAt(i, j);
                if (piece != -1) {
                    tempBoard.placePiece(i * 6 + j, piece);
                }
            }
        }

        return tempModel;
    }

    //  ספירת איומים ברמת 4 כלים ברצף
    private int countThreats(BitBoardRepresentation board, int player) {
        int threats = 0;

        // בדיקת שורות
        for (int i = 0; i < 6; i++) {
            threats += countThreatsInLine(board, i, 0, 0, 1, player);
        }

        // בדיקת עמודות
        for (int j = 0; j < 6; j++) {
            threats += countThreatsInLine(board, 0, j, 1, 0, player);
        }

        // בדיקת אלכסונים (שמאל לימין)
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                threats += countThreatsInLine(board, i, j, 1, 1, player);
            }
        }

        // בדיקת אלכסונים (ימין לשמאל)
        for (int i = 0; i <= 1; i++) {
            for (int j = 4; j <= 5; j++) {
                threats += countThreatsInLine(board, i, j, 1, -1, player);
            }
        }

        return threats;
    }

    // ספירת איומים בקו ספציפי
    private int countThreatsInLine(BitBoardRepresentation board, int startRow, int startCol, int rowInc, int colInc, int player) {
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

            int piece = board.getPieceAt(r, c);
            if (piece == player) {
                playerCount++;
            } else if (piece == -1) {
                emptyCount++;
            } else {
                // אם יש כלי של היריב, הקו הזה לא רלוונטי
                return 0;
            }
        }

        // מחזיר 1 אם יש 4 כלים של השחקן ומשבצת ריקה אחת
        return (playerCount == 4 && emptyCount == 1) ? 1 : 0;
    }

    // Helper method לקבלת ערך במיקום מסוים בלוח
    private int getPieceAt(int row, int col) {
        return model.getBoard().getPieceAt(row, col);
    }

    // הגדרת מספר השחקן
    public void setPlayerNumber(int player) {
        this.playerNumber = player;
    }

    // הגדרת רמת הקושי
    public void setDifficulty(int difficulty) {
        this.difficultyLevel = Math.max(0, Math.min(10, difficulty));
    }

    // איפוס מונה טורנים (למשחק חדש)
    public void resetTurnCount() {
        this.turnCount = 0;
    }
}