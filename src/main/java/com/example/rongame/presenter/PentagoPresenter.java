package com.example.rongame.presenter;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;

import com.example.rongame.ai.PentagoAI;
import com.example.rongame.model.PentagoModel;
import com.example.rongame.model.PentagoModel.GameState;
import com.example.rongame.view.BoardView.CellClickListener;
import com.example.rongame.view.BoardView.QuadrantRotationListener;
import com.example.rongame.view.PentagoView;
import com.example.rongame.view.PentagoView.GamePhase;

public class PentagoPresenter implements Observer {

    private PentagoModel model;
    private PentagoView view;
    private PentagoAI ai;
    private boolean isAIEnabled;
    private boolean isAIThinking = false;
    private ExecutorService aiExecutor;

    public PentagoPresenter(PentagoModel model, PentagoView view, boolean enableAI) {
        this.model = model;
        this.view = view;
        this.isAIEnabled = enableAI;

        if (isAIEnabled) {
            // יצירת מופע של ה-AI עם רמת קושי בינונית (7)
            this.ai = new PentagoAI(7);
            this.ai.setPlayerNumber(1); // ה-AI תמיד משחק כצד שני (אדום)
            this.aiExecutor = Executors.newSingleThreadExecutor();
        }

        // הגדרה של המודל בתצוגה
        model.addObserver(this);

        // הגדרת מאזינים לאירועים בתצוגה
        setupViewListeners();

        // עדכון ראשוני של התצוגה
        updateViewFromModel();

        // במידה ו-AI מופעל, צריך לבדוק אם זה תורו להתחיל
        if (isAIEnabled && model.getCurrentPlayer() == 1) {
            makeAIMove();
        }
    }

    // הגדרת מאזינים לאירועים בתצוגה
    private void setupViewListeners() {
        // מאזין ללחיצות על תאי הלוח
        view.getBoardView().setCellClickListener(new CellClickListener() {
            @Override
            public void onCellClick(int quadrant, int row, int col) {
                if (view.getCurrentPhase() == GamePhase.PLACE_PIECE && !isAIThinking) {
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
                if (view.getCurrentPhase() == GamePhase.ROTATE_QUADRANT && !isAIThinking) {
                    handleQuadrantRotation(quadrant, clockwise);
                }
            }
        });

        // מאזין למשחק חדש
        view.getNewGameButton().setOnAction(e -> {
            if (!isAIThinking) {
                model.resetGame();
                if (isAIEnabled && model.getCurrentPlayer() == 1) {
                    makeAIMove();
                }
            }
        });
    }

    // טיפול בלחיצה על תא בלוח
    private void handleCellClick(int row, int col) {
        // בדיקה שהמשחק עדיין בתהליך
        if (model.getGameState() != PentagoModel.GameState.IN_PROGRESS) {
            return; // אם המשחק הסתיים, מתעלמים מהלחיצה
        }

        // אם זה מצב AI, נאפשר רק לשחקן האנושי לשחק כשחקן 0 (שחור)
        if (isAIEnabled && model.getCurrentPlayer() != 0) {
            return; // אם זה תור ה-AI, לא מאפשרים לשחקן לשחק
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

        // אם זה מצב AI, נאפשר רק לשחקן האנושי לשחק כשחקן 0 (שחור)
        if (isAIEnabled && model.getCurrentPlayer() != 0) {
            return;
        }

        model.rotateQuadrant(quadrant, clockwise);

        // לאחר סיבוב הרביע, אם המשחק במצב AI וזה תור ה-AI, מפעילים אותו
        if (isAIEnabled && model.getGameState() == GameState.IN_PROGRESS && model.getCurrentPlayer() == 1) {
            makeAIMove();
        }
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
                if (isAIEnabled) {
                    view.updateGameStatus("Computer Wins!");
                } else {
                    view.updateGameStatus("Red Player Wins!");
                }
                // עדכון שלב המשחק למצב "משחק הסתיים"
                view.setGamePhase(GamePhase.GAME_OVER);
                break;
            case DRAW:
                view.updateGameStatus("Draw!");
                // עדכון שלב המשחק למצב "משחק הסתיים"
                view.setGamePhase(GamePhase.GAME_OVER);
                break;
            case IN_PROGRESS:
                // עדכון סטטוס בהתאם לשחקן נוכחי
                if (isAIEnabled) {
                    if (model.getCurrentPlayer() == 0) {
                        view.updateGameStatus("Your Turn");
                    } else {
                        view.updateGameStatus("Computer's Turn");
                    }
                } else {
                    if (model.getCurrentPlayer() == 0) {
                        view.updateGameStatus("Black's Turn");
                    } else {
                        view.updateGameStatus("Red's Turn");
                    }
                }
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

    /* ביצוע מהלך של ה-AI (הנחת כלי + סיבוב) */
    private void makeAIMove() {
        if (!isAIEnabled || model.getGameState() != GameState.IN_PROGRESS || model.getCurrentPlayer() != 1) {
            return;
        }

        // סימון שה-AI עובד כרגע - למנוע קלט משתמש בזמן מהלך ה-AI
        isAIThinking = true;

        // עדכון הודעה לממשק
        Platform.runLater(() -> {
            view.updateGameStatus("Computer is thinking...");

            // הסתרת כפתורי הסיבוב כשה-AI חושב
            view.getBoardView().hideRotationButtons();
        });

        // הרצת AI בתהליך נפרד
        aiExecutor.submit(() -> {
            try {
                // המתנה קצרה כדי שלמשתמש תהיה תחושה שהמחשב חושב
                Thread.sleep(800);

                // עדכון הלוח באובייקט ה-AI
                int[][] boardState = new int[6][6];
                for (int r = 0; r < 6; r++) {
                    for (int c = 0; c < 6; c++) {
                        boardState[r][c] = model.getBoard().getPieceAt(r, c);
                    }
                }
                ai.updateBoard(boardState);

                // קבלת מהלך הנחת כלי מה-AI
                int[] move = ai.makeMove();
                final int aiRow = move[0];
                final int aiCol = move[1];

                // הנחת הכלי בשרשור UI
                Platform.runLater(() -> {
                    // הנחת הכלי על הלוח
                    model.makeMove(aiRow, aiCol);

                    // עדכון שלב המשחק לסיבוב אך עדיין ללא הצגת כפתורים
                    view.setGamePhase(GamePhase.ROTATE_QUADRANT);
                    view.updateGameStatus("Computer is rotating...");

                    // וידוא שכפתורי הסיבוב נשארים מוסתרים
                    view.getBoardView().hideRotationButtons();
                });

                // המתנה נוספת לפני הסיבוב
                Thread.sleep(1200);

                // קבלת מהלך סיבוב מה-AI
                int[] rotation = ai.makeRotation();
                final int quadrant = rotation[0];
                final boolean clockwise = rotation[1] == 1;

                // ביצוע הסיבוב בשרשור UI
                Platform.runLater(() -> {
                    // הפעלת אנימציה של סיבוב בלי להציג את הכפתורים
                    view.getBoardView().animateRotationWithoutButtons(quadrant, clockwise, () -> {
                        // ביצוע הסיבוב במודל אחרי האנימציה
                        model.rotateQuadrant(quadrant, clockwise);
                        isAIThinking = false;
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                isAIThinking = false;
            }
        });
    }

    // עוצר את ה - AI כשחוזרים לMENU
    public void stopAI() {
        if (aiExecutor != null) {
            aiExecutor.shutdownNow();
        }
    }
}