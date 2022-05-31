package ua.vserver;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DBAuthenticationProvider implements AuthenticationProvider {
    private class User {
        private String login;
        private String password;
        private String nickname;

        public User(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private static final Logger log = LogManager.getLogger(DBAuthenticationProvider.class);
    private List<User> users;
    private static DBSource dbSource = new DBSource();

    public DBAuthenticationProvider() {
        this.users = new ArrayList<>();
        try (ResultSet rs = dbSource.getStatement().executeQuery("select login, password, nickname from users")) {
            while (rs.next()) {
                this.users.add(new User(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
    }


    @Override
    public synchronized Boolean isLoginBusy(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Boolean isNicknameBusy(String nickname) {
        try {
            dbSource.setPreparedStatement("select (nickname) from users where nickname = (?);");
            dbSource.getPreparedStatement().setString(1, nickname);
            ResultSet rs = dbSource.getPreparedStatement().executeQuery();
            if (rs.next()) {
                return true;
            }
            rs.close();
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
        return false;
    }

    @Override
    public synchronized Optional<String> getNicknameByLoginAndPassword(String login, String password) {
        try {
            dbSource.setPreparedStatement("select (nickname) from users where login = (?) and password = (?);");
            dbSource.getPreparedStatement().setString(1, login);
            dbSource.getPreparedStatement().setString(2, password);
            ResultSet rs = dbSource.getPreparedStatement().executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getString(1));
            }
            rs.close();
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void addUserToDB(String login, String password, String nickname) {
        try {
            dbSource.setPreparedStatement("insert into users (login, password, nickname) values (?, ?, ?);");
            dbSource.getPreparedStatement().setString(1, login);
            dbSource.getPreparedStatement().setString(2, password);
            dbSource.getPreparedStatement().setString(3, nickname);
            dbSource.getPreparedStatement().executeUpdate();
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
            throw new RuntimeException("Can't connect to DB");
        }
    }

    @Override
    public synchronized void changeNickname(String oldNickname, String newNickname) {
        try {
            dbSource.setPreparedStatement("update users set nickname = (?) where nickname = (?);");
            dbSource.getPreparedStatement().setString(1, newNickname);
            dbSource.getPreparedStatement().setString(2, oldNickname);
            dbSource.getPreparedStatement().executeUpdate();
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
            throw new RuntimeException("Can't connect to DB");
        }
    }

    @Override
    public void shutdown() {
        dbSource.close();
    }
}
