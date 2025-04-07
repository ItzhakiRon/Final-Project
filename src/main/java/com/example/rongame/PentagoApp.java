package com.example.rongame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.rongame.model.PentagoModel;
import com.example.rongame.presenter.PentagoPresenter;
import com.example.rongame.view.PentagoView;

public class PentagoApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // יצירת הרכיבים של ארכיטקטורת MVP
        PentagoModel model = new PentagoModel();
        PentagoView view = new PentagoView();
        PentagoPresenter presenter = new PentagoPresenter(model, view);

        // יצירת חלון המשחק
        Scene scene = new Scene(view, 800, 700);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        primaryStage.setTitle("PENTAGO GAME");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
