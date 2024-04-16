package p1.client;

import utils.ComUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientProtocol {
    // Opcodes
    private final byte HELLO = (byte) 1;
    private final byte READY = (byte) 2;
    private final byte PLAY = (byte) 3;
    private final byte ADMIT = (byte) 4;
    private final byte ACTION = (byte) 5;
    private final byte RESULT = (byte) 6;
    private final byte ERROR = (byte) 8;
    // Session ID
    private int id;
    private final ComUtils comutils;
    // Table
    private int[][] board;
    // Last move made by client, used to print after server action or result received
    private String lastMove;
    private Socket socket;

    /**
     * Constructor
     * Initializes the Comutils object and the board
     *
     * @param socket
     * @param comutils Comutils object to handle the communication
     */
    public ClientProtocol(Socket socket, ComUtils comutils) {
        this.socket = socket;
        this.comutils = comutils;
        board = new int[3][3];
    }

    /**
     * Sends a message to the server
     * @param opcode Opcode of the message
     * @throws IOException If an I/O error occurs
     */
    public void sendMessage(byte opcode) throws IOException {
        switch (opcode) {
            case HELLO:
                this.sendHello();
                break;
            case PLAY:
                this.sendPlay();
                break;
            case ACTION:
                this.sendAction();
                break;
            default:
                throw new RuntimeException("Invalid opcode");
        }
    }

    /**
     * Receives messages from the server
     * @return True if the game should moveto next state, false otherwise
     * @throws IOException If an I/O error occurs
     */
    public boolean receiveMessages() throws IOException {
        socket.setSoTimeout(5000); // 5 seconds timeout
        byte opcode = comutils.getDataInputStream().readByte();
        switch (opcode) {
            case READY:
                this.receiveReady();
                return true;
            case ADMIT:
                this.receiveAdmit();
                return true;
            case ERROR:
                this.receiveError();
                return false;
            case ACTION:
                this.receiveAction();
                return false;
            case RESULT:
                this.receiveResult();
                return true;
            default:
                throw new RuntimeException("Invalid opcode");
        }
    }

    /**
     * Sends a Hello message to the server
     * @throws IOException If an I/O error occurs
     */
    public void sendHello() throws IOException {
        // Asks for name
        Scanner sc = new Scanner(System.in);
        String name = "";
        while (name.isEmpty()) {
            System.out.println("Welcome to the game! What's your name:");
            name = sc.nextLine();
        }
        comutils.getDataOutputStream().writeByte(HELLO);
        comutils.write_int32(id); // Initial id is 0
        comutils.write_string(name);
        comutils.getDataOutputStream().writeByte(0);
        comutils.getDataOutputStream().writeByte(0);
    }

    /**
     * Receives a Ready message from the server
     * @throws IOException If an I/O error occurs
     */
    public void receiveReady() throws IOException {
        int id = comutils.read_int32();
        // If the id is 0, it's the first time the client receives a message, else there is an error
        if (this.id == 0) {
            this.id = id;
        } else if (this.id != id) {
            throw new RuntimeException("Invalid id");
        }
    }

    /**
     * Sends a Play message to the server
     * @throws IOException If an I/O error occurs
     */
    public void sendPlay() throws IOException {
        comutils.getDataOutputStream().writeByte(PLAY);
        comutils.write_int32(id);
    }

    /**
     * Receives an Admit message from the server, if admitted starts game
     * @throws IOException If an I/O error occurs
     */
    public void receiveAdmit() throws IOException {
        int id = comutils.read_int32();
        int flag = comutils.getDataInputStream().readByte();
        if (this.id != id || flag == 0) {
            throw new RuntimeException("Invalid id or not admitted");
        } else {
            System.out.println("Admitted to play");
            System.out.println("Game has started");
            System.out.println("You are 'X' and the server is 'O'");

            for (int i = 0; i < 3; i++) {
                if (i > 0) System.out.println("-------------------");
                System.out.print("| ");
                for (int j = 0; j < 3; j++) {
                    System.out.print(i + "-" + j +  " | ");
                }
                System.out.println();
            }
            System.out.println("\n\n");
        }
    }

    /**
     * Prints the board after a move
     * @param move Move made by the client or server
     * @param moved 1 if client moved, 2 if server moved
     */
    private void printBoard(String move, int moved) {
        int row = Integer.parseInt(move.split("-")[0]);
        int col = Integer.parseInt(move.split("-")[1]);
        if (moved == 1) {
            System.out.println("You have played: " + lastMove);
            board[row][col] = 1;
        } else if (moved == 2) {
            System.out.println("Server has played: " + move);
            board[row][col] = 2;
        }
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

    /**
     * Sends an Action message to the server
     * @throws IOException If an I/O error occurs
     */
    public void sendAction() throws IOException {
        lastMove = "";
        String choice = "";
        while (!choice.equalsIgnoreCase("M") && !choice.equalsIgnoreCase("A")) {
            System.out.println("Do you want to make the move or do you want it to be automatic? (M/A)");
            choice = new Scanner(System.in).nextLine();
        }
        if (choice.equalsIgnoreCase("A")) {
            int[] autoMove = minimaxClient(true);
            lastMove = autoMove[1] + "-" + autoMove[2];
        } else {
            /* Asks for the move to the client, checks for correct format */
            while (lastMove.length() != 3) {
                System.out.println("Enter your move with the following format: row-col (from 0 to 2) Example: 0-0");
                lastMove = new Scanner(System.in).nextLine();
            }
        }
        comutils.getDataOutputStream().writeByte(ACTION);
        comutils.write_int32(id);
        comutils.write_string(lastMove);
    }

    /**
     * Receives an Action message from the server
     * @throws IOException If an I/O error occurs
     */
    public void receiveAction() throws IOException {
        if (comutils.read_int32() != id) throw new RuntimeException("Invalid id");
        printBoard(lastMove, 1);
        String action = comutils.read_string(3);
        printBoard(action, 2);
    }

    /**
     * Receives a Result message from the server
     * @throws IOException If an I/O error occurs
     */
    public void receiveResult() throws IOException {
        if (comutils.read_int32() != id) throw new RuntimeException("Invalid id");
        String position = comutils.read_string(3);
        int result = comutils.getDataInputStream().readByte();
        printBoard(lastMove, 1);
        /* Prints the result of the game */
        switch (result) {
            case 0:
                System.out.println("Last move played: " + position);
                printBoard(position, 2);
                System.out.println("The Server has won.\nBetter luck next time.");
                break;
            case 1:
                System.out.println("Last move played: " + position);
                System.out.println("Congratulations!! You Win!!");
                break;
            case 2:
                System.out.println("It's a draw!");
                break;
            default:
                throw new RuntimeException("Invalid result");
        }
    }

    /**
     * Sends an Error message to the server
     * @param errorCode Error code
     * @param error Error message
     * @throws IOException If an I/O error occurs
     */
    public void sendError(int errorCode, String error) throws IOException {
        comutils.getDataOutputStream().writeByte(ERROR);
        comutils.write_int32(id);
        comutils.getDataOutputStream().writeByte(errorCode);
        comutils.write_string(error);
        comutils.getDataOutputStream().writeByte(0);
        comutils.getDataOutputStream().writeByte(0);
    }

    /**
     * Receives an Error message from the server
     * @throws IOException If an I/O error occurs
     */
    public void receiveError() throws IOException {
        if (comutils.read_int32() != id) throw new RuntimeException("Invalid id");
        int code = comutils.getDataInputStream().readByte();
        String message = comutils.findString();
        System.out.println(message);
    }

    /**
     * Resets the board to play again
     */
    public void playAgain() {
        lastMove = "";
        board = new int[3][3];
    }

    /**
     * Minimax algorithm to calculate the best move for the client
     * @param isMaximizing True if the client is maximizing, false otherwise
     * @return Array with the score and the row and column of the best move
     */
    private int[] minimaxClient(boolean isMaximizing) {
        if (clientWon()) return new int[]{1, 0, 0};
        if (serverWon()) return new int[]{-1, 0, 0};
        if (gameDraw()) return new int[]{0, 0, 0};

        if (isMaximizing) {
            int[] bestScore = {Integer.MIN_VALUE, 0, 0};
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == 0) {
                        board[i][j] = 1;
                        int[] score = minimaxClient(false);
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
                        board[i][j] = 2;
                        int[] score = minimaxClient(true);
                        board[i][j] = 0;
                        if (score[0] < bestScore[0]) bestScore = new int[]{score[0], i, j};
                    }
                }
            }
            return bestScore;
        }
    }

    /**
     * Checks if the client has won
     * @return True if the client has won, false otherwise
     */
    private boolean clientWon() {
        // Code for checking if game is won
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == 1 && board[i][1] == 1 && board[i][2] == 1) return true;
            if (board[0][i] == 1 && board[1][i] == 1 && board[2][i] == 1) return true;
        }
        if (board[0][0] == 1 && board[1][1] == 1 && board[2][2] == 1) return true;
        return board[0][2] == 1 && board[1][1] == 1 && board[2][0] == 1;
    }

    /**
     * Checks if the server has won
     * @return True if the server has won, false otherwise
     */
    private boolean serverWon() {
        // Code for checking if game is won
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == 2 && board[i][1] == 2 && board[i][2] == 2) return true;
            if (board[0][i] == 2 && board[1][i] == 2 && board[2][i] == 2) return true;
        }
        if (board[0][0] == 2 && board[1][1] == 2 && board[2][2] == 2) return true;
        return board[0][2] == 2 && board[1][1] == 2 && board[2][0] == 2;
    }

    /**
     * Checks if the game is a draw
     * @return True if the game is a draw, false otherwise
     */
    private boolean gameDraw() {
        // Code for checking if game is a draw
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) return false;
            }
        }
        return !clientWon() && !serverWon();
    }
}
