package server.webServer;

import java.io.*;
import java.net.Socket;

public class ClientThread extends Thread{
    private Socket socket;
    private BufferedReader socIn;
    private PrintWriter socOut;

    public ClientThread(Socket s) throws IOException {
        this.socket = s;
        socIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socOut = new PrintWriter(socket.getOutputStream());
    }

    public void run() {
        while (true) {
            try {
                String message = socIn.readLine();
                System.out.println("Client Thread received message: " + message);
                get("test.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        socOut.println(message);
    }

    public void sendTestMessage() {
        socOut.println("HTTP/1.0 200 OK");
        socOut.println("Content-Type: text/html");
        socOut.println("Server: Bot");
        // this blank line signals the end of the headers
        socOut.println("");
        // Send the HTML page
        socOut.println("<H1>Welcome to the Ultra Mini-WebServer</H2>");
        socOut.flush();
    }

    public void responseGetRequest (String fileName) {
        File file = new File("C:/Users/Thuy Vu/Desktop/TPServerHTTP/src/server/library/" + fileName);
        try {
            FileReader fr = new FileReader(file);
            BufferedReader bfr = new BufferedReader(fr);
            String line="";
            while (line != null) {
                line = bfr.readLine();
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
