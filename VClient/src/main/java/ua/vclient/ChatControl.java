package ua.vclient;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChatControl implements Initializable {
    private final String CLIENT_LIST_CMD = "/client_list";
    private final String CLEAR_HISTORY_CMD = "/clear_history";
    private static final Logger log = LogManager.getLogger(ChatControl.class);

    @FXML
    private Button exitBtn;

    @FXML
    private VBox secondaryLoginPanel, registrationPanel;

    @FXML
    private HBox chatPanel, loginPanel, menuPanel;

    @FXML
    private BorderPane registrationPane;

    @FXML
    private TextArea mainTextArea;

    @FXML
    private TextField msgField, loginField, regField, nicknameField;

    @FXML
    private PasswordField passwordField, regPassField;

    @FXML
    private ListView<String> clientsList;
    private Network network;
    private HistoryHandler history;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUsername(null);
        network = new Network();
        network.setOnRegOkCallback(args -> {
            regField.clear();
            regPassField.clear();
            nicknameField.clear();
            registrationPanel.setVisible(false);
            registrationPanel.setManaged(false);
            secondaryLoginPanel.setVisible(true);
            secondaryLoginPanel.setManaged(true);
        });

        network.setOnRegFailedCallback(args -> {
            String msg = (String) args[0];
            String cause = msg.split("\\s", 2)[1];
            sendAlert(cause);
            regField.clear();
            regPassField.clear();
            nicknameField.clear();
        });

        network.setOnAuthOkCallback(args -> {
            String msg = (String) args[0];
            mainTextArea.clear();
            mainTextArea.appendText(history.getChatHistory());
            setUsername(msg.split("\\s")[1]);
        });

        network.setOnAuthFailedCallback(args -> {
            String cause = (String) args[0];
            sendAlert(cause);
        });

        network.setOnMessageRecievedCallback(args -> {
            String msg = (String) args[0];
            if (msg.startsWith("/")) {
                processCommands(msg);
            } else {
                mainTextArea.appendText(msg);
                history.addStringToHistoryFile(msg);
            }
        });
        network.setOnDisconnectCallback(args -> {
            if (history != null) history.close();
            setUsername(null);
        });
        history = new HistoryHandler();
    }

    public void setUsername(String username) {
        boolean usernameIsNull = username == null;
        registrationPane.setManaged(usernameIsNull);
        registrationPane.setVisible(usernameIsNull);
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        mainTextArea.setVisible(!usernameIsNull);
        mainTextArea.setVisible(!usernameIsNull);
        chatPanel.setVisible(!usernameIsNull);
        chatPanel.setManaged(!usernameIsNull);
        menuPanel.setVisible(!usernameIsNull);
        menuPanel.setManaged(!usernameIsNull);
        clientsList.setVisible(!usernameIsNull);
        clientsList.setManaged(!usernameIsNull);
    }

    public void clickSendButton() {
        try {
            network.sendMessage(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            sendAlert("Can't send a message");
        }
    }

    public void registration() {
        if (regField.getText().isEmpty() || regPassField.getText().isEmpty() || nicknameField.getText().isEmpty()) {
            sendAlert("All fields must be filled");
            return;
        }
        if (!network.isConnected()) {
            try {
                network.connect(8189);
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
                sendAlert("Can't connect to server with port " + 8189);
                return;
            }
        }
        try {
            network.tryToRegister(regField.getText(), regPassField.getText(), nicknameField.getText());
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        }
    }

    public void login() {
        if (loginField.getText().isEmpty()) {
            sendAlert("Login can't be empty");
            return;
        }
        if (!network.isConnected()) {
            try {
                network.connect(8189);
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
                sendAlert("Can't connect to server with port " + 8189);
                return;
            }
        }
        try {
            network.tryToLogin(loginField.getText(), passwordField.getText());
            history.init(loginField.getText());
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            sendAlert("Can't send user data");
        }
    }

    private void processCommands(String cmd) {
        String[] tokens = cmd.split("\\s");
        switch (tokens[0]) {
            case CLIENT_LIST_CMD:
                Platform.runLater(() -> {
                    clientsList.getItems().clear();
                    for (int i = 1; i < tokens.length; i++) {
                        clientsList.getItems().add(tokens[i]);
                    }
                });
                break;
            case CLEAR_HISTORY_CMD:
                if (history.clearHistory()) {
                    mainTextArea.clear();
                    mainTextArea.appendText("Server: History is cleared. \n");
                }
                break;
        }
    }

    public void sendAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setTitle("VChat");
        alert.showAndWait();
    }

    public boolean sendConfirm(String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("VChat");
        confirm.showAndWait();
        return confirm.getResult().equals(ButtonType.YES);
    }

    public void registrationView() {
        registrationPanel.setVisible(true);
        registrationPanel.setManaged(true);
        secondaryLoginPanel.setVisible(false);
        secondaryLoginPanel.setManaged(false);
    }


    public void changeAcc() {
        network.disconnect();
    }

    public void whoAmI() throws IOException {
        network.sendMessage("/who_am_i");
    }

    public void changeNickname() {
        msgField.clear();
        msgField.appendText("/change_nickname ");
        msgField.requestFocus();
    }

    public void sendWisp() {
        msgField.clear();
        msgField.appendText("/w ");
        msgField.requestFocus();
    }

    public void back() {
        regField.clear();
        regPassField.clear();
        nicknameField.clear();
        registrationPanel.setVisible(false);
        registrationPanel.setManaged(false);
        secondaryLoginPanel.setVisible(true);
        secondaryLoginPanel.setManaged(true);
    }

    public void clearHistory() {
        try {
            if (sendConfirm("Are you sure you want to clear your history?")) {
                network.sendMessage("/clear_history");
            } else return;
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            sendAlert("Can't send a request to clear history");
        }
    }

    public void close() {
        network.disconnect();
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.close();
    }

    public void about() {
        sendAlert("Welcome to the VChat project. Have fun ;)");
    }
}