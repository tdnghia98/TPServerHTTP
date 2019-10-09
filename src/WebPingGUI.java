
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class WebPingGUI extends JFrame implements ActionListener {
    private final String server;
    private final int port;
    private Socket socket;
    private BufferedReader socIn;
    private PrintStream socOut;

    // GUI Elements
    private JPanel containerPane;
    private JPanel userInputPane;
    private JPanel serverResponsePane;
    private JLabel serverResponseJLabel;
    private JTextArea serverResponseJTextArea;
    private JLabel requestTypeJLabel;
    private JComboBox<String> requestTypeJComboBox;
    private JLabel requestPathJLabel;
    private JTextField requestPathJTextField;
    private JLabel headerJLabel;
    private JTextField headerJTextField;
    private JLabel bodyJLabel;
    private JTextArea bodyJTextArea;
    private JButton sendBtn;


    public WebPingGUI(String server, int port) throws IOException {
        // Setup UI
        // UI Components
        serverResponseJTextArea = new JTextArea("Hi");
        final String[] requestTypes = {"GET", "HEAD", "POST", "PUT", "DELETE"};
        requestTypeJComboBox = new JComboBox<>(requestTypes);
        requestTypeJLabel = new JLabel("Request Type");
        requestPathJTextField = new JTextField();
        requestPathJLabel = new JLabel("Request Path");
        headerJTextField = new JTextField();
        headerJLabel = new JLabel("Header");
        bodyJTextArea = new JTextArea();
        bodyJLabel = new JLabel("Body");
        sendBtn = new JButton("Send Request");
        sendBtn.addActionListener(this);
        containerPane = new JPanel();
        userInputPane = new JPanel();
        serverResponsePane = new JPanel();

        containerPane.setLayout(new GridLayout(1,2));
        containerPane.add(userInputPane);
        containerPane.add(serverResponsePane);

        userInputPane.setLayout(new GridLayout(5,1));
        userInputPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), "User Inputs"));

        userInputPane.add(requestTypeJLabel);
        userInputPane.add(requestTypeJComboBox);
        userInputPane.add(requestPathJLabel);
        userInputPane.add(requestPathJTextField);
        userInputPane.add(headerJLabel);
        userInputPane.add(headerJTextField);
        userInputPane.add(bodyJLabel);
        userInputPane.add(bodyJTextArea);
        userInputPane.add(sendBtn);

        serverResponsePane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), "Server Response"));
        serverResponsePane.add(serverResponseJTextArea);

        add(containerPane);

        setSize(900, 500);
//        pack();
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Setup socket
        this.server = server;
        this.port = port;
        socket = new Socket(server, port);
        socIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socOut = new PrintStream(socket.getOutputStream());
        serverResponseJTextArea.setText("[DEBUG] Connected. Connection : " + socket.getInetAddress() + ":" + socket.getPort());
        new ListenFromServer().start();
    }


    public static void main(String[] args) throws IOException {
        WebPingGUI webPingGUI = new WebPingGUI("localhost", 3000);

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

    private void sendRequest() {
        String requestType = Objects.requireNonNull(requestTypeJComboBox.getSelectedItem()).toString();
        String requestPath = requestPathJTextField.getText();
        String header = headerJTextField.getText();
        String body = bodyJTextArea.getText();
        String fullRequest = requestType + "/" + requestPath + "/" + header + "/" + body;
        System.out.println("Sending request to " + socket.getInetAddress() + ":" + socket.getPort() + "...");
        socOut.println(fullRequest);
        System.out.println("Request Sent! [REQUEST] " + fullRequest + " [REQUEST]");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendBtn) {
            sendRequest();
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