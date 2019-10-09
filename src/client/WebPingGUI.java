
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

public class WebPingGUI extends JFrame {
    private final String server;
    private final int port;
    private Socket socket;
    private BufferedReader socIn;
    private PrintStream socOut;
    // GUI Elements


    public WebPingGUI(String server, int port) throws IOException {
        this.server = server;
        this.port = port;
        socket = new Socket(server, port);
        socIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socOut = new PrintStream(socket.getOutputStream());
    }





    public static void main(String[] args) {
        WebPing webPingGUI = new WebPing("localhost", 3000);

        if (args.length != 2) {
            System.err.println("Usage java WebPing <server host name> <server port number>");
            return;
        }

        String httpServerHost = args[0];
        int httpServerPort = Integer.parseInt(args[1]);

        try {
            InetAddress addr;
            Socket sock = new Socket(httpServerHost, httpServerPort);
            addr = sock.getInetAddress();
            System.out.println("Connected to " + addr);
            sock.close();
        } catch (java.io.IOException e) {
            System.out.println("Can't connect to " + httpServerHost + ":" + httpServerPort);
            System.out.println(e);
        }
    }

    private class ListenFromServer extends Thread {
        public void run() {
            while (true) {
                String message = null;
                try {
                    message = socIn.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(message);
            }
        }
    }



}