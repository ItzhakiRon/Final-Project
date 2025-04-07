package com.example.rongame.model;

import java.util.Observable;

public class PentagoModel extends Observable {
    // BitBoard לייצוג לוח המשחק
    private BitBoardRepresentation board;

    // שחקן נוכחי (0 - שחור, 1 - לבן)
    private int currentPlayer;

    // מצב המשחק (בתהליך, ניצחון, תיקו)
    private GameState gameState;

    // מיקום המהלך האחרון
    private int lastMove;

    // רביע אחרון שסובב
    private int lastRotatedQuadrant;

    // כיוון סיבוב אחרון
    private boolean lastRotationClockwise;

    public enum GameState {
        IN_PROGRESS, WHITE_WINS, BLACK_WINS, DRAW
    }

    public PentagoModel() {
        board = new BitBoardRepresentation();
        currentPlayer = 0; // שחקן שחור מתחיל
        gameState = GameState.IN_PROGRESS;
    }

    // ביצוע מהלך - הנחת כלי משחק במיקום מסוים
    public boolean makeMove(int row, int col) {
        int position = row * 6 + col;

        // בדיקה אם המיקום פנוי
        if (!board.isPositionEmpty(position)) {
            return false;
        }

        // הנחת כלי במיקום המבוקש
        board.placePiece(position, currentPlayer);
        lastMove = position;

        setChanged();
        notifyObservers("PIECE_PLACED");

        return true;
    }

    // סיבוב רביע
    public void rotateQuadrant(int quadrant, boolean clockwise) {
        board.rotateQuadrant(quadrant, clockwise);
        lastRotatedQuadrant = quadrant;
        lastRotationClockwise = clockwise;

        // בדיקת ניצחון
        checkGameState();

        // החלפת שחקן
        if (gameState == GameState.IN_PROGRESS) {
            currentPlayer = 1 - currentPlayer;
        }

        setChanged();
        notifyObservers("BOARD_UPDATED");
    }

    //  בדיקת מצב המשחק (ניצחון או תיקו)
    private void checkGameState() {
        if (board.hasWinningLine(0)) {
            gameState = GameState.BLACK_WINS;
        } else if (board.hasWinningLine(1)) {
            gameState = GameState.WHITE_WINS;
        } else if (board.isBoardFull()) {
            gameState = GameState.DRAW;
        }
    }

    // Getters
    public BitBoardRepresentation getBoard() {
        return board;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return gameState;
    }


    // לאפס את המשחק
    public void resetGame() {
        board = new BitBoardRepresentation();
        currentPlayer = 0;
        gameState = GameState.IN_PROGRESS;
        setChanged();
        notifyObservers("GAME_RESET");
    }
}
