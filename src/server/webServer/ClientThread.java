package server.webServer;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientThread extends Thread {
    private Socket socket;
    private BufferedReader socIn;
    private DataOutputStream socOut;
    private boolean exit;
    private String requestPath;
    private String method;
    private Integer contentLength;
    private String requestBody;

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
                    if (method == null) {
                        // Get the method if method is not defined. This is only executed at the creation of the thread
                        // (avoid parsing the wrong line)
                        StringTokenizer parse = new StringTokenizer(request);
                        method = parse.nextToken().toUpperCase();
                        requestPath = parse.nextToken().toLowerCase();
                        System.out.println("Method: " + method);
                        System.out.println("requestPath: " + requestPath);
                    }

                    if (request.startsWith("Content-Length")) {
                        contentLength = Integer.parseInt(request.substring("Content-Length: ".length()));
                        System.out.println("Body detected. Content Length = " + contentLength);
                    }

                    if (request.equals("")) {
                        System.out.println("End of header");
                        if (contentLength == null) {
                            respondToRequest();
                        } else {
                            // If the content-length is not null -> Fetch the body
                            StringBuilder body = new StringBuilder();
                            int b = 0;
                            for (int i = 0; i < contentLength; i++) {
                                b = socIn.read();
                                body.append((char) b);
                            }
                            requestBody = body.toString();
                            System.out.println("Body parsed: " + body);
                            respondToRequest();
                        }
                        endThread();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void endThread() throws IOException {
        System.err.println("Thread ended");
        socket.close();
        exit = true;
    }

    private void respondToRequest() throws IOException {
        switch (method) {
            case "GET":
                responseGetRequest(requestPath);
                break;
            case "POST":
                responsePostRequest();
                break;
            case "PUT":
                break;
            case "HEAD":
                break;
            case "DELETE":
                break;
            default:
                System.err.println("Method" + method + "not recognized.");
                break;
        }
    }

    public void responsePostRequest() throws IOException {
        sendMessage("HTTP/1.0 200 OK");
        sendMessage("Content-Type: text/html");
        sendMessage("Server: Bot");
        // End of the headers
        sendMessage("");
        // HTML Page
        sendMessage("<H1>This is a post Response</H1>");
        sendMessage("<p>The message body that I received: " + requestBody + "</p>");
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

    public void responseGetRequest(String requestContent) {
        try {
            File file;
            try {
                if (requestContent.equals("/")) {
                    file = new File ("./src/server/library/index.html");
                } else {
                    file = new File ("./src/server/library" + requestContent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            FileInputStream inFile = new FileInputStream(file);
            long length = file.length();
            byte[] fileInBytes = new byte[(int) length];
            inFile.read(fileInBytes);

            // Response code
            socOut.writeBytes("HTTP/1/0 200 OK\r\n");

            // Content type
            if (requestContent.endsWith(".jpg"))
                socOut.writeBytes("Content-Type: image/jpeg\r\n");
            if (requestContent.endsWith(".gif"))
                socOut.writeBytes("Content-Type: image/gif\r\n");
            if (requestContent.endsWith(".txt"))
                socOut.writeBytes("Content-Type: text/html\r\n");

            // Content length
            socOut.writeBytes("Content-Length: " + length + "\r\n");
            socOut.writeBytes("\r\n");

            // Body
            socOut.write(fileInBytes, 0, fileInBytes.length);
            socket.close();
            System.out.println("[DEBUG] Socket closed");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
