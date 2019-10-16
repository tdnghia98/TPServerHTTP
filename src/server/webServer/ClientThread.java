package server.webServer;

import java.io.*;
import java.net.Socket;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static Logger LOGGER = Logger.getLogger(ClientThread.class.getName());

    enum RESPONSE_CODE {
        OK("200 OK","200.html"),
        Created("201 Created", "201.html"),
        NoContent("204 No Content", "204.html"),
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

        // Logging
        LOGGER.setLevel(Level.INFO);
    }

    // Thread
    public void run() {
        while (!exit) {
            try {
                String request = socIn.readLine();
                LOGGER.finest("Client Thread received request: " + request);
                if (request == null) {
                    endThread();
                    LOGGER.warning("null message received. Thread is killed");
                }
                if (request != null) {
                    if (method == null) {
                        // Get the method if method is not defined. This is only executed at the creation of the thread
                        // (avoid parsing the wrong line)
                        StringTokenizer parse = new StringTokenizer(request);
                        method = parse.nextToken().toUpperCase();
                        requestPath = parse.nextToken().toLowerCase();
                        LOGGER.info("Method: " + method);
                        LOGGER.info("requestPath: " + requestPath);
                    }

                    if (request.startsWith("Content-Length")) {
                        contentLength = Integer.parseInt(request.substring("Content-Length: ".length()));
                        LOGGER.info("Body detected. Content Length = " + contentLength);
                    }

                    if (request.equals("")) {
                        LOGGER.info("End of header");
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
                            LOGGER.fine("Body parsed: " + body);
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
        LOGGER.warning("Thread ended");
        socket.close();
        exit = true;
    }

    // Message sender
    public void sendMessage(String message) throws IOException {
        socOut.writeBytes(message);
        socOut.writeBytes("\r\n");
    }

    public void sendMessage() throws IOException {
        socOut.writeBytes("\r\n");
    }

    // File Handler
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

    private void createNewFile(String fileName, String fileContent) throws IOException {
        LOGGER.info("Creating new file");
        Files.write(Paths.get(WEBROOT, fileName), fileContent.getBytes());
        File file = new File(WEBROOT, fileName);
        if (file.exists()) {
            LOGGER.info("File created at " + Paths.get(WEBROOT, fileName));
        } else {
            LOGGER.info("File not created");
        }
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

    // Response
    private void respondToRequest() throws IOException {
        switch (method) {
            case "GET":
                responseGetRequest();
                break;
            case "POST":
                responsePostRequest();
                break;
            case "PUT":
                responsePutRequest();
                break;
            case "HEAD":
                responseHeadRequest();
                break;
            case "DELETE":
                responseDeleteRequest();
                break;
            default:
                LOGGER.warning("Method" + method + "not recognized.");
                sendMessage("HTTP/1.1 " + RESPONSE_CODE.InternalServerError.getMes());
                sendHtmlByResponseCode(RESPONSE_CODE.InternalServerError);
                break;

        }
    }

        public void sendHtmlByResponseCode(RESPONSE_CODE res) throws IOException {
            File file = new File(WEBROOT, res.getPath());
            int fileLength = (int) file.length();
            String content = "text/html";
            byte[] fileInByte = readFileInByte(file, fileLength);

            sendMessage("HTTP/1.1 " + res.getMes());
            sendMessage("Content-type: " + content);
            sendMessage("Content-length: " + fileLength);
            sendMessage(); // blank line between headers and content, very important !
            socOut.flush(); // flush character output stream buffer

            socOut.write(fileInByte, 0, fileLength);
        }

    public void responsePostRequest() throws IOException {
        LOGGER.info("POST Request");

        if (contentLength != null && requestBody != null) {
            try {
                File file = new File(WEBROOT, requestPath);
                if (file.exists()) {
                    LOGGER.warning("File exists at " + WEBROOT + requestPath);
                    sendHtmlByResponseCode(RESPONSE_CODE.BadRequest);
                } else {
                    createNewFile(requestPath, requestBody);
                    sendHtmlByResponseCode(RESPONSE_CODE.Created);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                errorCodeHandler(ex);
            }
        }
    }

    private void responsePutRequest() throws IOException {
        LOGGER.info("PUT Request");
        if (contentLength != null && requestBody != null) {
            try {
                File file = new File(WEBROOT, requestPath);
                if (file.exists()) {
                    LOGGER.info("File exists at " + requestPath);
                    BufferedWriter fileWriter = new BufferedWriter(new FileWriter(WEBROOT + requestPath));
                    fileWriter.write(requestBody);
                    fileWriter.close();
                    sendHtmlByResponseCode(RESPONSE_CODE.OK);
                } else {
                    createNewFile(requestPath, requestBody);
                    sendHtmlByResponseCode(RESPONSE_CODE.Created);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                errorCodeHandler(ex);
            }

            sendMessage();  // blank line between headers and content, very important
        }
    }

    public void responseDeleteRequest() throws IOException {
        LOGGER.info("DELETE Request");
        try {
            if (requestPath.equals("/")) {
                // Trying to delete index.html file
                // TODO: Review with ErrorHandler
                sendHtmlByResponseCode(RESPONSE_CODE.Forbidden);
            } else {
                try {
                    if (Files.deleteIfExists(Paths.get(WEBROOT, requestPath))) {
                        sendHtmlByResponseCode(RESPONSE_CODE.OK);
                    } else {
                        sendHtmlByResponseCode(RESPONSE_CODE.NotFound);
                    }

                } catch (NoSuchFileException e) {
                    LOGGER.info("No such file/directory exists");
                    // If file does not exist
                    // TODO: Review with ErrorHandler
                    sendHtmlByResponseCode(RESPONSE_CODE.NotFound);
                } catch (DirectoryNotEmptyException e) {
                    LOGGER.warning("Directory is not empty.");
                    sendHtmlByResponseCode(RESPONSE_CODE.NoContent);
                } catch (IOException e) {
                    LOGGER.warning("Invalid permissions.");
                    errorCodeHandler(e);
                }
                LOGGER.info("Deletion successful.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            errorCodeHandler(e);
        }
    }

    public void responseGetRequest() throws IOException {
        LOGGER.info("GET Request");
        try {
            File file;
            int fileLength;
            String contentType = getContentType(requestPath);
            if (requestPath.equals("/")) {
                file = new File(WEBROOT, HOMEPAGE);
            } else {
                file = new File(WEBROOT, requestPath);
            }
            fileLength = (int) file.length();
            byte[] fileInBytes = readFileInByte(file, fileLength);

            // Response code
            sendMessage("HTTP/1.1 " + RESPONSE_CODE.OK.getMes());

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

    public void responseHeadRequest() throws IOException {
        try {
            File file;
            int fileLength;
            String contentType = getContentType(requestPath);
            if (requestPath.equals("/")) {
                file = new File(WEBROOT, HOMEPAGE);
            } else {
                file = new File(WEBROOT, requestPath);
            }
            fileLength = (int) file.length();

            // Response code
            sendMessage("HTTP/1.1 " + RESPONSE_CODE.OK.getMes());

            // Content type
            sendMessage("Content-Type: " + contentType);
            // Content length
            sendMessage("Content-Length: " + fileLength);
            sendMessage();
        } catch (FileNotFoundException ex) {
            errorCodeHandler(ex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void errorCodeHandler (Exception ex) throws IOException {
        String mes = ex.getMessage();
        if (mes.matches("(.*)Access is denied(.*)")) {
            sendHtmlByResponseCode(RESPONSE_CODE.Forbidden);
            return;
        }
        if (mes.matches("(.*)The system cannot find the file specified(.*)")) {
            sendHtmlByResponseCode(RESPONSE_CODE.NotFound);
            return;
        }
        if(mes.matches("(.*)File exists(.*)")) {
            sendHtmlByResponseCode(RESPONSE_CODE.BadRequest);
            return;
        }
        sendHtmlByResponseCode(RESPONSE_CODE.InternalServerError);
    }
}
