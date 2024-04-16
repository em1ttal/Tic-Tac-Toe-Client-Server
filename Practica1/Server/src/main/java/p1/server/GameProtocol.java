package p1.server;

import utils.ComUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GameProtocol {
    // Session id
    private int id;
    // Opcodes
    private final byte HELLO = (byte) 1;
    private final byte READY = (byte) 2;
    private final byte PLAY = (byte) 3;
    private final byte ADMIT = (byte) 4;
    private final byte ACTION = (byte) 5;
    private final byte RESULT = (byte) 6;
    private final byte ERROR = (byte) 8;
    // ComUtils object to handle the communication
    private final ComUtils comutils;
    // GameLogic object to handle the game logic
    private GameLogic gameLogic;
    private boolean gameEnded;
    private Socket socket;

    /**
     * Constructor
     * @param comutils ComUtils object to handle the communication
     */
    public GameProtocol(Socket socket, ComUtils comutils) {
        this.socket = socket;
        this.comutils = comutils;
        gameLogic = new GameLogic();
    }

    /**
     * Client connection
     * Starts the connection with the client
     */
    public void clientConnection() {
        gameEnded = false;
        try {
            receiveHello();
            sendReady();
        } catch (Exception e) {
            System.err.println("Error in connection: " + e.getMessage());
        }
    }

    /**
     * Receive hello message
     * Receives the hello message from the client
     */
    public void receiveHello() {
        try {
            id = comutils.read_int32();
            String name = comutils.findString();
            // If the id is 0, generate a random id
            if (id == 0) {
                Random rand = new Random();
                id = rand.nextInt(90000) + 10000;
            }
            System.out.println("Client: " + name + ", with id: " + id + " started a game, good luck!");
        } catch (Exception e) {
            throw new RuntimeException("Error in receiveHello: " + e.getMessage());
        }
    }

    /**
     * Send ready message
     * Sends the ready message to the client
     */
    public void sendReady() {
        try {
            comutils.getDataOutputStream().writeByte(READY);
            comutils.write_int32(id);
        } catch (Exception e) {
            throw new RuntimeException("Error in sendReady: " + e.getMessage());
        }
    }

    /**
     * Receive messages
     * Receives the messages from the client
     * @throws IOException If an I/O error occurs
     */
    public void receiveMessages() throws IOException {
        // If the game has ended, start a new game
        if (gameLogic.isGameEnded()) {
            gameLogic = new GameLogic();
        }
        socket.setSoTimeout(30000); // 30 seconds for client to send message
        while (!gameLogic.isGameEnded()) {
            switch (comutils.getDataInputStream().readByte()) {
                case HELLO:
                    clientConnection();
                    break;
                case PLAY:
                    int flag = receivePlay();
                    sendAdmit(flag);
                    break;
                case ACTION:
                    if (gameLogic.isGameEnded()) throw new IOException("Game ended");
                    boolean valid = receiveAction();
                    if (!gameLogic.isGameEnded() && valid) serverMove();
                    break;
                case ERROR:
                    this.receiveError();
                    gameLogic.setGameEnded(true);
                    break;
                default:
                    System.err.println("Invalid opcode");
                    break;
            }
        }
        gameEnded = true;
    }

    /**
     * Receive play message
     * Receives the play message from the client
     * @return 1 if the play message is valid, 0 otherwise
     */
    public int receivePlay() {
        int flag = 0;
        try {
            if (this.id == comutils.read_int32()) {
                flag = 1;
                System.out.println("Client " + id + " wants to play");
            } else {
                sendError(9, "Invalid session id");
            }
        } catch (Exception e) {
            System.err.println("Error in receivePlay: " + e.getMessage());
        }
        return flag;
    }

    /**
     * Send admit message
     * Sends the admit message to the client
     * @param flag Flag to admit the client, depending on the play message
     */
    public void sendAdmit(int flag) {
        try {
            comutils.getDataOutputStream().writeByte(ADMIT);
            comutils.write_int32(id);
            comutils.getDataOutputStream().writeByte(flag);
            System.out.println("Admitting client " + id + " with flag " + flag);
            System.out.println("Tic-Tac-Toe game started");
        } catch (Exception e) {
            System.err.println("Error in sendAdmit: " + e.getMessage());
        }
    }

    /**
     * Receive action message
     * Receives the action message from the client
     * @return True if the action message is valid, false otherwise
     */
    public boolean receiveAction() {
        try {
            if (comutils.read_int32() != id) {
                sendError(9, "Invalid session id");
                return false;
            }
            String action = comutils.read_string(3);
            int clientMove = gameLogic.clientMove(action);
            switch (clientMove){
                case 0:
                    sendError(0, "Unknown move, check the format move format: row-col (from 0 to 2) Example: 0-0");
                    return false;
                case 1:
                    sendError(1, "Invalid move, there is already a piece in that position");
                    return false;
                default:
                    break;
            }
            System.out.println("Client " + id + " played: " + action);
            if (gameLogic.isGameEnded()) {
                sendResult(action);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error in readAction: " + e.getMessage());
        }
        return true;
    }

    /**
     * Server move
     * Makes the server move
     */
    public void serverMove() {
        String move = gameLogic.serverMove();
        if (gameLogic.isGameEnded()) {
            sendResult(move);
            return;
        }
        sendAction(move);
    }

    /**
     * Send action message
     * If the game hasn't ended
     * Sends the action message to the client
     * @param move Move to be sent
     */
    private void sendAction(String move) {
        try {
            comutils.getDataOutputStream().writeByte(ACTION);
            comutils.write_int32(id);
            comutils.write_string(move);
            System.out.println("Server played: " + move);
        } catch (Exception e) {
            System.err.println("Error in sendAction: " + e.getMessage());
        }
    }

    /**
     * Send result message
     * If the game has ended
     * Sends the result message to the client
     * @param move Move to be sent
     */
    private void sendResult(String move) {
        int flag;
        switch (gameLogic.getWinner()) {
            case "Client":
                System.out.println("Client " + id + " won");
                flag = 1;
                move = "---";
                break;
            case "Server":
                System.out.println("Server played: " + move);
                System.out.println("Server won");
                flag = 0;
                break;
            case "Draw":
                System.out.println("Game ended in a draw");
                // No need to implement Server causing a draw as Client will always go first.
                flag = 2;
                break;
            default:
                throw new RuntimeException("Invalid winner");
        }
        try {
            comutils.getDataOutputStream().writeByte(RESULT);
            comutils.write_int32(id);
            comutils.write_string(move);
            comutils.getDataOutputStream().writeByte(flag);
            System.out.println("Game ended");
        } catch (Exception e) {
            System.err.println("Error in sendResult: " + e.getMessage());
        }
    }

    /**
     * Send error message
     * Sends the error message to the client
     * @param errorCode Error code
     * @param errorMessage Error message
     * @throws IOException If an I/O error occurs
     */
    private void sendError(int errorCode, String errorMessage) throws IOException {
        try {
            comutils.getDataOutputStream().writeByte(ERROR);
            comutils.write_int32(id);
            comutils.getDataOutputStream().writeByte(errorCode);
            comutils.write_string(errorMessage);
            comutils.getDataOutputStream().writeByte(0);
            comutils.getDataOutputStream().writeByte(0);
        } catch (Exception e) {
            System.err.println("Error in sendError: " + e.getMessage());
        }
    }

    /**
     * Receive error message
     * Receives the error message from the client
     * @throws IOException If an I/O error occurs
     */
    private void receiveError() throws IOException {
        try {
            if (comutils.read_int32() != id) sendError(9, "Invalid session id");
            int code = comutils.getDataInputStream().readByte();
            String message = comutils.findString();
            System.out.println("Error Code " + code + ": " + message);
        } catch (Exception e) {
            System.err.println("Error in receiveError: " + e.getMessage());
        }
    }
}
