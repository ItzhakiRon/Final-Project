package com.example.rongame.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import com.example.rongame.view.PentagoView.GamePhase;

public class BoardView extends StackPane {

    private GridPane boardGrid;
    private QuadrantView[] quadrants;
    private int currentPlayer;
    private GamePhase currentPhase;

    // כפתורי סיבוב
    private Button[] rotationButtons;
    private StackPane boardContainer;
    private Pane buttonOverlay;

    // מימדי לוח
    private static final double BOARD_TOTAL_WIDTH = 400;
    private static final double BOARD_TOTAL_HEIGHT = 400;
    private static final double BOARD_MARGIN = 20;
    private static final double QUADRANT_GAP = 10;

    public BoardView() {
        // יצירת מיכל ללוח המשחק עם רקע אפור
        boardContainer = new StackPane();
        Color grayColor = Color.rgb(80, 80, 80); // צבע אפור כהה
        boardContainer.setBackground(new Background(new BackgroundFill(grayColor, new CornerRadii(10), Insets.EMPTY)));
        boardContainer.setPadding(new Insets(BOARD_MARGIN)); // מרווח מסביב ללוח האפור

        // קביעת גודל קבוע ללוח האפור
        boardContainer.setPrefSize(BOARD_TOTAL_WIDTH, BOARD_TOTAL_HEIGHT);
        boardContainer.setMinSize(BOARD_TOTAL_WIDTH, BOARD_TOTAL_HEIGHT);
        boardContainer.setMaxSize(BOARD_TOTAL_WIDTH, BOARD_TOTAL_HEIGHT);

        // יצירת גריד לארבעת תתי-הלוחות
        boardGrid = new GridPane();
        boardGrid.setHgap(QUADRANT_GAP); // מרווח אופקי בין תתי-הלוחות
        boardGrid.setVgap(QUADRANT_GAP); // מרווח אנכי בין תתי-הלוחות

        // יצירת 4 רביעים לוח המשחק
        quadrants = new QuadrantView[4];

        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int col = i % 2;

            quadrants[i] = new QuadrantView(i);
            boardGrid.add(quadrants[i], col, row);
        }

        // הוספת הגריד למיכל הלוח האפור
        boardContainer.getChildren().add(boardGrid);

        // יצירת שכבת כפתורים בשכבה עליונה
        buttonOverlay = new Pane();
        buttonOverlay.setPrefSize(BOARD_TOTAL_WIDTH + 100, BOARD_TOTAL_HEIGHT + 100); // גדול יותר מהלוח האפור
        buttonOverlay.setPickOnBounds(false); // חשוב - מאפשר לחיצות לעבור דרך המיכל לרכיבים מתחת

        // יצירת כפתורי סיבוב
        createRotationButtons();

        // הוספת השכבות למיכל הראשי
        getChildren().addAll(boardContainer, buttonOverlay);

        currentPlayer = 0; // שחקן שחור מתחיל
        currentPhase = GamePhase.PLACE_PIECE;

        // דחיית עדכון מיקום הכפתורים לאחר שהרכיב מוצג במסך
        Platform.runLater(this::layoutButtons);
    }

    /**
     * יצירת כפתורי סיבוב מסביב לתתי-הלוחות
     */
    private void createRotationButtons() {
        rotationButtons = new Button[8]; // 8 כפתורי סיבוב - 2 לכל תת-לוח

        // גודל הכפתורים
        final double buttonSize = 40;

        // יצירת 8 כפתורי סיבוב - 2 לכל תת-לוח
        for (int i = 0; i < 4; i++) {
            // יצירת כפתורים לכל תת-לוח
            int quadIndex = i;

            // כפתורי הסיבוב
            Button buttonCCW = createArrowButton("↺", quadIndex, false); // נגד כיוון השעון
            Button buttonCW = createArrowButton("↻", quadIndex, true);  // עם כיוון השעון

            // הוספה למערך הכפתורים
            switch (i) {
                case 0: // שמאל עליון
                    rotationButtons[0] = buttonCCW; // למעלה
                    rotationButtons[4] = buttonCW;  // משמאל
                    break;
                case 1: // ימין עליון
                    rotationButtons[1] = buttonCW;  // למעלה
                    rotationButtons[6] = buttonCCW; // מימין
                    break;
                case 2: // שמאל תחתון
                    rotationButtons[2] = buttonCW;  // למטה
                    rotationButtons[5] = buttonCCW; // משמאל
                    break;
                case 3: // ימין תחתון
                    rotationButtons[3] = buttonCCW; // למטה
                    rotationButtons[7] = buttonCW;  // מימין
                    break;
            }
        }

        // הוספת הכפתורים לשכבת הכפתורים
        buttonOverlay.getChildren().addAll(rotationButtons);

        // הסתרת הכפתורים בתחילת המשחק
        updateRotationButtonsVisibility();

        // מאזין לשינוי גודל - לא נדרש כאן כי אנחנו משתמשים בגדלים קבועים
        // widthProperty().addListener((obs, oldVal, newVal) -> layoutButtons());
        // heightProperty().addListener((obs, oldVal, newVal) -> layoutButtons());
    }

    /**
     * עדכון מיקום הכפתורים בהתאם לגודל החלון
     */
    private void layoutButtons() {
        // חישוב גודל של כל תת-לוח
        final double quadrantWidth = (BOARD_TOTAL_WIDTH - 2 * BOARD_MARGIN - QUADRANT_GAP) / 2;
        final double quadrantHeight = (BOARD_TOTAL_HEIGHT - 2 * BOARD_MARGIN - QUADRANT_GAP) / 2;

        // חישוב מיקום הלוח האפור בתוך מיכל הלוח (הניחו שהוא ממורכז)
        double boardX = (getWidth() - BOARD_TOTAL_WIDTH) / 2;
        double boardY = (getHeight() - BOARD_TOTAL_HEIGHT) / 2;

        if (boardX < 0) boardX = 0;
        if (boardY < 0) boardY = 0;

        // גודל הכפתורים
        final double buttonSize = 40;

        // מיקום של תתי הלוחות
        double[][] quadrantPositions = {
                // שמאל עליון (0)
                {boardX + BOARD_MARGIN, boardY + BOARD_MARGIN},
                // ימין עליון (1)
                {boardX + BOARD_MARGIN + quadrantWidth + QUADRANT_GAP, boardY + BOARD_MARGIN},
                // שמאל תחתון (2)
                {boardX + BOARD_MARGIN, boardY + BOARD_MARGIN + quadrantHeight + QUADRANT_GAP},
                // ימין תחתון (3)
                {boardX + BOARD_MARGIN + quadrantWidth + QUADRANT_GAP, boardY + BOARD_MARGIN + quadrantHeight + QUADRANT_GAP}
        };

        // תאם את מיקום הכפתורים
        if (rotationButtons[0] != null) { // וידוא שהכפתורים קיימים
            for (int i = 0; i < 4; i++) {
                double qLeft = quadrantPositions[i][0];
                double qTop = quadrantPositions[i][1];
                double qRight = qLeft + quadrantWidth;
                double qBottom = qTop + quadrantHeight;
                double qCenterX = qLeft + quadrantWidth / 2;
                double qCenterY = qTop + quadrantHeight / 2;

                switch (i) {
                    case 0: // שמאל עליון
                        // כפתור למעלה
                        rotationButtons[0].setLayoutX(qCenterX - buttonSize / 2);
                        rotationButtons[0].setLayoutY(qTop - buttonSize - 5);

                        // כפתור משמאל
                        rotationButtons[4].setLayoutX(qLeft - buttonSize - 5);
                        rotationButtons[4].setLayoutY(qCenterY - buttonSize / 2);
                        break;

                    case 1: // ימין עליון
                        // כפתור למעלה
                        rotationButtons[1].setLayoutX(qCenterX - buttonSize / 2);
                        rotationButtons[1].setLayoutY(qTop - buttonSize - 5);

                        // כפתור מימין
                        rotationButtons[6].setLayoutX(qRight + 5);
                        rotationButtons[6].setLayoutY(qCenterY - buttonSize / 2);
                        break;

                    case 2: // שמאל תחתון
                        // כפתור למטה
                        rotationButtons[2].setLayoutX(qCenterX - buttonSize / 2);
                        rotationButtons[2].setLayoutY(qBottom + 5);

                        // כפתור משמאל
                        rotationButtons[5].setLayoutX(qLeft - buttonSize - 5);
                        rotationButtons[5].setLayoutY(qCenterY - buttonSize / 2);
                        break;

                    case 3: // ימין תחתון
                        // כפתור למטה
                        rotationButtons[3].setLayoutX(qCenterX - buttonSize / 2);
                        rotationButtons[3].setLayoutY(qBottom + 5);

                        // כפתור מימין
                        rotationButtons[7].setLayoutX(qRight + 5);
                        rotationButtons[7].setLayoutY(qCenterY - buttonSize / 2);
                        break;
                }
            }
        }
    }

    /**
     * יצירת כפתור חץ לסיבוב
     */
    private Button createArrowButton(String arrowSymbol, int quadrantIndex, boolean clockwise) {
        Button button = new Button(arrowSymbol);
        button.setPrefSize(40, 40);
        button.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 18px; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );

        // הוספת אפקטים בעת מעבר עכבר
        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: #2980b9; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 18px; " +
                            "-fx-border-radius: 5; " +
                            "-fx-background-radius: 5; " +
                            "-fx-cursor: hand;"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: #3498db; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 18px; " +
                            "-fx-border-radius: 5; " +
                            "-fx-background-radius: 5;"
            );
        });

        // הגדרת פעולה לסיבוב הרביע המתאים
        final int quadIndex = quadrantIndex;
        final boolean isClockwise = clockwise;

        button.setOnAction(e -> {
            if (currentPhase == GamePhase.ROTATE_QUADRANT) {
                // הסתרת כל כפתורי הסיבוב מיד
                for (Button b : rotationButtons) {
                    b.setVisible(false);
                }

                // הפעלת אנימציית סיבוב
                quadrants[quadIndex].animateRotation(isClockwise, () -> {
                    // קוד שירוץ לאחר סיום האנימציה
                    if (quadrants[quadIndex].getQuadrantRotationListener() != null) {
                        quadrants[quadIndex].getQuadrantRotationListener().onQuadrantRotation(quadIndex, isClockwise);
                    }

                    // אין צורך להחזיר את הכפתורים לנראות כי שלב המשחק משתנה
                    // והפונקציה updateRotationButtonsVisibility כבר תטפל בזה
                });
            }
        });

        return button;
    }

    /**
     * עדכון נראות כפתורי הסיבוב בהתאם לשלב המשחק
     */
    private void updateRotationButtonsVisibility() {
        boolean shouldShowButtons = currentPhase == GamePhase.ROTATE_QUADRANT;

        // הצגה או הסתרה של כל הכפתורים
        for (Button button : rotationButtons) {
            button.setVisible(shouldShowButtons);
        }
    }

    /**
     * עדכון מצב הלוח
     */
    public void updateBoard(int[][] boardState) {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                int quadrantIndex = (r / 3) * 2 + (c / 3);
                int localRow = r % 3;
                int localCol = c % 3;

                quadrants[quadrantIndex].updateCell(localRow, localCol, boardState[r][c]);
            }
        }
    }

    /**
     * הגדרת שלב המשחק (הנחה או סיבוב)
     */
    public void setGamePhase(GamePhase phase) {
        this.currentPhase = phase;
        for (QuadrantView quadrant : quadrants) {
            quadrant.setGamePhase(phase);
        }
        updateRotationButtonsVisibility();
    }

    /**
     * הגדרת השחקן הנוכחי
     */
    public void setCurrentPlayer(int player) {
        this.currentPlayer = player;
    }

    /**
     * הגדרת מאזיני אירועים לכל תאי הלוח
     */
    public void setCellClickListener(CellClickListener listener) {
        for (QuadrantView quadrant : quadrants) {
            quadrant.setCellClickListener(listener);
        }
    }

    /**
     * הגדרת מאזיני אירועים לסיבוב רביע בלוח
     */
    public void setQuadrantRotationListener(QuadrantRotationListener listener) {
        for (QuadrantView quadrant : quadrants) {
            quadrant.setQuadrantRotationListener(listener);
        }
    }

    /**
     * סימון המהלך האחרון
     */
    public void highlightLastMove(int row, int col) {
        int quadrantIndex = (row / 3) * 2 + (col / 3);
        int localRow = row % 3;
        int localCol = col % 3;

        quadrants[quadrantIndex].highlightCell(localRow, localCol);
    }

    /**
     * סימון הרביע האחרון שסובב (מיושן - לא בשימוש)
     */
    public void highlightLastRotatedQuadrant(int quadrant) {
        // פונקציה ריקה - לא מדגישה יותר את הרביע האחרון שסובב
    }

    // Getters
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * ממשק למאזין לחיצה על תא בלוח
     */
    public interface CellClickListener {
        void onCellClick(int quadrant, int row, int col);
    }

    /**
     * ממשק למאזין לסיבוב רביע בלוח
     */
    public interface QuadrantRotationListener {
        void onQuadrantRotation(int quadrant, boolean clockwise);
    }
}