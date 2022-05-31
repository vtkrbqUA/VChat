package ua.vserver;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class DBSource {
    private Connection connection;
    private Statement statement;
    private PreparedStatement preparedStatement;
    private static final Logger log = LogManager.getLogger(DBSource.class);

    public Statement getStatement() {
        return statement;
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    public void setPreparedStatement(String sql) throws SQLException {
        this.preparedStatement = connection.prepareStatement(sql);
    }

    public DBSource() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:VChat.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            log.throwing(Level.ERROR, e);
            throw new RuntimeException("Can't connect to DB");
        }
    }

    public void close() {
        if (this.statement != null) {
            try {
                this.statement.close();
            } catch (SQLException e) {
                log.throwing(Level.ERROR, e);
            }
        }
        if (this.preparedStatement != null) {
            try {
                this.preparedStatement.close();
            } catch (SQLException e) {
                log.throwing(Level.ERROR, e);
            }
        }
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                log.throwing(Level.ERROR, e);
            }
        }
    }
}
