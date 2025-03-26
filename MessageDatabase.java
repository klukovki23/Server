package com.viikko5;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Base64;
import java.security.SecureRandom;
import org.apache.commons.codec.digest.Crypt;

public class MessageDatabase {

    private static MessageDatabase instance;
    private static Connection connection;
    private String dbPath;
    private SecureRandom secureRandom = new SecureRandom();
    
    private MessageDatabase(String dbPath) throws SQLException {
        this.dbPath = dbPath;
        
        boolean dbExists = new File(dbPath).exists();
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        if (!dbExists) {
            initializeDatabase();
        }
    }

    public static synchronized MessageDatabase getInstance(String dbPath) throws SQLException {
        if (instance == null || MessageDatabase.connection == null || MessageDatabase.connection.isClosed()) {
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
                    + "email TEXT UNIQUE NOT NULL, "
                    + "userNickname TEXT NOT NULL"
                    + ");";

            String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "recordIdentifier TEXT UNIQUE NOT NULL, "
                    + "recordDescription TEXT NOT NULL, "
                    + "recordPayload TEXT NOT NULL, "
                    + "recordRightAscension TEXT NOT NULL, "
                    + "recordDeclination TEXT NOT NULL, "
                    + "recordOwner TEXT, "
                    + "observatory TEXT, "
                    + "originalPostingTime INTEGER NOT NULL, "
                    + "recordTimeReceived INTEGER DEFAULT (strftime('%s', 'now') * 1000) "
                    + ");";

            String createObservatoryTable = "CREATE TABLE IF NOT EXISTS observatory ("
                    + "observatoryName TEXT NOT NULL, "
                    + "latitude REAL NOT NULL, "
                    + "longitude REAL NOT NULL "
                    + ");";

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createUsersTable);
                stmt.executeUpdate(createMessagesTable);
                stmt.executeUpdate(createObservatoryTable);
            }
            return true;
        }
        return false;
    }

    public boolean registerUser(String username, String password, String email, String nickname) throws SQLException {
        Connection connection = getConnection();

        String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?;";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkUserQuery)) {
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false;
            }
        }

        byte[] bytes = new byte[13];
        secureRandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        salt = salt.replace("+", "a");
        String hashedPassword = Crypt.crypt(password, salt);

        String insertUserQuery = "INSERT INTO users (username, password, email, userNickname) VALUES (?, ?, ?, ?);";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertUserQuery)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);
            insertStmt.setString(3, email);
            insertStmt.setString(4, nickname);
            insertStmt.executeUpdate();
        }
        return true;
    }

    public boolean validateUser(String username, String password) throws SQLException {
        String query = "SELECT password FROM users WHERE username = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHashedPassword = rs.getString("password");
                return storedHashedPassword.equals(Crypt.crypt(password, storedHashedPassword));
            }
        }
        return false;
    }

    public String getNicknameForUser(String username) throws SQLException {
        String query = "SELECT userNickname FROM users WHERE username = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("userNickname");
            }
        }
        return null;
    }

    public void storeMessage(String recordIdentifier, String recordDescription, String recordPayload,
                             String recordRightAscension, String recordDeclination, String recordOwner, JSONArray observatory, long originalPostingTime, String username) throws SQLException {

        String resolvedOwner = recordOwner;                        
        

        if (resolvedOwner == null || resolvedOwner.isEmpty()) {
            resolvedOwner = getNicknameForUser(username);  
            
            if (resolvedOwner == null) {
                resolvedOwner = "Unknown";  
            }
        }

        String query = "INSERT INTO messages (recordIdentifier, recordDescription, recordPayload, "
                + "recordRightAscension, recordDeclination, recordOwner, originalPostingTime, recordTimeReceived, observatory) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, recordIdentifier);
            stmt.setString(2, recordDescription);
            stmt.setString(3, recordPayload);
            stmt.setString(4, recordRightAscension);
            stmt.setString(5, recordDeclination);
            stmt.setString(6, resolvedOwner);
            stmt.setLong(7, originalPostingTime);
            stmt.setLong(8, System.currentTimeMillis());
            
        
        
            if (observatory != null && observatory.length() > 0) {
                stmt.setString(9, observatory.toString());
            } else {
                stmt.setNull(9, Types.VARCHAR); 
            }
    
            stmt.executeUpdate();
        }
    }

    public void storeObservatory(JSONArray observatory) throws SQLException {
        String query = "INSERT INTO observatory (observatoryName, latitude, longitude) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(query)) {
            for (int i = 0; i < observatory.length(); i++) {
                JSONObject observatoryItem = observatory.getJSONObject(i);

                String observatoryName = observatoryItem.getString("observatoryName");
                double latitude = observatoryItem.getDouble("latitude");
                double longitude = observatoryItem.getDouble("longitude");

                pstmt.setString(1, observatoryName);
                pstmt.setDouble(2, latitude);
                pstmt.setDouble(3, longitude);

                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public JSONArray fetchMessages() throws SQLException {
        String query = "SELECT recordIdentifier, recordDescription, recordPayload, "
                + "recordRightAscension, recordDeclination, recordOwner, observatory, originalPostingTime, recordTimeReceived "
                + "FROM messages ORDER BY recordTimeReceived DESC;";

        JSONArray messagesArray = new JSONArray();
        Connection connection = getConnection();

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                JSONObject message = new JSONObject();
                message.put("recordIdentifier", rs.getString("recordIdentifier"));
                message.put("recordDescription", rs.getString("recordDescription"));
                message.put("recordPayload", rs.getString("recordPayload"));
                message.put("recordRightAscension", rs.getString("recordRightAscension"));
                message.put("recordDeclination", rs.getString("recordDeclination"));
                message.put("recordOwner", rs.getString("recordOwner"));

                String observatoryData = rs.getString("observatory");
                if (observatoryData != null && !observatoryData.isEmpty()) {
                    try {
                        JSONArray observatoryArray = new JSONArray(observatoryData);
                        message.put("observatory", observatoryArray);
                    } catch (Exception e) {
                        message.put("observatory", new JSONArray());
                    }
                }

                long originalPostingTimeEpochMillis = rs.getLong("originalPostingTime");
                message.put("originalPostingTime", Instant.ofEpochMilli(originalPostingTimeEpochMillis).toString());

                long recordTimeReceivedEpochMillis = rs.getLong("recordTimeReceived");
                message.put("recordTimeReceived", Instant.ofEpochMilli(recordTimeReceivedEpochMillis).toString());

                messagesArray.put(message);
            }
        }
        return messagesArray;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        }
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
           
        }
    }
}

