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
    private Button backToMenuButton;

    // מצב המשחק (PLACE_PIECE, ROTATE_QUADRANT, GAME_OVER)
    private GamePhase currentPhase;

    // סוג המשחק (נגד שחקן או נגד מחשב)
    private GameMode gameMode = GameMode.HUMAN;

    public enum GamePhase {
        PLACE_PIECE, ROTATE_QUADRANT, GAME_OVER
    }

    public enum GameMode {
        HUMAN, AI
    }

    public PentagoView() {
        createUI();
        currentPhase = GamePhase.PLACE_PIECE;
        updateInstructions();

        // הגדרת סגנון רקע עם תמונת הרקע
        getStyleClass().add("game-background");
    }

    private void createUI() {
        // האזור העליון למלל
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(20, 0, 20, 0));
        topBox.setAlignment(Pos.CENTER);

        statusLabel = new Label("PENTAGO");
        statusLabel.setFont(Font.font("Arial", 36));
        // שינוי צבע הטקסט לשחור
        statusLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 3, 0, 0, 1);");

        instructionLabel = new Label("Player Black, place a piece");
        instructionLabel.setFont(Font.font("Arial", 18));
        // שינוי צבע הטקסט לשחור
        instructionLabel.setStyle("-fx-text-fill: #000000;");

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
        HBox bottomBox = new HBox(20); // מרווח גדול יותר בין הכפתורים
        bottomBox.setPadding(new Insets(20, 0, 20, 0));
        bottomBox.setAlignment(Pos.CENTER);

        // יצירת כפתור משחק חדש
        newGameButton = createStyledButton("New Game", "#3498db");

        // יצירת כפתור חזרה לתפריט
        backToMenuButton = createStyledButton("Back to Menu", "#e74c3c");

        bottomBox.getChildren().addAll(newGameButton, backToMenuButton);
        setBottom(bottomBox);

        // הגדרת מסגרת עם padding של 20px מסביב
        setPadding(new Insets(20));
    }

    // יצירת כפתור מעוצב עם צבע מתאים
    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);

        // עיצוב כפתור
        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10px 20px; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

        // הוספת אפקט מעבר עכבר
        String darkerColor = getDarkerColor(color);

        button.setOnMouseEntered(e ->
                button.setStyle(
                        "-fx-background-color: " + darkerColor + "; " +
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

        button.setOnMouseExited(e ->
                button.setStyle(
                        "-fx-background-color: " + color + "; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 10px 20px; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-border-radius: 5; " +
                                "-fx-background-radius: 5; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
                )
        );

        return button;
    }

    // אפקט מעבר עכבר לצבע כהה יותר
    private String getDarkerColor(String hexColor) {
        if (hexColor.equals("#3498db")) return "#2980b9";
        if (hexColor.equals("#e74c3c")) return "#c0392b";
        return hexColor; // מקרה ברירת מחדל
    }

    // עדכון הוראות המשחק
    public void updateInstructions() {
        String playerName = (gameMode == GameMode.AI && boardView.getCurrentPlayer() == 1) ? "Computer" :
                ((boardView.getCurrentPlayer() == 0) ? "Black" : "Red");

        if (currentPhase == GamePhase.PLACE_PIECE) {
            instructionLabel.setText("Player " + playerName + ", place a piece");
        } else if (currentPhase == GamePhase.ROTATE_QUADRANT) {
            instructionLabel.setText("Player " + playerName + ", rotate a quadrant");
        } else if (currentPhase == GamePhase.GAME_OVER) {
            instructionLabel.setText("Game Over! Press 'New Game' to restart");
        }
    }

    // עדכון סטטוס המשחק
    public void updateGameStatus(String status) {
        statusLabel.setText(status);
    }

    // החלפת שלב המשחק (הנחת כלי או סיבוב רביע)
    public void setGamePhase(GamePhase phase) {
        currentPhase = phase;
        boardView.setGamePhase(phase);
        updateInstructions();
    }

    // עדכון מצב הלוח
    public void updateBoard(int[][] board) {
        boardView.updateBoard(board);
    }

    // עדכון השחקן הנוכחי
    public void setCurrentPlayer(int player) {
        boardView.setCurrentPlayer(player);
        updateInstructions();
    }

    // הגדרת מצב המשחק (נגד אדם או נגד מחשב)
    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
        updateInstructions();
    }

    // מגדירי גישה
    public BoardView getBoardView() {
        return boardView;
    }

    public Button getNewGameButton() {
        return newGameButton;
    }

    public Button getBackToMenuButton() {
        return backToMenuButton;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}