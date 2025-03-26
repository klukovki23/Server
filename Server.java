package com.viikko5;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;



import java.net.InetSocketAddress;

public class Server implements HttpHandler {
   
    private Authenticator authenticator;
    private static MessageDatabase database;

    private static final String DB_PATH = "database.db"; 

    static {
        try {
            database = MessageDatabase.getInstance(DB_PATH);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Server() {}

    private static SSLContext myServerSSLContext(String keystorePath, String keystorePassword) throws Exception {
        char[] passphrase = keystorePassword.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(new File(keystorePath))) {
            ks.load(fis, passphrase);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "POST":
                try {
                    handlePOSTRequest(exchange);
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                    String response = "Internal Server Error: " + e.getMessage();
                    exchange.sendResponseHeaders(500, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
                break;
            case "GET":
                handleGETRequest(exchange);
                break;
            default:
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
        }
    }

    private void handlePOSTRequest(HttpExchange exchange) throws IOException, SQLException {


        String text = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        System.out.println("Received JSON: " + text);
        
        try {
            JSONObject json = new JSONObject(text);
            String recordIdentifier = json.getString("recordIdentifier");
            String recordDescription = json.getString("recordDescription");
            String recordPayload = json.getString("recordPayload");
            String recordRightAscension = json.getString("recordRightAscension");
            String recordDeclination = json.getString("recordDeclination");
            
            String recordOwner = null;

            if (json.has("recordOwner")) {
             recordOwner = json.getString("recordOwner");
        } else {
            
             
             String username = exchange.getPrincipal().getUsername();  
             
             database.getConnection();

             recordOwner = database.getNicknameForUser(username);
             if (recordOwner == null) {
                 recordOwner = "Unknown"; 
             }
         }
            
            long recordTimeReceived = System.currentTimeMillis();
            JSONArray observatory = json.optJSONArray("observatory");
        
            database.storeMessage(recordIdentifier, recordDescription, recordPayload, recordRightAscension, recordDeclination, recordOwner, observatory, recordTimeReceived, exchange.getPrincipal().getUsername());
            if (observatory != null) {
                
                database.storeObservatory(observatory);
            }
            exchange.sendResponseHeaders(200, -1);
        } catch (JSONException e) {
            String response = "Invalid JSON: " + e.getMessage();
            exchange.sendResponseHeaders(400, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    

    private void handleGETRequest(HttpExchange exchange) throws IOException {
        String query = "SELECT recordIdentifier, recordDescription, recordPayload, recordRightAscension, " +
                "recordDeclination, originalPostingTime, recordOwner, observatory FROM messages ORDER BY originalPostingTime DESC;";
        try (Statement stmt = database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (!rs.isBeforeFirst()) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            JSONArray responseMessages = new JSONArray();
            while (rs.next()) {
                JSONObject messageJson = new JSONObject();
                messageJson.put("recordIdentifier", rs.getString("recordIdentifier"));
                messageJson.put("recordDescription", rs.getString("recordDescription"));
                messageJson.put("recordPayload", rs.getString("recordPayload"));
                messageJson.put("recordRightAscension", rs.getString("recordRightAscension"));
                messageJson.put("recordDeclination", rs.getString("recordDeclination"));

                messageJson.put("recordTimeReceived", Instant.ofEpochMilli(rs.getLong("originalPostingTime")).atZone(ZoneOffset.UTC).toString());
                responseMessages.put(messageJson);

                String recordOwner = rs.getString("recordOwner");
                if (recordOwner == null || recordOwner.isEmpty()) {

                    database.getConnection();
                
                    String username = exchange.getPrincipal().getUsername();  
                    recordOwner = database.getNicknameForUser(username);
                    if (recordOwner == null) {
                        recordOwner = "Unknown";
                    }
                }
                messageJson.put("recordOwner", recordOwner);

                String observatory = rs.getString("observatory");
                if (observatory != null) {
                    messageJson.put("observatory", new JSONArray(observatory));  
                }

            }
            byte[] bytes = responseMessages.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        } catch (SQLException e) {
            String response = "Internal server error: " + e.getMessage();
            exchange.sendResponseHeaders(500, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    public static void main(String[] args) throws Exception {
       
        String keystorePath = args[0];
        String keystorePassword = args[1];
        try {
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            UserAuthenticator authenticator = new UserAuthenticator(database);
            SSLContext sslContext = myServerSSLContext(keystorePath, keystorePassword);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });
            HttpContext context = server.createContext("/datarecord", new Server());
            context.setAuthenticator(authenticator);
            server.createContext("/registration", new RegistrationHandler(authenticator));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



