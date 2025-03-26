package com.viikko5;

import org.apache.commons.codec.digest.Crypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.sun.net.httpserver.BasicAuthenticator;



public class UserAuthenticator extends BasicAuthenticator {

    private final MessageDatabase database;
    

    public UserAuthenticator(MessageDatabase database) {
        super("datarecord"); 
        this.database = database;
        
        
    }

    @Override
    public boolean checkCredentials(String username, String password) {

        String storedHashedPassword = getStoredPassword(username);
        if (storedHashedPassword == null) {
            return false;
        }
        return storedHashedPassword.equals(Crypt.crypt(password, storedHashedPassword));
    }       

    private String getStoredPassword(String username) {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = database.getConnection();
            String query = "SELECT password FROM users WHERE username = ?;";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            rs = stmt.executeQuery();
                
            if (rs.next()) {
                    return rs.getString("password");  
                }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized boolean addUser(String username, String password, String email, String nickname) {
        try {
            
            return database.registerUser(username, password, email, nickname);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized String getUserNickname(String username) {
        
        try {
            return getStoredNickname(username);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getStoredNickname(String username) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = database.getConnection();
            String query = "SELECT userNickname FROM users WHERE username = ?;";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("userNickname");
            }
        } finally {
            closeResources(stmt, rs);
        }
        return null;
    }

    private void closeResources(PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


