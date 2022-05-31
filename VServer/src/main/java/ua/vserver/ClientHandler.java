package ua.vserver;

import javafx.scene.control.Alert;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;
import java.util.Optional;

public class ClientHandler {
    private final String EXIT_REQUEST = "/exit";
    private final String PRIVATE_MESSAGE_REQUEST = "/w";
    private final String IDENTITY_REQUEST = "/who_am_i";
    private final String CHANGE_NICKNAME_REQUEST = "/change_nickname";
    private final String CLEAR_HISTORY_REQUEST = "/clear_history";
    private String username;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private static final Logger log = LogManager.getLogger(ClientHandler.class);

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (executeRegistration(msg)) continue;
                    if (executeAuthenticationMessage(msg)) break;
                }
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/")) processCommand(msg);
                    else server.broadcastMessage(username + ": " + msg + "\n");
                }
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            } finally {
                disconnect();

            }
        }).start();
    }

    private boolean executeRegistration(String msg) {
        if (msg.startsWith("/register")) {
            String[] tokens = msg.split("\\s");
            String login = tokens[1];
            String password = tokens[2];
            String nickname = tokens[3];
            if (server.isLoginBusy(login)) {
                sendMessage("/register_failed Current login is already in use");
                return false;
            }
            if (server.isNickBusy(nickname)) {
                sendMessage("/register_failed Current nickname is already in use");
                return false;
            }
            server.getAuthenticationProvider().addUserToDB(login, password, nickname);
            sendMessage("/register_ok");
            return true;
        }
        return false;
    }

    private boolean executeAuthenticationMessage(String msg) {
        if (msg.startsWith("/login")) {
            String[] tokens = msg.split("\\s");
            if (tokens.length != 3) {
                sendMessage("/login_failed Type in your login and password \n");
                return false;
            }
            String login = tokens[1];
            String password = tokens[2];
            Optional<String> userNickname = server.getAuthenticationProvider().getNicknameByLoginAndPassword(login, password);
            if (userNickname.isEmpty()) {
                sendMessage("/login_failed Login/password is incorrect \n");
                return false;
            }
            if (server.isNickBusy(login)) {
                sendMessage("/login_failed Current nickname is already in use \n");
                return false;
            }
            username = userNickname.get();
            sendMessage("/login_ok " + username + " " + login);
            server.subscribe(this);
            return true;
        }
        return false;
    }

    private void processCommand(String command) {
        switch (command.toLowerCase(Locale.ROOT).split("\\s")[0]) {
            case EXIT_REQUEST:
                disconnect();
                break;
            case PRIVATE_MESSAGE_REQUEST:
                String[] tokens = command.split("\\s", 3);
                if (tokens.length != 3) {
                    sendMessage("Server: Incorrect command");
                    return;
                }
                server.sendPrivateMessage(this, tokens[1], tokens[2]);
                break;
            case IDENTITY_REQUEST:
                sendMessage("Your name is: " + username + "\n");
                break;
            case CHANGE_NICKNAME_REQUEST:
                String[] token = command.split("\\s");
                String newNickname = token[1];
                if (token.length != 2) {
                    sendMessage("Command is incorrect \n");
                    return;
                }
                if (server.isNicknameInDB(newNickname)) {
                    sendMessage("Server: Nickname is already in use \n");
                    return;
                }
                server.getAuthenticationProvider().changeNickname(username, newNickname);
                username = newNickname;
                sendMessage("Server: You changed your nickname to: " + newNickname + "\n");
                server.broadcastClientList();
                break;
            case CLEAR_HISTORY_REQUEST:
                sendMessage("/clear_history");
                break;
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            disconnect();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            }
        }
    }

    public void sendAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setTitle("VChat");
        alert.showAndWait();
    }
}
