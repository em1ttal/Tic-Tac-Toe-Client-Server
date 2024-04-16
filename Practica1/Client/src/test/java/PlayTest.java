import org.junit.Test;
import static org.junit.Assert.*;

import p1.server.GameHandler;
import utils.ComUtils;

import p1.client.Client;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;

public class PlayTest {

    class ServerEcho extends Thread{

        private int port;
        private ServerSocket ss;
        private Socket socket;
        private ComUtils comutils;

        public ServerEcho(int port) {
            this.port = port;
            setConnection();

        }

        public void run(){
            this.init();
        }

        public ComUtils getComutils(Socket socket) {
            if (comutils == null) {
                try {
                    comutils = new ComUtils(socket.getInputStream(), socket.getOutputStream());
                } catch (IOException e) {
                    throw new RuntimeException("I/O Error when creating the ComUtils:\n"+e.getMessage());
                }
            }
            return comutils;
        }

        public void setConnection() {
            if (this.ss == null) {
                try {
                    ss = new ServerSocket(port);
                    System.out.println("Server up & listening on port "+port+"...\nPress Cntrl + C to stop.");
                } catch (IOException e) {
                    throw new RuntimeException("I/O error when opening the Server Socket:\n" + e.getMessage());
                }
            }
        }

        public Socket getSocket() {
            return this.socket;
        }

        private void init() {
            while(true) {
                try {
                    socket = ss.accept();
                    comutils = getComutils(socket);
                    System.out.println("Client accepted");

                    GameHandler gameHandler = new GameHandler(socket);
                    gameHandler.run();

                    System.out.println("Closing server...");
                    socket.close();
                    ss.close();
                    break;
                } catch (IOException e) {
                    throw new RuntimeException("I/O error when accepting a client:\n" + e.getMessage());
                } catch (SecurityException e) {
                    throw new RuntimeException("Operation not accepted:\n"+e.getMessage());
                } catch (IllegalBlockingModeException e) {
                    throw new RuntimeException("There is no connection ready to be accepted:\n"+e.getMessage());
                }

            }
        }
    }


    @Test
    public void example_test() {

        try {
            (new ServerEcho(3333)).start();
            int id = 0;
            Client client = new Client("localhost", 3333);
            System.out.println("Connection started...");
            ComUtils comUtils = client.getComutils();

            comUtils.getDataOutputStream().writeByte(1);
            comUtils.write_int32(id);
            comUtils.write_string("Paula");
            comUtils.getDataOutputStream().writeByte(0);
            comUtils.getDataOutputStream().writeByte(0);

            byte response = comUtils.getDataInputStream().readByte();
            id = comUtils.read_int32();

            comUtils.getDataOutputStream().writeByte(3);
            comUtils.write_int32(id);

            response = comUtils.getDataInputStream().readByte();
            assertEquals(4, response);
            assertEquals(id, comUtils.read_int32());
            assertEquals(1, comUtils.getDataInputStream().readByte());

            client.getSocket().close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
