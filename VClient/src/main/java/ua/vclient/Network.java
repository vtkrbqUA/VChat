package ua.vclient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {
    private static final Logger log = LogManager.getLogger(Network.class);
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Callback onAuthOkCallback;
    private Callback onAuthFailedCallback;
    private Callback onMessageRecievedCallback;
    private Callback onRegOkCallback;
    private Callback onRegFailedCallback;
    private Callback onDisconnectCallback;
    private Callback onConnectCallback;

    public void setOnRegOkCallback(Callback onRegOkCallback) {
        this.onRegOkCallback = onRegOkCallback;
    }

    public void setOnRegFailedCallback(Callback onRegFailedCallback) {
        this.onRegFailedCallback = onRegFailedCallback;
    }

    public void setOnAuthOkCallback(Callback onAuthOkCallback) {
        this.onAuthOkCallback = onAuthOkCallback;
    }

    public void setOnAuthFailedCallback(Callback onAuthFailedCallback) {
        this.onAuthFailedCallback = onAuthFailedCallback;
    }

    public void setOnMessageRecievedCallback(Callback onMessageRecievedCallback) {
        this.onMessageRecievedCallback = onMessageRecievedCallback;
    }

    public void setOnDisconnectCallback(Callback onDisconnectCallback) {
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public void setOnConnectCallback(Callback onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void connect(int port) throws IOException {
        socket = new Socket("localhost", port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        if (onConnectCallback != null) onConnectCallback.callback();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/register_ok")) {
                        if (onRegOkCallback != null) onRegOkCallback.callback(msg);
                        continue;
                    }
                    if (msg.startsWith("/register_failed")) {
                        String cause = msg.split("\\s", 2)[1];
                        if (onRegFailedCallback != null) onRegFailedCallback.callback(cause);
                        continue;
                    }
                    if (msg.startsWith("/login_ok")) {
                        if (onAuthOkCallback != null) onAuthOkCallback.callback(msg);
                        break;
                    }
                    if (msg.startsWith("/login_failed")) {
                        String cause = msg.split("\\s", 2)[1];
                        if (onAuthFailedCallback != null) onAuthFailedCallback.callback(cause);
                    }
                }
                while (true) {
                    String msg = in.readUTF();
                    if (onMessageRecievedCallback != null) onMessageRecievedCallback.callback(msg);
                }
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            } finally {
                disconnect();
            }
        });
        t.start();
    }

    public void tryToLogin(String login, String password) throws IOException {
        sendMessage("/login " + login + " " + password);
    }

    public void tryToRegister(String login, String password, String nickname) throws IOException {
        sendMessage("/register " + login + " " + password + " " + nickname);
    }

    public void sendMessage(String message) throws IOException {
        if (out != null) out.writeUTF(message);
    }

    public void disconnect() {
        if (onDisconnectCallback != null) onDisconnectCallback.callback();
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        }
    }
}
