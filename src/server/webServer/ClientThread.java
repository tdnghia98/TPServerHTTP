package server.webServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientThread extends Thread {
    private Socket socket;
    private BufferedReader socIn;
    private PrintStream socOut;
    private boolean exit;
    private String requestPath;
    private String method;
    private Integer contentLength;
    private String requestBody;

    public ClientThread(Socket s) throws IOException {
        this.socket = s;
        exit = false;
        socIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socOut = new PrintStream(socket.getOutputStream());
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
        socket.close();
        exit = true;
    }

    private void respondToRequest() {
        switch (method) {
            case "GET":
                sendTestMessage();
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

    public void responsePostRequest() {
        sendMessage("HTTP/1.0 200 OK");
        sendMessage("Content-Type: text/html");
        sendMessage("Server: Bot");
        // End of the headers
        sendMessage("");
        // HTML Page
        sendMessage("<H1>This is a post Response</H1>");
        sendMessage("<p>The message body that I received: " + requestBody + "</p>");
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
        socOut.println("<H1>Welcome to the Ultra Mini-WebServer</H1>");
        socOut.flush();
    }
}
