package com.example.rongame.presenter;

import java.util.Observable;
import java.util.Observer;

import com.example.rongame.model.PentagoModel;
import com.example.rongame.model.PentagoModel.GameState;
import com.example.rongame.view.BoardView.CellClickListener;
import com.example.rongame.view.BoardView.QuadrantRotationListener;
import com.example.rongame.view.PentagoView;
import com.example.rongame.view.PentagoView.GamePhase;

public class PentagoPresenter implements Observer {

    private PentagoModel model;
    private PentagoView view;

    public PentagoPresenter(PentagoModel model, PentagoView view) {
        this.model = model;
        this.view = view;

        // הגדרה של המודל בתצוגה
        model.addObserver(this);

        // הגדרת מאזינים לאירועים בתצוגה
        setupViewListeners();

        // עדכון ראשוני של התצוגה
        updateViewFromModel();
    }


    // הגדרת מאזינים לאירועים בתצוגה
    private void setupViewListeners() {
        // מאזין ללחיצות על תאי הלוח
        view.getBoardView().setCellClickListener(new CellClickListener() {
            @Override
            public void onCellClick(int quadrant, int row, int col) {
                if (view.getCurrentPhase() == GamePhase.PLACE_PIECE) {
                    int globalRow = (quadrant / 2) * 3 + row;
                    int globalCol = (quadrant % 2) * 3 + col;
                    handleCellClick(globalRow, globalCol);
                }
            }
        });

        // מאזין לסיבוב כל רביע
        view.getBoardView().setQuadrantRotationListener(new QuadrantRotationListener() {
            @Override
            public void onQuadrantRotation(int quadrant, boolean clockwise) {
                if (view.getCurrentPhase() == GamePhase.ROTATE_QUADRANT) {
                    handleQuadrantRotation(quadrant, clockwise);
                }
            }
        });



        // מאזין למשחק חדש
        view.getNewGameButton().setOnAction(e -> {
            model.resetGame();
        });
    }


    // טיפול בלחיצה על תא בלוח
    private void handleCellClick(int row, int col) {
        // בדיקה שהמשחק עדיין בתהליך
        if (model.getGameState() != PentagoModel.GameState.IN_PROGRESS) {
            return; // אם המשחק הסתיים, מתעלמים מהלחיצה
        }

        boolean moveSuccess = model.makeMove(row, col);

        if (moveSuccess) {
            // מעבר לשלב סיבוב הרביע
            view.setGamePhase(GamePhase.ROTATE_QUADRANT);
        }
    }


    // טיפול בסיבוב רביע
    private void handleQuadrantRotation(int quadrant, boolean clockwise) {
        // נוסיף בדיקה שהמשחק עדיין בתהליך
        if (model.getGameState() != PentagoModel.GameState.IN_PROGRESS) {
            return; // אם המשחק הסתיים, מתעלמים מהלחיצה
        }

        model.rotateQuadrant(quadrant, clockwise);
    }

    // עדכון מצב המשחק והטקסט
    private void updateGameStatus() {
        GameState state = model.getGameState();

        switch (state) {
            case BLACK_WINS:
                view.updateGameStatus("Black Player Wins!");
                // עדכון שלב המשחק למצב "משחק הסתיים"
                view.setGamePhase(GamePhase.GAME_OVER);
                break;
            case WHITE_WINS:
                view.updateGameStatus("Red Player Wins!");
                // עדכון שלב המשחק למצב "משחק הסתיים"
                view.setGamePhase(GamePhase.GAME_OVER);
                break;
            case DRAW:
                view.updateGameStatus("Draw!");
                // עדכון שלב המשחק למצב "משחק הסתיים"
                view.setGamePhase(GamePhase.GAME_OVER);
                break;
            case IN_PROGRESS:
                view.updateGameStatus("PENTAGO");
                break;
        }
    }

    // מעדכן את התצוגה בהתאם למודל
    private void updateViewFromModel() {
        // עדכון מצב הלוח
        int[][] boardState = new int[6][6];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                boardState[r][c] = model.getBoard().getPieceAt(r, c);
            }
        }
        view.updateBoard(boardState);

        // עדכון השחקן הנוכחי
        view.setCurrentPlayer(model.getCurrentPlayer());

        // עדכון סטטוס המשחק
        updateGameStatus();

    }


    // מאזין לשינויים במודל
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
            String notification = (String) arg;

            switch (notification) {
                case "PIECE_PLACED":
                    // אחרי הנחת כלי, אני מוסיף עדכון של הלוח בלי לשנות את שלב המשחק
                    updateViewFromModel();
                    break;

                case "BOARD_UPDATED":
                    // אחרי סיבוב רביע והחלפת שחקן, עוברים לשלב הנחת כלי
                    view.setGamePhase(GamePhase.PLACE_PIECE);
                    updateViewFromModel();
                    break;

                case "GAME_RESET":
                    // איפוס המשחק
                    view.setGamePhase(GamePhase.PLACE_PIECE);
                    updateViewFromModel();
                    break;
            }
        }
    }
}