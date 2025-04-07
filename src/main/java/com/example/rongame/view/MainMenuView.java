package com.example.rongame.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;


public class MainMenuView extends BorderPane {

    private Button playAgainstPlayerButton;
    private Button playAgainstAIButton;
    private GameModeListener gameModeListener;

    public MainMenuView() {
        // עיצוב המסך הראשי
        setPadding(new Insets(20));
        setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");

        // יצירת כותרת ראשית
        Label titleLabel = new Label("PENTAGO");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        // יצירת כפתור למשחק נגד שחקן אחר
        playAgainstPlayerButton = createMenuButton("Play Against Player");
        playAgainstPlayerButton.setOnAction(e -> {
            if (gameModeListener != null) {
                gameModeListener.onGameModeSelected(GameMode.PLAYER_VS_PLAYER);
            }
        });

        // יצירת כפתור למשחק נגד הAI
        playAgainstAIButton = createMenuButton("Play Against Computer");
        playAgainstAIButton.setOnAction(e -> {
            if (gameModeListener != null) {
                gameModeListener.onGameModeSelected(GameMode.PLAYER_VS_AI);
            }
        });

        // יצירת מיכל לכותרות
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().addAll(titleLabel);

        // יצירת מיכל לכפתורים
        VBox buttonBox = new VBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(playAgainstPlayerButton, playAgainstAIButton);

        // יצירת מיכל מרכזי
        VBox centerBox = new VBox(50);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(titleBox, buttonBox);

        // הוספת מיכל מרכזי לחלון
        setCenter(centerBox);
    }

    // יצירת כפתור תפריט
    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(250);
        button.setPrefHeight(60);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // עיצוב כפתור
        button.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);"
        );

        // הוספת אפקטים בעת מעבר עכבר
        button.setOnMouseEntered(e ->
                button.setStyle(
                        "-fx-background-color: #2980b9; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 5px; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 5, 0, 0, 1);" +
                                "-fx-cursor: hand;"
                )
        );

        button.setOnMouseExited(e ->
                button.setStyle(
                        "-fx-background-color: #3498db; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 5px; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);"
                )
        );

        return button;
    }

    // הגדרת מאזין למצב משחק נבחר
    public void setGameModeListener(GameModeListener listener) {
        this.gameModeListener = listener;
    }

    // מצבי המשחק האפשריים
    public enum GameMode {
        PLAYER_VS_PLAYER,
        PLAYER_VS_AI
    }

    // ממשק למאזין בחירת מצב משחק
    public interface GameModeListener {
        void onGameModeSelected(GameMode mode);
    }
}