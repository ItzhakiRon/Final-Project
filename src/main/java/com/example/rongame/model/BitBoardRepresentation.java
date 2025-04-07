package com.example.rongame.model;

public class BitBoardRepresentation {
    // הצגת הלוח כשני מספרים ארוכים - אחד לכל שחקן
    // כל ביט מייצג משבצת בלוח 6x6
    private long blackBoard;
    private long whiteBoard;

    // קבועים חשובים
    private static final int BOARD_SIZE = 6;
    private static final int WINNING_LENGTH = 5;

    public BitBoardRepresentation() {
        blackBoard = 0L;
        whiteBoard = 0L;
    }


    // בדיקה האם המיקום פנוי
    public boolean isPositionEmpty(int position) {
        long mask = 1L << position;
        return ((blackBoard | whiteBoard) & mask) == 0;
    }

    //  הנחת כלי משחק במיקום מסוים
    public void placePiece(int position, int player) {
        long mask = 1L << position;
        if (player == 0) {
            blackBoard |= mask;
        } else {
            whiteBoard |= mask;
        }
    }

    // קבלת המצב של משבצת מסוימת (ריק, שחור, לבן)
    public int getPieceAt(int row, int col) {
        int position = row * BOARD_SIZE + col;
        long mask = 1L << position;

        if ((blackBoard & mask) != 0) {
            return 0; // שחור
        } else if ((whiteBoard & mask) != 0) {
            return 1; // לבן
        } else {
            return -1; // ריק
        }
    }

    /**
     * סיבוב רביע בלוח
     * רביעים:
     * 0 | 1
     * -----
     * 2 | 3
     */
    public void rotateQuadrant(int quadrant, boolean clockwise) {
        int startRow = (quadrant / 2) * 3;
        int startCol = (quadrant % 2) * 3;

        // שמירת המצב הנוכחי של הרביע
        int[][] tempBoard = new int[3][3];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                tempBoard[r][c] = getPieceAt(startRow + r, startCol + c);
            }
        }

        // הסרת הכלים הקיימים ברביע
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int position = (startRow + r) * BOARD_SIZE + (startCol + c);
                long mask = ~(1L << position);
                blackBoard &= mask;
                whiteBoard &= mask;
            }
        }

        // הנחת הכלים בסיבוב
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int newR, newC;

                if (clockwise) {
                    newR = c;
                    newC = 2 - r;
                } else {
                    newR = 2 - c;
                    newC = r;
                }

                int piece = tempBoard[r][c];
                if (piece != -1) {
                    int position = (startRow + newR) * BOARD_SIZE + (startCol + newC);
                    placePiece(position, piece);
                }
            }
        }
    }

    //  בדיקה האם קיים רצף מנצח
    public boolean hasWinningLine(int player) {
        long board = (player == 0) ? blackBoard : whiteBoard;

        // בדיקת שורות
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col <= BOARD_SIZE - WINNING_LENGTH; col++) {
                boolean win = true;
                for (int i = 0; i < WINNING_LENGTH; i++) {
                    int pos = row * BOARD_SIZE + (col + i);
                    if (((board >>> pos) & 1) == 0) {
                        win = false;
                        break;
                    }
                }
                if (win) return true;
            }
        }

        // בדיקת עמודות
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row <= BOARD_SIZE - WINNING_LENGTH; row++) {
                boolean win = true;
                for (int i = 0; i < WINNING_LENGTH; i++) {
                    int pos = (row + i) * BOARD_SIZE + col;
                    if (((board >>> pos) & 1) == 0) {
                        win = false;
                        break;
                    }
                }
                if (win) return true;
            }
        }

        // בדיקת אלכסונים (מימין לשמאל)
        for (int row = 0; row <= BOARD_SIZE - WINNING_LENGTH; row++) {
            for (int col = 0; col <= BOARD_SIZE - WINNING_LENGTH; col++) {
                boolean win = true;
                for (int i = 0; i < WINNING_LENGTH; i++) {
                    int pos = (row + i) * BOARD_SIZE + (col + i);
                    if (((board >>> pos) & 1) == 0) {
                        win = false;
                        break;
                    }
                }
                if (win) return true;
            }
        }

        // בדיקת אלכסונים (משמאל לימין)
        for (int row = 0; row <= BOARD_SIZE - WINNING_LENGTH; row++) {
            for (int col = WINNING_LENGTH - 1; col < BOARD_SIZE; col++) {
                boolean win = true;
                for (int i = 0; i < WINNING_LENGTH; i++) {
                    int pos = (row + i) * BOARD_SIZE + (col - i);
                    if (((board >>> pos) & 1) == 0) {
                        win = false;
                        break;
                    }
                }
                if (win) return true;
            }
        }

        return false;
    }

    // בדיקה אם הלוח מלא
    public boolean isBoardFull() {
        return (~(blackBoard | whiteBoard) & ((1L << 36) - 1)) == 0;
    }
}
