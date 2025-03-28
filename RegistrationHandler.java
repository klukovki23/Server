package com.viikko5;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.OutputStream;
import org.json.JSONObject;



public class RegistrationHandler implements HttpHandler {
    private final UserAuthenticator authenticator;
    

    public RegistrationHandler(UserAuthenticator authenticator) {
        this.authenticator = authenticator; 
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            handlePostRequest(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }
    
    private void handlePostRequest(HttpExchange exchange) throws IOException {
        
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
       
        if (contentType == null || !contentType.trim().equalsIgnoreCase("application/json")) {
            sendResponse(exchange, 400, "invalid content type.");
            return;
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        
        String requestBody = requestBodyBuilder.toString();

        if (requestBody.trim().isEmpty()) {
            sendResponse(exchange, 400, "Request body is empty");
            return;
        }
        
       

        JSONObject json;
        try {
            json = new JSONObject(requestBody);
        } catch (Exception e) {
            sendResponse(exchange, 400, "Invalid JSON format");
            return;
        }

        if (!json.has("username") || !json.has("password") || !json.has("email") || !json.has("userNickname")) {
            sendResponse(exchange, 400, "Missing required fields: username, password, email, nickname");
            return;
        }

        
        

        String username = json.getString("username").trim();
        String password = json.getString("password").trim();
        String email = json.getString("email").trim();
        String nickname = json.getString("userNickname").trim();

        
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()|| nickname.isEmpty()) {
            sendResponse(exchange, 400, "Fields cannot be empty");
            return;
        }

           
    

        boolean success = authenticator.addUser(username, password, email, nickname);
        if (!success) {
            sendResponse(exchange, 409, "Username already exists");
            return;
        }

        sendResponse(exchange, 201, "User registered successfully");

       }

        
        
        private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            
            exchange.sendResponseHeaders(statusCode, message.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(message.getBytes());
            }
        }
    
}
