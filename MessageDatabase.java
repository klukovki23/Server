package com.viikko4;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.json.JSONArray;
import org.json.JSONObject;

public class MessageDatabase {
    private static MessageDatabase instance;
    private Connection connection;

    private MessageDatabase(String dbPath) throws SQLException {
        boolean dbExists = new File(dbPath).exists();
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        if (!dbExists) {
            initializeDatabase();
        }
    }

    public static synchronized MessageDatabase getInstance(String dbPath) throws SQLException {
        if (instance == null) {
            instance = new MessageDatabase(dbPath);
        }
        return instance;
    }

    private boolean initializeDatabase() throws SQLException {
        if (connection != null) {
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT UNIQUE NOT NULL, "
                    + "password TEXT NOT NULL, "
                    + "email TEXT UNIQUE NOT NULL"
                    + ");";

            String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "recordIdentifier TEXT UNIQUE NOT NULL, "
                    + "recordDescription TEXT NOT NULL, "
                    + "recordPayload TEXT NOT NULL, "
                    + "recordRightAscension TEXT NOT NULL, "
                    + "recordDeclination TEXT NOT NULL, "
                    + "originalPostingTime INTEGER NOT NULL, "
                    + "recordTimeReceived INTEGER DEFAULT (strftime('%s', 'now') * 1000) "
                    + ");";

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createUsersTable);
                stmt.executeUpdate(createMessagesTable);
            }
            return true;
        }
        return false;
    }

    public boolean registerUser(String username, String password, String email) throws SQLException {
        String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?;";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkUserQuery)) {
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false;
            }
        }

        String insertUserQuery = "INSERT INTO users (username, password, email) VALUES (?, ?, ?);";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertUserQuery)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.setString(3, email);
            insertStmt.executeUpdate();
        }
        return true;
    }

    public boolean validateUser(String username, String password) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public void storeMessage(String recordIdentifier, String recordDescription, String recordPayload,
                             String recordRightAscension, String recordDeclination, long originalPostingTime) throws SQLException {
        String query = "INSERT INTO messages (recordIdentifier, recordDescription, recordPayload, " +
                       "recordRightAscension, recordDeclination, originalPostingTime, recordTimeReceived) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, recordIdentifier);
            stmt.setString(2, recordDescription);
            stmt.setString(3, recordPayload);
            stmt.setString(4, recordRightAscension);
            stmt.setString(5, recordDeclination);
            stmt.setLong(6, originalPostingTime);
            stmt.setLong(7, System.currentTimeMillis()); 
            stmt.executeUpdate();
        }
    }

    public JSONArray fetchMessages() throws SQLException {
        String query = "SELECT recordIdentifier, recordDescription, recordPayload, " +
                       "recordRightAscension, recordDeclination, originalPostingTime, recordTimeReceived " +
                       "FROM messages ORDER BY recordTimeReceived DESC;";

        JSONArray messagesArray = new JSONArray();

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                JSONObject message = new JSONObject();
                message.put("recordIdentifier", rs.getString("recordIdentifier"));
                message.put("recordDescription", rs.getString("recordDescription"));
                message.put("recordPayload", rs.getString("recordPayload"));
                message.put("recordRightAscension", rs.getString("recordRightAscension"));
                message.put("recordDeclination", rs.getString("recordDeclination"));

                long originalPostingTimeEpochMillis = rs.getLong("originalPostingTime");
                message.put("originalPostingTime", Instant.ofEpochMilli(originalPostingTimeEpochMillis).toString());

                long recordTimeReceivedEpochMillis = rs.getLong("recordTimeReceived");
                message.put("recordTimeReceived", Instant.ofEpochMilli(recordTimeReceivedEpochMillis).toString());

                messagesArray.put(message);
            }
        }
        return messagesArray;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
            instance = null;
        }
    }
}

