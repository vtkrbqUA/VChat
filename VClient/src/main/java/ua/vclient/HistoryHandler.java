package ua.vclient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class HistoryHandler {
    private OutputStream out;
    private String login;
    private static final Logger log = LogManager.getLogger(HistoryHandler.class);

    public void init(String login) {
        try {
            this.login = login;
            this.out = new BufferedOutputStream(new FileOutputStream(getFilename(), true));
        } catch (FileNotFoundException e) {
            log.throwing(Level.ERROR, e);
            throw new RuntimeException("There is some problem with history work");
        }
    }

    public void addStringToHistoryFile(String message) {
        try {
            out.write(message.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            throw new RuntimeException("There is some problem with history work");
        }
    }


    public String getChatHistory() {
        StringBuilder string = new StringBuilder();
        try (InputStream in = new BufferedInputStream(new FileInputStream(getFilename()))) {
            int x;
            while ((x = in.read()) != -1) {
                string.append((char) x);
            }
            return string.toString();
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            throw new RuntimeException("There is some problem with history work");
        }
    }

    public String getFilename() {
        return "../history/history_" + login + ".txt";
    }

    public void close() {
        login = null;
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            }
        }
    }

    public boolean clearHistory() {
        try {
            OutputStream tempOut = new BufferedOutputStream(new FileOutputStream(getFilename()));
            tempOut.write("".getBytes(StandardCharsets.UTF_8));
            tempOut.close();
            return true;
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            throw new RuntimeException("There is some problem with history work");
        }
    }

}
