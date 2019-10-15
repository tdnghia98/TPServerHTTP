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

    private final String WEBROOT = "./src/server/library";
    private final String HOMEPAGE = "index.html";
    enum RESPONSE_CODE {
        OK("200 OK",""),
        Created("201 Created", ""),
        NoContent("204 No Content", ""),
        NotModified("304 Not Modified", ""),
        BadRequest("400 Bad Request", "400.html"),
        Forbidden("403 Forbidden", "403.html"),
        Unauthorized("401 Unauthorized", "401"),
        NotFound("404 Not Found","404.html"),
        InternalServerError("500 Internal Sever Error", "500.html");

        private String desc;
        private String path;
        RESPONSE_CODE(String desc, String path) {
            this.desc = desc;
            this.path = path;
        }

        public String getMes() {
            return desc;
        }

        public String getPath() {return path;}
    }
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
                responseHeadRequest(requestPath);
                break;
            case "DELETE":
                break;
            default:
                System.err.println("Method" + method + "not recognized.");
                sendMessage("HTTP/1.1 " + RESPONSE_CODE.InternalServerError.getMes());
                sendErrorMes(RESPONSE_CODE.InternalServerError);
                break;
        }
    }

    public void responsePostRequest() throws IOException {
        sendMessage("HTTP/1.1 " + RESPONSE_CODE.OK.toString());
        sendMessage("Content-Type: text/html");
        sendMessage("Server: Bot");
        // End of the headers
        sendMessage();
        // HTML Page
        sendMessage("<H1>This is a post Response</H1>");
        sendMessage("<p>The message body that I received: " + requestBody + "</p>");
    }

    public void sendMessage(String message) throws IOException {
        socOut.writeBytes(message);
        socOut.writeBytes("\r\n");
    }

    public void sendMessage() throws IOException {
        socOut.writeBytes("\r\n");
    }

    private byte[] readFileInByte(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileInByte = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileInByte);
        } finally {
            if (fileIn != null) {
                fileIn.close();
            }
        }
        return fileInByte;
    }

    private String getContentType(String requestContent) {
        if (requestContent.endsWith(".jpg"))
            return "image/jpeg";
        if (requestContent.endsWith(".gif"))
            return "image/gif";
        if (requestContent.endsWith(".txt") || requestContent.endsWith(".html"))
            return "text/html";
        return null;
    }

    public void sendErrorMes(RESPONSE_CODE res) throws IOException {
        File file = new File(WEBROOT, res.getPath());
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileInByte = readFileInByte(file, fileLength);

        sendMessage("HTTP/1.1");
        sendMessage("Content-type: " + content);
        sendMessage("Content-length: " + fileLength);
        sendMessage(); // blank line between headers and content, very important !
        socOut.flush(); // flush character output stream buffer

        socOut.write(fileInByte, 0, fileLength);
    }


    public void responseGetRequest(String requestContent) throws IOException {
        try {
            File file;
            int fileLength;
            String contentType = getContentType(requestContent);
            if (requestContent.equals("/")) {
                file = new File(WEBROOT, HOMEPAGE);
            } else {
                file = new File(WEBROOT, requestContent);
            }
            fileLength = (int) file.length();
            byte[] fileInBytes = readFileInByte(file, fileLength);

            // Response code
            sendMessage("HTTP/1.1 " + RESPONSE_CODE.OK.toString());

            // Content type
            sendMessage("Content-Type: " + contentType);
            // Content length
            sendMessage("Content-Length: " + fileLength);
            sendMessage();  // blank line between headers and content, very important

            // Body
            socOut.write(fileInBytes, 0, fileInBytes.length);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            errorCodeHandler(ex);
        }
    }

    public void responseHeadRequest(String requestContent) throws IOException {

        try {
            File file;
            int fileLength;
            String contentType = getContentType(requestContent);
            if (requestContent.equals("/")) {
                file = new File(WEBROOT, HOMEPAGE);
            } else {
                file = new File(WEBROOT, requestContent);
            }
            fileLength = (int) file.length();

            // Response code
            sendMessage("HTTP/1.1 " + RESPONSE_CODE.OK.toString());

            // Content type
            sendMessage("Content-Type: " + contentType);
            // Content length
            sendMessage("Content-Length: " + fileLength);
            sendMessage();
        } catch (FileNotFoundException ex) {
            errorCodeHandler(ex);
        }
    }

    private void errorCodeHandler (Exception ex) throws IOException {
        String mes = ex.getMessage();
        if (mes.matches("(.*)Access is denied(.*)")) {
            sendMessage("HTTP/1.1 " + RESPONSE_CODE.Forbidden.getMes());
            sendErrorMes(RESPONSE_CODE.Forbidden);
            return;
        }
        if (mes.matches("(.*)The system cannot find the file specified(.*)")) {
            sendMessage("HTTP/1.1 " + RESPONSE_CODE.NotFound.getMes());
            sendErrorMes(RESPONSE_CODE.NotFound);
            return;
        }
        sendMessage("HTTP/1.1 " + RESPONSE_CODE.InternalServerError.getMes());
        sendErrorMes(RESPONSE_CODE.InternalServerError);
    }
}
