package com.example.rongame.view;

import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.example.rongame.view.BoardView.CellClickListener;
import com.example.rongame.view.BoardView.QuadrantRotationListener;
import com.example.rongame.view.PentagoView.GamePhase;

public class QuadrantView extends StackPane {

    private int quadrantIndex;
    private GridPane cellsGrid;
    private Circle[][] cellCircles;

    private CellClickListener cellClickListener;
    private QuadrantRotationListener rotationListener;

    private GamePhase currentPhase;

    // הגדרת צבעים
    private static final Color EMPTY_COLOR = Color.LIGHTGRAY;
    private static final Color BLACK_COLOR = Color.BLACK;
    private static final Color RED_COLOR = Color.RED;
    private static final Color QUADRANT_HIGHLIGHT_COLOR = Color.LIGHTGREEN;

    // הגדרות אנימציה
    private static final double ROTATION_DURATION = 0.5; // חצי שנייה

    public QuadrantView(int index) {
        this.quadrantIndex = index;
        this.currentPhase = GamePhase.PLACE_PIECE;

        // עיצוב הרביע - גבול דק
        setPadding(new Insets(5));
        setStyle("-fx-background-color: #DDDDDD; -fx-border-color: #333333; -fx-border-width: 1px;");

        // יצירת רשת תאים 3x3 ללא מרווחים
        cellsGrid = new GridPane();
        cellsGrid.setHgap(0); // הסרת המרווח האופקי
        cellsGrid.setVgap(0); // הסרת המרווח האנכי
        cellsGrid.setAlignment(Pos.CENTER);

        cellCircles = new Circle[3][3];

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                StackPane cellPane = new StackPane();
                cellPane.setPrefSize(60, 60);
                // גבול דק בצבע כהה להפרדה קלה בין המשבצות
                cellPane.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #777777; -fx-border-width: 1px;");

                Circle circle = new Circle(20);
                circle.setFill(EMPTY_COLOR);
                circle.setStroke(Color.DARKGRAY);
                circle.setStrokeWidth(1);

                cellCircles[r][c] = circle;
                cellPane.getChildren().add(circle);

                final int row = r;
                final int col = c;

                // אירוע לחיצה על תא
                cellPane.setOnMouseClicked(event -> {
                    if (currentPhase == GamePhase.PLACE_PIECE && cellClickListener != null) {
                        cellClickListener.onCellClick(quadrantIndex, row, col);
                    }
                });

                cellsGrid.add(cellPane, c, r);
            }
        }

        // הוספת תאי הלוח למרכז
        getChildren().add(cellsGrid);
    }

    /**
     * הפעלת אנימציית סיבוב תת-לוח
     * @param clockwise האם הסיבוב הוא בכיוון השעון
     */
    public void animateRotation(boolean clockwise, Runnable onFinished) {
        // יצירת אנימציית סיבוב
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(ROTATION_DURATION), this);

        // הגדרת זווית סיבוב (90 מעלות עם או נגד כיוון השעון)
        if (clockwise) {
            rotateTransition.setByAngle(90);
        } else {
            rotateTransition.setByAngle(-90);
        }

        // הגדרת פעולה לאחר סיום האנימציה
        rotateTransition.setOnFinished(e -> {
            // איפוס זווית הסיבוב של הרכיב הגרפי (המודל כבר מעודכן)
            setRotate(0);

            // הפעלת פעולה שהועברה
            if (onFinished != null) {
                onFinished.run();
            }
        });

        // הפעלת האנימציה
        rotateTransition.play();
    }

    // עדכון מצב תא בלוח
    public void updateCell(int row, int col, int state) {
        Circle circle = cellCircles[row][col];

        switch (state) {
            case -1: // ריק
                circle.setFill(EMPTY_COLOR);
                break;
            case 0: // שחור
                circle.setFill(BLACK_COLOR);
                break;
            case 1: // אדום
                circle.setFill(RED_COLOR);
                break;
        }
    }


    // מעבר בין שלבי המשחק
    public void setGamePhase(GamePhase phase) {
        this.currentPhase = phase;
    }

    // מגדירי גישה
    public void setCellClickListener(CellClickListener listener) {
        this.cellClickListener = listener;
    }

    public void setQuadrantRotationListener(QuadrantRotationListener listener) {
        this.rotationListener = listener;
    }

    public QuadrantRotationListener getQuadrantRotationListener() {
        return this.rotationListener;
    }

    public int getQuadrantIndex() {
        return this.quadrantIndex;
    }

}