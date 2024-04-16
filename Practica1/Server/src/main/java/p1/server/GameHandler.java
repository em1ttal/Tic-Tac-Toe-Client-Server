package p1.server;
import utils.ComUtils;

import java.io.IOException;
import java.net.Socket;

public class GameHandler implements Runnable{

    /*
    TO DO
    Protocol dynamics from Server.
    Methods: run(), init(), play().
     */
    ComUtils comutils;
    private GameProtocol protocol;
    private final Socket socket;

    /**
     * Constructor
     * @param socket Socket to be used
     * @throws IOException If an I/O error occurs
     */
    public GameHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.comutils = new ComUtils(socket.getInputStream(), socket.getOutputStream());
    }

    /**
     * Run method
     * Starts the protocol
     */
    public void run() {
        protocol = new GameProtocol(socket, comutils);
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Init method
     * Connects to the client and starts the game
     * @throws IOException If an I/O error occurs
     */
    public void init() throws IOException {
        try {
            protocol.receiveMessages();
            play();
        } catch (Exception e) {
            socket.close();
            System.out.println("Client disconnected");
        }
    }

    /**
     * Play method
     * Starts the game
     * @throws IOException If an I/O error occurs
     */
    public void play() throws IOException {
        while (true) {
            try {
                protocol.receiveMessages();
            } catch (IOException e) {
                socket.close();
                System.out.println("Client disconnected");
                break;
            }
        }
    }
}
