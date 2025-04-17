package com.example.rongame.ai;

import com.example.rongame.model.BitBoardRepresentation;
import com.example.rongame.model.PentagoModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * בינה מלאכותית מבוססת FSM למשחק Pentago
 * גרסה מנוקה עם רמת קושי קבועה ומאתגרת
 */
public class PentagoAI {

    // קבועים למצבי המשחק
    public enum AIState {
        OFFENSE,         // מצב התקפה - יצירת רצף או השלמת רצף
        DEFENSE,         // מצב הגנה - חסימת האויב
        CONTROL_CENTER,  // מצב שליטה במרכז - תפיסת עמדות חשובות
        CONTROL_EDGES,   // מצב שליטה בקצוות הלוח
        BLOCK_CORNERS,   // מצב חסימת פינות אסטרטגיות
        BUILD_PATTERN,   // מצב בניית דפוס אסטרטגי
        CONTROL_ROTATION,// מצב שליטה בסיבוב הלוח
        LOOK_AHEAD       // מצב חישוב מהלכים קדימה
    }

    // הפנייה למודל המשחק
    private PentagoModel model;

    // מצב נוכחי של ה-AI
    private AIState currentState;

    // מספר השחקן (0 או 1)
    private int playerNumber;

    // משתנה לאקראיות
    private Random random;

    // מונה טורנים למעקב אחר התקדמות המשחק
    private int turnCount;

    // מטריצת משקלים לעמדות על הלוח
    private int[][] positionWeights;

    // מפת אסטרטגיות משחק
    private Map<String, List<int[]>> strategicPatterns;

    // מספר המהלכים להסתכל קדימה בניתוח
    private final int LOOK_AHEAD_DEPTH = 3;

    /**
     * בנאי
     * @param model מודל המשחק
     */
    public PentagoAI(PentagoModel model) {
        this.playerNumber = 1; // ברירת מחדל - שחקן 1 (לבן)
        this.random = new Random();
        this.model = model;
        this.currentState = AIState.CONTROL_CENTER; // מצב התחלתי
        this.turnCount = 0;

        initializePositionWeights();
        initializeStrategicPatterns();
    }

    /**
     * אתחול מטריצת משקלים לעמדות על הלוח
     */
    private void initializePositionWeights() {
        positionWeights = new int[6][6];

        // הגדרת משקלים שונים לאזורים שונים בלוח
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                // חישוב מרחק ממרכז הלוח
                double rowDistance = Math.min(Math.abs(i - 2), Math.abs(i - 3));
                double colDistance = Math.min(Math.abs(j - 2), Math.abs(j - 3));
                double centerDistance = Math.max(rowDistance, colDistance);

                // קביעת משקל בסיסי לפי המרחק מהמרכז
                if (centerDistance < 1) {
                    // מרכז הלוח - ערך מקסימלי
                    positionWeights[i][j] = 8;
                } else if (centerDistance < 2) {
                    // טבעת פנימית סביב המרכז
                    positionWeights[i][j] = 6;
                } else if (centerDistance < 3) {
                    // טבעת חיצונית
                    positionWeights[i][j] = 4;
                } else {
                    // הקצוות והפינות
                    positionWeights[i][j] = 2;
                }

                // הגדלת הערך של נקודות מפתח
                if ((i == 1 && j == 1) || (i == 1 && j == 4) ||
                        (i == 4 && j == 1) || (i == 4 && j == 4)) {
                    // מרכז כל רביע
                    positionWeights[i][j] += 2;
                }

                // נקודות אסטרטגיות נוספות - חיבורים בין רביעים
                if ((i == 2 && j == 1) || (i == 3 && j == 1) ||
                        (i == 2 && j == 4) || (i == 3 && j == 4) ||
                        (i == 1 && j == 2) || (i == 1 && j == 3) ||
                        (i == 4 && j == 2) || (i == 4 && j == 3)) {
                    positionWeights[i][j] += 1;
                }
            }
        }
    }

    /**
     * אתחול דפוסים אסטרטגיים
     */
    private void initializeStrategicPatterns() {
        strategicPatterns = new HashMap<>();

        // דפוס אלכסוני
        List<int[]> diagonalPattern = new ArrayList<>();
        diagonalPattern.add(new int[]{0, 0});
        diagonalPattern.add(new int[]{1, 1});
        diagonalPattern.add(new int[]{2, 2});
        diagonalPattern.add(new int[]{3, 3});
        diagonalPattern.add(new int[]{4, 4});
        diagonalPattern.add(new int[]{5, 5});
        strategicPatterns.put("diagonal1", diagonalPattern);

        // דפוס אלכסון נגדי
        List<int[]> antiDiagonalPattern = new ArrayList<>();
        antiDiagonalPattern.add(new int[]{0, 5});
        antiDiagonalPattern.add(new int[]{1, 4});
        antiDiagonalPattern.add(new int[]{2, 3});
        antiDiagonalPattern.add(new int[]{3, 2});
        antiDiagonalPattern.add(new int[]{4, 1});
        antiDiagonalPattern.add(new int[]{5, 0});
        strategicPatterns.put("diagonal2", antiDiagonalPattern);

        // דפוס Z
        List<int[]> zPattern = new ArrayList<>();
        zPattern.add(new int[]{1, 1});
        zPattern.add(new int[]{1, 2});
        zPattern.add(new int[]{2, 2});
        zPattern.add(new int[]{2, 3});
        zPattern.add(new int[]{3, 3});
        zPattern.add(new int[]{3, 4});
        zPattern.add(new int[]{4, 4});
        strategicPatterns.put("z_pattern", zPattern);

        // דפוס טבעת
        List<int[]> ringPattern = new ArrayList<>();
        ringPattern.add(new int[]{1, 1});
        ringPattern.add(new int[]{1, 2});
        ringPattern.add(new int[]{1, 3});
        ringPattern.add(new int[]{1, 4});
        ringPattern.add(new int[]{2, 4});
        ringPattern.add(new int[]{3, 4});
        ringPattern.add(new int[]{4, 4});
        ringPattern.add(new int[]{4, 3});
        ringPattern.add(new int[]{4, 2});
        ringPattern.add(new int[]{4, 1});
        ringPattern.add(new int[]{3, 1});
        ringPattern.add(new int[]{2, 1});
        strategicPatterns.put("ring_pattern", ringPattern);
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

        // בחירת המהלך המתאים למצב הנוכחי
        boolean useLookAhead = (random.nextInt(10) < 8); // הסתברות גבוהה לחשיבה קדימה (80%)

        if (useLookAhead && currentState == AIState.LOOK_AHEAD) {
            move = calculateBestMoveWithLookAhead();
        } else {
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
                    move = getAdvancedPatternMove();
                    break;
                case CONTROL_ROTATION:
                    move = getRotationControlMove();
                    break;
                default:
                    // מהלך אקראי כברירת מחדל
                    move = getRandomMove();
                    break;
            }
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

        // הסתברות גבוהה לניתוח מעמיק
        boolean deepAnalysis = (random.nextInt(10) < 8);

        if (deepAnalysis) {
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
                // בדיקת סיבוב אסטרטגי משופר
                else {
                    int[] strategicRotation = findAdvancedStrategicRotation();
                    if (strategicRotation != null) {
                        quadrant = strategicRotation[0];
                        clockwise = strategicRotation[1] == 1;
                    }
                    // בחירת סיבוב חכם
                    else {
                        int[] smartRotation = findSmartRandomRotation();
                        quadrant = smartRotation[0];
                        clockwise = smartRotation[1] == 1;
                    }
                }
            }
        } else {
            // לוגיקה פשוטה יותר
            int[] offensiveRotation = findOffensiveRotation();
            if (offensiveRotation != null) {
                quadrant = offensiveRotation[0];
                clockwise = offensiveRotation[1] == 1;
            } else {
                int[] defensiveRotation = findDefensiveRotation();
                if (defensiveRotation != null) {
                    quadrant = defensiveRotation[0];
                    clockwise = defensiveRotation[1] == 1;
                } else {
                    quadrant = random.nextInt(4);
                    clockwise = random.nextBoolean();
                }
            }
        }

        return new int[]{quadrant, clockwise ? 1 : 0};
    }

    /**
     * עדכון מצב ה-FSM בהתבסס על מצב הלוח הנוכחי
     */
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

        // בדיקת צורך בחישוב מהלכים קדימה
        if (shouldUseLookAhead()) {
            currentState = AIState.LOOK_AHEAD;
            return;
        }

        // בתחילת המשחק - התמקד בבניית דפוסים אסטרטגיים
        if (turnCount <= 4) {
            if (hasAdvancedPatternOpportunity()) {
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
        if (hasAdvancedPatternOpportunity()) {
            currentState = AIState.BUILD_PATTERN;
            return;
        }

        // בדיקה אם כדאי להתמקד בקצוות הלוח
        if (shouldControlEdges()) {
            currentState = AIState.CONTROL_EDGES;
            return;
        }

        // בשלבים מתקדמים יותר של המשחק, התכונן לסיבובים אסטרטגיים
        if (turnCount > 8) {
            currentState = AIState.CONTROL_ROTATION;
            return;
        }

        // ברירת מחדל - בחירה באסטרטגיה התקפית או בניית דפוס
        currentState = (random.nextInt(2) == 0) ? AIState.BUILD_PATTERN : AIState.CONTROL_CENTER;
    }

    /**
     * האם להשתמש בחישוב מהלכים קדימה
     */
    private boolean shouldUseLookAhead() {
        // יותר סיכוי לחישוב מעמיק ככל שהמשחק מתקדם
        int stageProgress = Math.min(turnCount / 3, 10);

        // שילוב של רמת קושי גבוהה (8/10) עם התקדמות המשחק
        return random.nextInt(10) < (8 + stageProgress) / 2;
    }

    /**
     * חישוב המהלך הטוב ביותר עם חשיבה קדימה
     */
    private int[] calculateBestMoveWithLookAhead() {
        List<int[]> possibleMoves = getAllPossibleMoves();
        int[] bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (int[] move : possibleMoves) {
            // בדיקת מהלך וחישוב ציון
            int score = evaluateMoveScore(move[0], move[1]);

            // האם זה המהלך הטוב ביותר עד כה?
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        // אם לא נמצא מהלך טוב, בחר מהלך אסטרטגי
        if (bestMove == null) {
            return getStrategicMove();
        }

        return bestMove;
    }

    /**
     * הערכת ציון למהלך ספציפי
     */
    private int evaluateMoveScore(int row, int col) {
        // חישוב בסיסי של ציון המהלך
        int score = 0;

        // תוספת ציון לפי משקל העמדה
        score += positionWeights[row][col] * 2;

        // ביצוע מהלך בלוח זמני
        PentagoModel tempModel = createTempModel();
        int position = row * 6 + col;
        tempModel.getBoard().placePiece(position, playerNumber);

        // בדיקה אם המהלך יוצר איום (רצף של 3)
        score += countThreats(tempModel.getBoard(), playerNumber) * 20;

        // בדיקה אם המהלך חוסם איום של היריב
        score += countThreats(tempModel.getBoard(), 1 - playerNumber) * (-15);

        // בדיקת כמה אפשרויות סיבוב יכולות להוביל לניצחון אחרי המהלך
        int winningRotationsCount = countWinningRotations(tempModel);
        score += winningRotationsCount * 25;

        // בונוס אם המהלך הוא חלק מדפוס אסטרטגי
        if (isPartOfStrategicPattern(row, col)) {
            score += 10;
        }

        return score;
    }

    /**
     * ספירת אפשרויות סיבוב שיובילו לניצחון
     */
    private int countWinningRotations(PentagoModel model) {
        int count = 0;

        // בדיקת כל אפשרויות הסיבוב
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int dir = 0; dir < 2; dir++) {
                boolean clockwise = (dir == 1);

                // יצירת לוח זמני לבדיקת הסיבוב
                PentagoModel rotationModel = createTempModelFrom(model);
                rotationModel.getBoard().rotateQuadrant(quadrant, clockwise);

                // בדיקה אם הסיבוב מוביל לניצחון
                if (rotationModel.getBoard().hasWinningLine(playerNumber)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * בדיקה אם מיקום הוא חלק מדפוס אסטרטגי
     */
    private boolean isPartOfStrategicPattern(int row, int col) {
        for (List<int[]> pattern : strategicPatterns.values()) {
            for (int[] pos : pattern) {
                if (pos[0] == row && pos[1] == col) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * יצירת מודל זמני על בסיס מודל קיים
     */
    private PentagoModel createTempModelFrom(PentagoModel sourceModel) {
        PentagoModel tempModel = new PentagoModel();
        BitBoardRepresentation sourceBoard = sourceModel.getBoard();
        BitBoardRepresentation tempBoard = tempModel.getBoard();

        // העתקת מצב הלוח
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                int piece = sourceBoard.getPieceAt(i, j);
                if (piece != -1) {
                    tempBoard.placePiece(i * 6 + j, piece);
                }
            }
        }

        return tempModel;
    }

    /**
     * קבלת כל המהלכים האפשריים
     */
    private List<int[]> getAllPossibleMoves() {
        List<int[]> moves = new ArrayList<>();
        BitBoardRepresentation board = model.getBoard();

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (board.isPositionEmpty(i * 6 + j)) {
                    moves.add(new int[]{i, j});
                }
            }
        }

        return moves;
    }

    /**
     * מהלך דפוס אסטרטגי משופר
     */
    private int[] getAdvancedPatternMove() {
        // בחירת דפוס אסטרטגי מתאים למצב המשחק הנוכחי
        String bestPatternKey = selectBestPattern();

        if (bestPatternKey != null && strategicPatterns.containsKey(bestPatternKey)) {
            List<int[]> pattern = strategicPatterns.get(bestPatternKey);
            List<int[]> availablePositions = new ArrayList<>();

            // בדיקת אילו משבצות בדפוס פנויות
            for (int[] pos : pattern) {
                if (getPieceAt(pos[0], pos[1]) == -1) {
                    availablePositions.add(pos);
                }
            }

            // בדיקת אילו משבצות בדפוס כבר תפוסות על ידינו
            int ownedCount = 0;
            for (int[] pos : pattern) {
                if (getPieceAt(pos[0], pos[1]) == playerNumber) {
                    ownedCount++;
                }
            }

            // אם יש משבצות פנויות בדפוס, ויש לנו כבר כלים בדפוס, בחר אחת
            if (!availablePositions.isEmpty() && ownedCount > 0) {
                return availablePositions.get(random.nextInt(availablePositions.size()));
            }
            // אם אין לנו עדיין כלים בדפוס, בחר בהסתברות גבוהה
            else if (!availablePositions.isEmpty() && random.nextInt(10) < 8) {
                return availablePositions.get(random.nextInt(availablePositions.size()));
            }
        }

        // אם לא מצאנו דפוס מתאים, נחזור למהלך דפוס רגיל
        return getPatternMove();
    }

    /**
     * בחירת הדפוס האסטרטגי הטוב ביותר למצב הנוכחי
     */
    private String selectBestPattern() {
        Map<String, Integer> patternScores = new HashMap<>();

        // חישוב ציון לכל דפוס
        for (String patternKey : strategicPatterns.keySet()) {
            List<int[]> pattern = strategicPatterns.get(patternKey);
            int score = 0;

            // כמה משבצות בדפוס כבר תפסנו
            int ownedCount = 0;
            // כמה משבצות בדפוס היריב תפס
            int opponentCount = 0;
            // כמה משבצות בדפוס פנויות
            int emptyCount = 0;

            for (int[] pos : pattern) {
                int piece = getPieceAt(pos[0], pos[1]);
                if (piece == playerNumber) {
                    ownedCount++;
                    score += 2;
                } else if (piece == 1 - playerNumber) {
                    opponentCount++;
                    score -= 3;
                } else {
                    emptyCount++;
                    score += 1;
                }
            }

            // בונוס אם יש לנו יותר מכלי אחד בדפוס
            if (ownedCount > 1) {
                score += ownedCount * 3;
            }

            // קנס אם היריב שולט בדפוס
            if (opponentCount > pattern.size() / 3) {
                score -= 10;
            }

            // שמירת הציון הסופי
            patternScores.put(patternKey, score);
        }

        // בחירת הדפוס בעל הציון הגבוה ביותר
        String bestPattern = null;
        int bestScore = Integer.MIN_VALUE;

        for (Map.Entry<String, Integer> entry : patternScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestPattern = entry.getKey();
            }
        }

        // אם כל הדפוסים קיבלו ציון נמוך, נחזיר null
        if (bestScore < 0) {
            return null;
        }

        return bestPattern;
    }

    /**
     * חיפוש סיבוב אסטרטגי משופר
     */
    private int[] findAdvancedStrategicRotation() {
        int bestScore = Integer.MIN_VALUE;
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

                // חישוב הערכה מתקדמת של מצב הלוח לאחר הסיבוב
                int score = evaluateAdvancedRotationScore(tempBoard, quadrant, clockwise);

                if (score > bestScore) {
                    bestScore = score;
                    bestRotation = new int[]{quadrant, direction};
                }
            }
        }

        return bestRotation;
    }

    /**
     * הערכה מתקדמת של ציון סיבוב
     */
    private int evaluateAdvancedRotationScore(BitBoardRepresentation board, int quadrant, boolean clockwise) {
        int score = 0;

        // בונוס למספר האיומים שלנו (רצף של 3 + משבצת ריקה)
        score += 5 * countThreats(board, playerNumber);

        // מינוס למספר האיומים של היריב
        score -= 8 * countThreats(board, 1 - playerNumber);

        // בדיקת דפוסים אסטרטגיים
        for (String patternKey : strategicPatterns.keySet()) {
            int patternScore = evaluatePatternAfterRotation(board, strategicPatterns.get(patternKey));
            score += patternScore;
        }

        // בדיקת מספר הכלים ברביע ומספר הכלים שלנו לאחר הסיבוב
        int piecesInQuadrant = countPiecesInQuadrant(board, quadrant);
        int ourPiecesInQuadrant = countPlayerPiecesInQuadrant(board, quadrant, playerNumber);

        // בונוס אם רוב הכלים ברביע הם שלנו
        if (piecesInQuadrant > 0 && ourPiecesInQuadrant > piecesInQuadrant / 2) {
            score += 5;
        }

        // בדיקת עמדות אסטרטגיות
        score += evaluateStrategicPositionsAfterRotation(board);

        // מרכיב אקראי קטן
        score += random.nextInt(3);

        return score;
    }

    /**
     * הערכת ערך של דפוס לאחר סיבוב
     */
    private int evaluatePatternAfterRotation(BitBoardRepresentation board, List<int[]> pattern) {
        int score = 0;
        int ownedCount = 0;
        int opponentCount = 0;

        for (int[] pos : pattern) {
            int piece = board.getPieceAt(pos[0], pos[1]);
            if (piece == playerNumber) {
                ownedCount++;
            } else if (piece == 1 - playerNumber) {
                opponentCount++;
            }
        }

        // חישוב ציון בהתאם לשליטה בדפוס
        if (ownedCount > pattern.size() / 3) {
            score += ownedCount * 2;
        }

        if (opponentCount > pattern.size() / 3) {
            score -= opponentCount * 2;
        }

        return score;
    }

    /**
     * ספירת כלים ברביע
     */
    private int countPiecesInQuadrant(BitBoardRepresentation board, int quadrant) {
        int count = 0;
        int startRow = (quadrant / 2) * 3;
        int startCol = (quadrant % 2) * 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board.getPieceAt(startRow + i, startCol + j) != -1) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * ספירת כלים של שחקן ספציפי ברביע
     */
    private int countPlayerPiecesInQuadrant(BitBoardRepresentation board, int quadrant, int player) {
        int count = 0;
        int startRow = (quadrant / 2) * 3;
        int startCol = (quadrant % 2) * 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board.getPieceAt(startRow + i, startCol + j) == player) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * הערכת עמדות אסטרטגיות לאחר סיבוב
     */
    private int evaluateStrategicPositionsAfterRotation(BitBoardRepresentation board) {
        int score = 0;

        // בדיקת עמדות מרכזיות
        int[][] strategicSpots = {
                {2, 2}, {2, 3}, {3, 2}, {3, 3}, // מרכז הלוח
                {1, 1}, {1, 4}, {4, 1}, {4, 4}  // מרכזי רביעים
        };

        for (int[] pos : strategicSpots) {
            int piece = board.getPieceAt(pos[0], pos[1]);
            if (piece == playerNumber) {
                score += 3;
            } else if (piece == 1 - playerNumber) {
                score -= 2;
            }
        }

        return score;
    }

    /**
     * מציאת סיבוב חכם (כאשר אין אופציות ברורות)
     */
    private int[] findSmartRandomRotation() {
        // מועדף להשתמש ברביע עם כמות מאוזנת של כלים או ברביע עם כלים של היריב
        List<Integer> potentialQuadrants = new ArrayList<>();
        int[] pieceBalance = new int[4]; // ההפרש בין כלים שלנו לכלים של היריב בכל רביע

        for (int q = 0; q < 4; q++) {
            int ourPieces = countPlayerPiecesInQuadrant(model.getBoard(), q, playerNumber);
            int theirPieces = countPlayerPiecesInQuadrant(model.getBoard(), q, 1 - playerNumber);
            pieceBalance[q] = theirPieces - ourPieces;

            // אם יש יותר כלים של היריב ברביע
            if (pieceBalance[q] > 0) {
                // הוסף את הרביע פעמיים להגדלת הסיכוי לבחירתו
                potentialQuadrants.add(q);
                potentialQuadrants.add(q);
            } else {
                // הוסף את הרביע פעם אחת
                potentialQuadrants.add(q);
            }
        }

        // אם אין רביעים מועדפים, השתמש בכל הרביעים
        if (potentialQuadrants.isEmpty()) {
            for (int q = 0; q < 4; q++) {
                potentialQuadrants.add(q);
            }
        }

        // בחירת רביע אקראי מתוך הרשימה (עם העדפה לרביעים שיש בהם יותר כלים של היריב)
        int selectedQuadrant = potentialQuadrants.get(random.nextInt(potentialQuadrants.size()));

        // החלטה אם לסובב עם כיוון השעון או נגדו
        boolean clockwise = random.nextBoolean();

        return new int[]{selectedQuadrant, clockwise ? 1 : 0};
    }

    /**
     * בדיקת הזדמנות לדפוסים אסטרטגיים
     */
    private boolean hasAdvancedPatternOpportunity() {
        // בדיקת האם יש דפוס שכבר התחלנו
        for (List<int[]> pattern : strategicPatterns.values()) {
            int countPlayerPieces = 0;
            int countEmptySpaces = 0;

            for (int[] pos : pattern) {
                int piece = getPieceAt(pos[0], pos[1]);
                if (piece == playerNumber) {
                    countPlayerPieces++;
                } else if (piece == -1) {
                    countEmptySpaces++;
                }
            }

            // אם יש לנו לפחות כלי אחד בדפוס ויש מקום להרחיב
            if (countPlayerPieces > 0 && countEmptySpaces > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * חיפוש המהלך הטוב ביותר במצב התקפה
     */
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

    /**
     * חיפוש המהלך הטוב ביותר במצב הגנה
     */
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

    /**
     * חיפוש המהלך הטוב ביותר במצב אסטרטגי (שליטה במרכז)
     */
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

    /**
     * חיפוש מהלך לשליטה בקצוות הלוח
     */
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

    /**
     * חיפוש מהלך לחסימת פינות אסטרטגיות
     */
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

    /**
     * חיפוש מהלך לבניית דפוס אסטרטגי
     */
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

    /**
     * חיפוש מהלך שמכין לסיבוב אסטרטגי
     */
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

    /**
     * בחירת מהלך אקראי - כברירת מחדל אחרונה
     */
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

    /**
     * בדיקה אם יש עמדות אסטרטגיות פנויות במרכז הלוח
     */
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

    /**
     * בדיקה אם יש איום על פינות חשובות שכדאי לחסום
     */
    private boolean hasCornersThreat() {
        int[][] corners = {{0, 0}, {0, 5}, {5, 0}, {5, 5}};

        // בדיקה אם היריב כבר תפס פינה
        for (int[] corner : corners) {
            if (getPieceAt(corner[0], corner[1]) == 1 - playerNumber) {
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

    /**
     * בדיקה אם כדאי להתמקד בשליטה בקצוות
     */
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

    /**
     * בדיקה אם יש מהלך שיכול להוביל לניצחון מיידי
     */
    private boolean hasWinningMove(int player) {
        List<int[]> potentialWins = findPotentialLinesWith(player, 4);
        return !potentialWins.isEmpty();
    }

    /**
     * מציאת מהלכים שיכולים להשלים רצף בגודל מסוים
     */
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

    /**
     * בדיקה ספציפית של קו פוטנציאלי (שורה, עמודה או אלכסון)
     */
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
        if (emptyCount == 1 && playerCount == lineSize) {
            moves.add(emptyPos);
        }
    }

    /**
     * חיפוש סיבוב רביע שיוביל לניצחון
     */
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

    /**
     * חיפוש סיבוב רביע שימנע ניצחון של היריב
     */
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

    /**
     * יצירת מודל זמני לסימולציות
     */
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

    /**
     * ספירת איומים ברמת 4 כלים ברצף
     */
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

    /**
     * ספירת איומים בקו ספציפי
     */
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

    /**
     * קבלת ערך במיקום מסוים בלוח
     */
    private int getPieceAt(int row, int col) {
        return model.getBoard().getPieceAt(row, col);
    }

    /**
     * הגדרת מספר השחקן
     */
    public void setPlayerNumber(int player) {
        this.playerNumber = player;
    }

    /**
     * איפוס מונה טורנים (למשחק חדש)
     */
    public void resetTurnCount() {
        this.turnCount = 0;
    }

    /**
     * עדכון התייחסות למודל חדש
     */
    public void setModel(PentagoModel model) {
        this.model = model;
        determineState();
    }
}