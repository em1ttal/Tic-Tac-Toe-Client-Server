package p1.server;

public class GameLogic {
    private final int[][] board;
    /* 1:Client, 2:Server */
    private int turn;
    private int winner;
    private boolean gameEnded;

    public GameLogic() {
        this.board = new int[3][3];
        this.turn = 1;
        this.gameEnded = false;
    }

    private int validateMove(String move) {
        if (move.length()!= 3) return 0; // Invalid format
        if (move.charAt(1) != '-') return 0;
        String[] parts = move.split("-");

        if (parts.length != 2) return 0;

        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);
        if (row < 0 || row > 2 || col < 0 || col > 2) return 0;
        if (board[row][col] != 0) return 1; // Invalid move
        return -1; // Success
    }

    public int clientMove(String move) {
        int validMove = validateMove(move);
        if (validMove != -1) return validMove;
        String[] parts = move.split("-");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);
        board[row][col] = 1;

        System.out.println("Client Moved");
        printBoard();
        if (clientWon()) {
            gameEnded = true;
            winner = 1;
        } else if (gameDraw()) {
            gameEnded = true;
            winner = 0;
        } else {
            turn = 2;
        }
        return validMove;
    }

    public int[] minimax(boolean isMaximizing) {
        if (clientWon()) return new int[]{-1, 0, 0};
        if (serverWon()) return new int[]{1, 0, 0};
        if (gameDraw()) return new int[]{0, 0, 0};

        if (isMaximizing) {
            int[] bestScore = {Integer.MIN_VALUE, 0, 0};
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == 0) {
                        board[i][j] = 2;
                        int[] score = minimax(false);
                        board[i][j] = 0;
                        if (score[0] > bestScore[0]) bestScore = new int[]{score[0], i, j};
                    }
                }
            }
            return bestScore;
        } else {
            int[] bestScore = {Integer.MAX_VALUE, 0, 0};
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == 0) {
                        board[i][j] = 1;
                        int[] score = minimax(true);
                        board[i][j] = 0;
                        if (score[0] < bestScore[0]) bestScore = new int[]{score[0], i, j};
                    }
                }
            }
            return bestScore;
        }
    }

    public void printBoard() {
        for (int i = 0; i < 3; i++) {
            if (i > 0) System.out.println("-------------");
            System.out.print("| ");
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) {
                    System.out.print("  | ");
                } else if (board[i][j] == 1) {
                    System.out.print("X | ");
                } else {
                    System.out.print("O | ");
                }
            }
            System.out.println();
        }
        System.out.println("\n\n");
    }

    public String serverMove() {
        int[] bestMove = minimax(true);
        board[bestMove[1]][bestMove[2]] = 2;
        System.out.println("Server Moved");
        printBoard();
        if (serverWon()) {
            gameEnded = true;
            winner = 2;
        } else if (gameDraw()) {
            gameEnded = true;
            winner = 0;
        } else {
            turn = 1;
        }
        return bestMove[1] + "-" + bestMove[2];
    }

    public boolean isGameEnded() {
        return this.gameEnded;
    }

    public String getWinner() {
        switch (winner) {
            case 0:
                return "Draw";
            case 1:
                return "Client";
            case 2:
                return "Server";
            default:
                return "No winner";
        }
    }

    private boolean clientWon() {
        // Code for checking if game is won
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == 1 && board[i][1] == 1 && board[i][2] == 1) return true;
            if (board[0][i] == 1 && board[1][i] == 1 && board[2][i] == 1) return true;
        }
        if (board[0][0] == 1 && board[1][1] == 1 && board[2][2] == 1) return true;
        return board[0][2] == 1 && board[1][1] == 1 && board[2][0] == 1;
    }

    private boolean serverWon() {
        // Code for checking if game is won
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == 2 && board[i][1] == 2 && board[i][2] == 2) return true;
            if (board[0][i] == 2 && board[1][i] == 2 && board[2][i] == 2) return true;
        }
        if (board[0][0] == 2 && board[1][1] == 2 && board[2][2] == 2) return true;
        return board[0][2] == 2 && board[1][1] == 2 && board[2][0] == 2;
    }

    private boolean gameDraw() {
        // Code for checking if game is a draw
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) return false;
            }
        }
        return !clientWon() && !serverWon();
    }

    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }
}