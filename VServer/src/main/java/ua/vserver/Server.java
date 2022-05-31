package ua.vserver;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;
    private static final Logger log = LogManager.getLogger(Server.class);

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.authenticationProvider = new DBAuthenticationProvider();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server is running at port:  {}", port);
            while (true) {
                log.info("Waiting for client...");
                Socket socket = serverSocket.accept();
                log.info("Client connected");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        } finally {
            this.authenticationProvider.shutdown();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized boolean isNicknameInDB(String nickname) {
        return this.authenticationProvider.isNicknameBusy(nickname);
    }

    public synchronized boolean isNickBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isLoginBusy(String login) {
        return this.authenticationProvider.isLoginBusy(login);
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String recieverUsername, String msg) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(recieverUsername)) {
                c.sendMessage("(w) " + sender.getUsername() + ": " + msg + "\n");
                sender.sendMessage("(w) -> " + c.getUsername() + ": " + msg + "\n");
                return;
            }
        }
        sender.sendMessage("Can't send wisp to " + recieverUsername + "." + "\n");
    }

    public synchronized void broadcastClientList() {
        StringBuilder stringBuilder = new StringBuilder("/client_list ");
        for (ClientHandler c : clients) {
            stringBuilder.append(c.getUsername()).append(" ");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        String clientsList = stringBuilder.toString();
        for (ClientHandler c : clients) {
            c.sendMessage(clientsList);
        }
    }
}
