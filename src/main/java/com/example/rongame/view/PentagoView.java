package com.example.rongame.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class PentagoView extends BorderPane {

    private BoardView boardView;
    private Label statusLabel;
    private Label instructionLabel;
    private Button newGameButton;

    // מצב המשחק (PLACE_PIECE, ROTATE_QUADRANT, GAME_OVER)
    private GamePhase currentPhase;

    public enum GamePhase {
        PLACE_PIECE, ROTATE_QUADRANT, GAME_OVER
    }

    public PentagoView() {
        createUI();
        currentPhase = GamePhase.PLACE_PIECE;
        updateInstructions();

        // הגדרת סגנון בסיסי לכל ה-BorderPane
        setStyle("-fx-background-color: #f0f0f0;");
    }

    private void createUI() {
        // אזור העליון למידע
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(20, 0, 20, 0));
        topBox.setAlignment(Pos.CENTER);

        statusLabel = new Label("PENTAGO");
        statusLabel.setFont(Font.font("Arial", 36));
        statusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");

        instructionLabel = new Label("Player Black, place a piece");
        instructionLabel.setFont(Font.font("Arial", 18));
        instructionLabel.setStyle("-fx-text-fill: #34495e;");

        topBox.getChildren().addAll(statusLabel, instructionLabel);
        setTop(topBox);

        // יצירת אזור מרכזי מורכב
        VBox centerVBox = new VBox();
        centerVBox.setAlignment(Pos.CENTER);

        // יצירת מרווח גמיש למעלה לדחיפת הלוח למטה
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        // יצירת תצוגת הלוח
        boardView = new BoardView();

        // עטיפת הלוח ב-HBox עם מרווחים בצדדים
        HBox boardContainer = new HBox();
        boardContainer.setAlignment(Pos.CENTER);

        // מרווח גמיש בצד שמאל
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        // מרווח גמיש בצד ימין
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // הוספת המרווחים והלוח ל-HBox
        boardContainer.getChildren().addAll(leftSpacer, boardView, rightSpacer);

        // יצירת מרווח גמיש למטה לדחיפת הלוח למעלה
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        // הוספת כל הרכיבים ל-VBox המרכזי
        centerVBox.getChildren().addAll(topSpacer, boardContainer, bottomSpacer);

        // הגדרת המרכז להיות ה-VBox המרכזי
        setCenter(centerVBox);

        // אזור תחתון
        HBox bottomBox = new HBox(10);
        bottomBox.setPadding(new Insets(20, 0, 20, 0));
        bottomBox.setAlignment(Pos.CENTER);

        newGameButton = new Button("New Game");
        newGameButton.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10px 20px; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

        // הוספת אפקט מעבר עכבר באמצעות מאזין אירועים
        newGameButton.setOnMouseEntered(e ->
                newGameButton.setStyle(
                        "-fx-background-color: #2980b9; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 10px 20px; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-border-radius: 5; " +
                                "-fx-background-radius: 5; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
                )
        );

        newGameButton.setOnMouseExited(e ->
                newGameButton.setStyle(
                        "-fx-background-color: #3498db; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 10px 20px; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-border-radius: 5; " +
                                "-fx-background-radius: 5; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
                )
        );

        bottomBox.getChildren().add(newGameButton);
        setBottom(bottomBox);

        // הגדרת מסגרת עם padding של 20px מסביב
        setPadding(new Insets(20));
    }

    /**
     * עדכון הוראות למשתמש
     */
    public void updateInstructions() {
        if (currentPhase == GamePhase.PLACE_PIECE) {
            instructionLabel.setText("Player " + (boardView.getCurrentPlayer() == 0 ? "Black" : "Red") + ", place a piece");
        } else if (currentPhase == GamePhase.ROTATE_QUADRANT) {
            instructionLabel.setText("Player " + (boardView.getCurrentPlayer() == 0 ? "Black" : "Red") + ", rotate a quadrant");
        } else if (currentPhase == GamePhase.GAME_OVER) {
            instructionLabel.setText("Game Over! Press 'New Game' to restart");
        }
    }

    /**
     * עדכון סטטוס המשחק
     */
    public void updateGameStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * החלפת שלב המשחק (הנחת כלי או סיבוב רביע)
     */
    public void setGamePhase(GamePhase phase) {
        currentPhase = phase;
        boardView.setGamePhase(phase);
        updateInstructions();
    }

    /**
     * עדכון מצב הלוח
     */
    public void updateBoard(int[][] board) {
        boardView.updateBoard(board);
    }

    /**
     * עדכון השחקן הנוכחי
     */
    public void setCurrentPlayer(int player) {
        boardView.setCurrentPlayer(player);
        updateInstructions();
    }

    // מגדירי גישה
    public BoardView getBoardView() {
        return boardView;
    }

    public Button getNewGameButton() {
        return newGameButton;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }
}