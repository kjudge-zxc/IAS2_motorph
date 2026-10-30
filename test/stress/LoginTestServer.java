package stress;

import DAOs.DatabaseConnection;
import DAOs.UserAuthenticationDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * TEST-ONLY adapter so k6 can drive the real login code.
 * MotorPH is a desktop app with no HTTP endpoint, so this exposes
 * UserAuthenticationDAO.authenticateUser() on localhost only.
 * It is not part of the application and is never deployed.
 *
 *   POST /login          (form: email, password)  -> {"authenticated":true|false}
 *   GET  /audit-count?prefix=...                  -> {"recorded":N}
 */

public class LoginTestServer {

    // Same email check the Login screen applies before calling the DAO
    static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.setExecutor(Executors.newCachedThreadPool()); // one thread per concurrent request

        server.createContext("/login", ex -> {
            Map<String, String> form = parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String email = form.getOrDefault("email", "").trim();
            String password = form.getOrDefault("password", "");
            if (email.isEmpty() || password.isEmpty() || !email.matches(EMAIL_REGEX)) {
                send(ex, 400, "{\"error\":\"rejected by login screen validation\"}");
                return;
            }
            boolean ok = new UserAuthenticationDAO().authenticateUser(email, password) != null;
            send(ex, ok ? 200 : 401, "{\"authenticated\":" + ok + "}");
        });

        server.createContext("/audit-count", ex -> {
            String prefix = parse(ex.getRequestURI().getRawQuery()).getOrDefault("prefix", "");
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM audit_log WHERE email LIKE ?")) {
                ps.setString(1, prefix + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    send(ex, 200, "{\"recorded\":" + rs.getInt(1) + "}");
                }
            } catch (Exception e) {
                send(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            }
        });

        server.start();
        System.out.println("Login test adapter running on http://127.0.0.1:8080 (Ctrl+C to stop)");
    }

    static Map<String, String> parse(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String pair : s.split("&")) {
            String[] kv = pair.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                  kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    static void send(HttpExchange ex, int code, String body) throws java.io.IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}