package server.webServer;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientThread extends Thread{
    private Socket socket;
    private BufferedReader socIn;
    private DataOutputStream socOut;
    private boolean exit;
    private String requestPath;
    private String method;

    public ClientThread(Socket s) throws IOException {
        this.socket = s;
        exit = false;
        socIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socOut = new DataOutputStream(socket.getOutputStream());
    }

    public void run() {
        while (!exit) {
            try {

                String request = socIn.readLine();
                System.out.println("Client Thread received request: " + request);
                if (request == null) {
                    endThread();
                    System.err.println("null message received. Thread is killed");
                }
                if (request != null) {
                    if (request.equals("")) {
                        System.out.println("End of request");
                        endThread();
                    }
                    if (method == null) {
                        // Get the method if method is not defined. This is only executed at the creation of the thread
                        // (avoid parsing the wrong line)
                        StringTokenizer parse = new StringTokenizer(request);
                        method = parse.nextToken().toUpperCase();
                        requestPath = parse.nextToken().toLowerCase();
                        System.out.println("Method: " + method);
                        System.out.println("requestPath: " + requestPath);
                        if (method.equals("GET")) {
                            //sendTestMessage();
                            responseGetRequest("test.txt");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void endThread() throws IOException {
        socket.close();
        exit = true;
    }




    public void sendMessage(String message) throws IOException {
        socOut.writeBytes(message);
    }

//    public void sendTestMessage() {
//        socOut.println("HTTP/1.0 200 OK");
//        socOut.println("Content-Type: text/html");
//        socOut.println("Server: Bot");
//        // this blank line signals the end of the headers
//        socOut.println("");
//        // Send the HTML page
//        socOut.println("<H1>Welcome to the Ultra Mini-WebServer</H1>");
//        socOut.flush();
//    }

    public void responseGetRequest (String fileName) {
        try {
            int NBBYTES = 256;
            FileInputStream inFile = new FileInputStream("C:/Users/Thuy Vu/Desktop/TPServerHTTP/src/server/library/" + fileName);
            byte[] fileInBytes = new byte[NBBYTES];
            inFile.read(fileInBytes);
            socOut.writeBytes("HTTP/1/0 200 OK\r\n");

            if (fileName.endsWith(".jpg"))
                socOut.writeBytes("Content-Type: image/jpeg\r\n");
            if (fileName.endsWith(".gif"))
                socOut.writeBytes("Content-Type: image/gif\r\n");
            if (fileName.endsWith(".txt"))
                socOut.writeBytes("Content-Type: text/txt\r\n");

            socOut.writeBytes("Content-Length: " + NBBYTES + "\r\n");
            socOut.writeBytes("\r\n");

            InputStream is = socket.getInputStream();
            int bytesRead = is.read(fileInBytes, 0, fileInBytes.length);
            socOut.write(fileInBytes, 0, bytesRead);
            socket.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
