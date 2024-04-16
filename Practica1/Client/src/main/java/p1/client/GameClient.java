package p1.client;

import utils.ComUtils;

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class GameClient {
    /*
    TO DO.
    Class that encapsulates the game's logic. Sequence of states following the established protocol .
     */
    private final ClientProtocol protocol;

    /**
     * Constructor
     *
     * @param socket
     * @param comutils ComUtils object to communicate with the server
     */
    public GameClient(Socket socket, ComUtils comutils) {
        protocol = new ClientProtocol(socket, comutils);
    }

    /**
     * Main method to connect and start the game
     */
    public void run() {
        this.connect(); // sendHello
        this.startGame(); // sendPlay
    }

    /**
     * Connects to the server
     * Sends a Hello message and waits for a Ready message
     */
    private void connect() {
        boolean condition = false;
        while (!condition) {
            try {
                protocol.sendMessage((byte) 1); // sendHello
                condition = protocol.receiveMessages(); // Expects receiveReady
            } catch (SocketException e) {
                System.err.println("You took too long to respond. Goodbye!");
                System.exit(0);
            } catch (SocketTimeoutException e) {
                System.err.println("The server is not responding. Goodbye!");
                System.exit(0);
            }catch (java.io.IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Starts the game
     * Sends a Play message and starts the game
     */
    public void startGame() {
        String playerInput = "";
        System.out.println("Do you want to play Tic-Tac-Toe? (y/n)");
        while (!playerInput.equalsIgnoreCase("y")) {
            playerInput = new Scanner(System.in).nextLine();
            if (playerInput.equals("n")) {
                System.out.println("Goodbye!");
                System.exit(0);
            }
        }
        this.sendPlay(); // sendPlay
        this.playGame();
    }

    /**
     * Plays the game
     * Sends an Action message and plays the game
     */
    public void playGame() {
        this.play(); // Initial play of game
        // If the player wants to play again
        String playerInput = "";
        while (!playerInput.equalsIgnoreCase("n") && !playerInput.equalsIgnoreCase("y")) {
            System.out.println("Do you want to play again? (y/n)");
            playerInput = new Scanner(System.in).nextLine();
        }
        if (playerInput.equalsIgnoreCase("y")) {
            protocol.playAgain();
            this.sendPlay(); // Resends play
            this.playGame(); // Recursive call to play again
        } else {
            System.out.println("Thank you for playing!\nGoodbye!");
        }
    }

    /**
     * Sends a Play message
     * Waits for an Admit message
     */
    private void sendPlay() {
        boolean condition = false;
        while (!condition) {
            try {
                protocol.sendMessage((byte) 3); // sendPlay
                condition = protocol.receiveMessages(); // Expected receiveAdmit
            }  catch (SocketException e) {
                System.err.println("You took too long to respond. Goodbye!");
                System.exit(0);
            } catch (SocketTimeoutException e) {
                System.err.println("The server is not responding. Goodbye!");
                System.exit(0);
            }catch (java.io.IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

     /**
      * Plays the game
      * Sends an Action message and plays the game
      */
    private void play() {
        boolean condition = false;
        while (!condition) {
            try {
                protocol.sendMessage((byte) 5); // sendAction
                condition = protocol.receiveMessages(); // Expected action or result
            }  catch (SocketException e) {
                System.err.println("You took too long to respond. Goodbye!");
                System.exit(0);
            } catch (SocketTimeoutException e) {
                System.err.println("The server is not responding. Goodbye!");
                System.exit(0);
            }catch (java.io.IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
