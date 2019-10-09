package server.webServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
    ServerSocket serverSocket;
    protected void start() {
        System.out.println("Webserver starting up on port 3000");
        System.out.println("(press ctrl-c to exit)");
        try {
            // create the main server socket
            serverSocket = new ServerSocket(3000);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return;
        }

        System.out.println("Waiting for connection");
        for (;;) {
            try {
                // wait for a connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection from:" + clientSocket.getInetAddress() + " || New client request received :" + clientSocket);
                ClientThread ct = new ClientThread(clientSocket);
                ct.start();

                // remote is now the connected socket
                System.out.println("Connection, sending data.");

                // read the data sent. We basically ignore it,
                // stop reading once a blank line is hit. This
                // blank line signals the end of the client HTTP
                // headers.
//                String str = ".";
//                while (!str.equals(""))
//                    str = in.readLine();

                // Send the response
                // Send the headers
            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }



    public void analyzeClientRequest(String request) {
        String[] splitRequest = request.split("/");
    }

    public static void main(String args[]) {
        WebServer ws = new WebServer();
        ws.start();
    }

}
