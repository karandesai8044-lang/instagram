import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal local dev server for a demo enquiry form.
 *
 * The form submits to /login, validates client-side data, and tries to send
 * an email to karandesai8044@gmail.com using SMTP config from environment
 * variables when JavaMail is available.
 *
 * To send real email, set:
 *   SMTP_USERNAME=your-gmail-address@gmail.com
 *   SMTP_PASSWORD=your-16-char-app-password
 *   EMAIL_TO=karandesai8044@gmail.com
 *
 * Without those values, the enquiry is saved to enquiries.txt as a fallback.
 */
public class LoginServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String LOG_FILE = "login_attempts.txt";
    private static final String ENQUIRY_FILE = "enquiries.txt";
    private static final String DEFAULT_TO_EMAIL = "karandesai8044@gmail.com";
    private static final String EMAIL_SUBJECT = "New enquiry from website";

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/login", new LoginHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server running at http://0.0.0.0:" + port);
        System.out.println("Enquiries are treated as form submissions and saved to " + LOG_FILE);
        System.out.println("If SMTP_USERNAME and SMTP_PASSWORD are configured, the enquiry will also be emailed.");
    }

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

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> fields = parseJson(body);
            String fullName = clean(fields.get("fullName"));
            String email = clean(fields.get("email"));
            String phone = clean(fields.get("phone"));
            String message = clean(fields.get("message"));

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("[ENQUIRY] " + timestamp + " -> " + fullName + " | " + email + " | " + phone);

            String validationMessage = validateEnquiry(fullName, email, phone, message);
            String responseJson;
            int statusCode;

            if (validationMessage != null) {
                responseJson = "{\"status\":\"error\",\"message\":\"" + sanitizeJson(validationMessage) + "\"}";
                statusCode = 200;
            } else {
                String emailBody = buildEmailBody(fullName, email, phone, message);
                boolean sent = sendEnquiryEmail(EMAIL_SUBJECT, emailBody);
                if (sent) {
                    appendLog(LOG_FILE, timestamp + " | Full Name: " + fullName + " | Email: " + email + " | Phone: " + phone + " | Message: " + message + System.lineSeparator());
                    responseJson = "{\"status\":\"ok\",\"message\":\"Enquiry sent successfully.\"}";
                    statusCode = 200;
                } else {
                    appendLog(ENQUIRY_FILE, "-----\n" + timestamp + "\n" + emailBody + "\n-----\n");
                    responseJson = "{\"status\":\"error\",\"message\":\"Your enquiry was saved locally. Set SMTP_USERNAME and SMTP_PASSWORD to enable email delivery.\"}";
                    statusCode = 200;
                }
            }

            byte[] respBytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        }

        private String validateEnquiry(String fullName, String email, String phone, String message) {
            if (fullName == null || fullName.length() < 2) {
                return "Full name is required.";
            }
            if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return "Please enter a valid email address.";
            }
            if (phone == null || !phone.matches("^\\+?[0-9\\s-]{10,15}$")) {
                return "Please enter a valid phone number.";
            }
            if (message == null || message.length() < 10) {
                return "Message should be at least 10 characters long.";
            }
            return null;
        }

        private String buildEmailBody(String fullName, String email, String phone, String message) {
            return "Full Name: " + fullName + "\n" +
                    "Email: " + email + "\n" +
                    "Phone: " + phone + "\n" +
                    "Message:\n" + message + "\n";
        }

        /** Try to send mail using JavaMail if the library is available in the classpath. */
        private boolean sendEnquiryEmail(String subject, String body) {
            String smtpHost = System.getenv().getOrDefault("SMTP_HOST", "smtp.gmail.com");
            String smtpPort = System.getenv().getOrDefault("SMTP_PORT", "587");
            String username = System.getenv("SMTP_USERNAME");
            String password = System.getenv("SMTP_PASSWORD");
            String toAddress = System.getenv().getOrDefault("EMAIL_TO", DEFAULT_TO_EMAIL);

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                return false;
            }

            try {
                Object properties = Class.forName("java.util.Properties").getDeclaredConstructor().newInstance();
                Method put = properties.getClass().getMethod("put", Object.class, Object.class);
                put.invoke(properties, "mail.smtp.auth", "true");
                put.invoke(properties, "mail.smtp.starttls.enable", "true");
                put.invoke(properties, "mail.smtp.host", smtpHost);
                put.invoke(properties, "mail.smtp.port", smtpPort);
                put.invoke(properties, "mail.smtp.ssl.trust", smtpHost);

                Class<?> sessionClass;
                try {
                    sessionClass = Class.forName("javax.mail.Session");
                } catch (ClassNotFoundException e) {
                    sessionClass = Class.forName("jakarta.mail.Session");
                }

                Object session = sessionClass.getMethod("getInstance", Class.forName("java.util.Properties")).invoke(null, properties);

                Class<?> mimeMessageClass;
                try {
                    mimeMessageClass = Class.forName("javax.mail.internet.MimeMessage");
                } catch (ClassNotFoundException e) {
                    mimeMessageClass = Class.forName("jakarta.mail.internet.MimeMessage");
                }

                Object message = mimeMessageClass.getConstructor(sessionClass).newInstance(session);
                Class<?> addressClass;
                try {
                    addressClass = Class.forName("javax.mail.internet.InternetAddress");
                } catch (ClassNotFoundException e) {
                    addressClass = Class.forName("jakarta.mail.internet.InternetAddress");
                }

                Object fromAddress = addressClass.getConstructor(String.class).newInstance(username.trim());
                message.getClass().getMethod("setFrom", addressClass).invoke(message, fromAddress);

                Class<?> recipientTypeClass;
                try {
                    recipientTypeClass = Class.forName("javax.mail.Message$RecipientType");
                } catch (ClassNotFoundException e) {
                    recipientTypeClass = Class.forName("jakarta.mail.Message$RecipientType");
                }

                Object recipientType = recipientTypeClass.getField("TO").get(null);
                Object toAddressObj = addressClass.getConstructor(String.class).newInstance(toAddress);
                message.getClass().getMethod("setRecipient", recipientTypeClass, addressClass).invoke(message, recipientType, toAddressObj);
                message.getClass().getMethod("setSubject", String.class).invoke(message, subject);
                message.getClass().getMethod("setText", String.class).invoke(message, body);

                Class<?> transportClass;
                try {
                    transportClass = Class.forName("javax.mail.Transport");
                } catch (ClassNotFoundException e) {
                    transportClass = Class.forName("jakarta.mail.Transport");
                }

                Object transport = sessionClass.getMethod("getTransport", String.class).invoke(session, "smtp");
                transport.getClass().getMethod("connect", String.class, String.class, String.class).invoke(transport, smtpHost, username, password);

                Object addressArray = java.lang.reflect.Array.newInstance(addressClass, 1);
                java.lang.reflect.Array.set(addressArray, 0, toAddressObj);
                transportClass.getMethod("sendMessage", mimeMessageClass, addressArray.getClass()).invoke(transport, message, addressArray);
                transport.getClass().getMethod("close").invoke(transport);
                return true;
            } catch (Exception ex) {
                System.out.println("[EMAIL ERROR] " + ex.getMessage());
                return false;
            }
        }

        private void appendLog(String fileName, String content) {
            try {
                Path filePath = Path.of(fileName).toAbsolutePath().normalize();
                Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.out.println("[FILE ERROR] Could not append to " + fileName + " -> " + e.getMessage());
            }
        }

        private String sanitizeJson(String value) {
            return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        }

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

        private String clean(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
