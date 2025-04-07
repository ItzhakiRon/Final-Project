package com.example.rongame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.rongame.model.PentagoModel;
import com.example.rongame.presenter.PentagoPresenter;
import com.example.rongame.view.MainMenuView;
import com.example.rongame.view.PentagoView;
import com.example.rongame.view.MainMenuView.GameMode;

public class PentagoApp extends Application {

    private Stage primaryStage;
    private Scene menuScene;
    private Scene gameScene;
    private MainMenuView menuView;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // יצירת מסך פתיחה
        menuView = new MainMenuView();
        menuView.setGameModeListener(this::startGame);

        // יצירת סצינת תפריט
        menuScene = new Scene(menuView, 800, 700);
        menuScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        // הגדרת חלון המשחק
        primaryStage.setTitle("PENTAGO GAME");
        primaryStage.setScene(menuScene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    // התחלת משחק בהתאם למצב שנבחר
    private void startGame(GameMode mode) {
        // יצירת המשתנים של הארכיטקטורת MVP
        PentagoModel model = new PentagoModel();
        PentagoView view = new PentagoView();

        // יצירת תצוגה והגדרת מצב AI תלוי בבחירה
        PentagoPresenter presenter = new PentagoPresenter(model, view, mode == GameMode.PLAYER_VS_AI);

        // הגדרת כפתור חזרה לתפריט
        view.getBackToMenuButton().setOnAction(e -> {
            // עצירת AI אם פועל
            presenter.stopAI();
            // מעבר לתפריט הראשי
            primaryStage.setScene(menuScene);
            primaryStage.setTitle("PENTAGO GAME");
        });

        // התאמת תצוגה למצב המשחק שנבחר
        if (mode == GameMode.PLAYER_VS_AI) {
            view.setGameMode(PentagoView.GameMode.AI);
            view.updateGameStatus("Player VS Computer");
        } else {
            view.setGameMode(PentagoView.GameMode.HUMAN);
            view.updateGameStatus("Player VS Player");
        }

        // יצירת חלון המשחק
        gameScene = new Scene(view, 800, 700);
        gameScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        primaryStage.setScene(gameScene);
        primaryStage.setTitle("PENTAGO - Game in Progress");
    }

    public static void main(String[] args) {
        launch(args);
    }
}