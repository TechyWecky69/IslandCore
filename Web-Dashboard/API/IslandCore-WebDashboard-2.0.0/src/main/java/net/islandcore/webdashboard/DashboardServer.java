package net.islandcore.webdashboard;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class DashboardServer {
    private final JavaPlugin plugin;
    private final MetricsCollector metrics;
    private final HistoryStore history;
    private final TradeLogStore tradeLogs;
    private HttpServer server;

    private final Map<String, Credential> users = new HashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> loginFailures = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private String applicationKey;
    private String allowedOrigin;

    private record Credential(String username, byte[] salt, byte[] hash) {}
    private record Session(String username, long expiresAt) {}

    public DashboardServer(JavaPlugin plugin, MetricsCollector metrics, HistoryStore history, TradeLogStore tradeLogs) {
        this.plugin = plugin;
        this.metrics = metrics;
        this.history = history;
        this.tradeLogs = tradeLogs;
        loadSecurityConfig();
    }

    private void loadSecurityConfig() {
        applicationKey = plugin.getConfig().getString("api.application-key", "").trim();
        allowedOrigin = plugin.getConfig().getString("api.allowed-origin", "*").trim();

        if (applicationKey.isBlank() || applicationKey.length() < 32) {
            applicationKey = randomToken(32);
            plugin.getConfig().set("api.application-key", applicationKey);
            plugin.saveConfig();
            plugin.getLogger().warning("Dashboard API generated a new application key. Copy it from config.yml into the authorized app configuration.");
        }

        List<Map<?, ?>> list = plugin.getConfig().getMapList("users");
        List<Map<String, Object>> rewritten = new ArrayList<>();
        boolean changed = false;

        for (Map<?, ?> entry : list) {
            Object u = entry.get("username");
            Object hash = entry.get("passwordHash");
            Object salt = entry.get("passwordSalt");
            Object plain = entry.get("password");
            if (u == null) continue;
            String username = u.toString().trim();
            if (username.isBlank()) continue;

            byte[] saltBytes;
            byte[] hashBytes;
            if (hash != null && salt != null) {
                try {
                    saltBytes = Base64.getDecoder().decode(salt.toString());
                    hashBytes = Base64.getDecoder().decode(hash.toString());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Dashboard: invalid password hash for user " + username);
                    continue;
                }
            } else if (plain != null) {
                saltBytes = new byte[16];
                random.nextBytes(saltBytes);
                hashBytes = hashPassword(plain.toString(), saltBytes);
                changed = true;
            } else {
                plugin.getLogger().warning("Dashboard: user " + username + " has no passwordHash/password.");
                continue;
            }

            users.put(username, new Credential(username, saltBytes, hashBytes));

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("username", username);
            out.put("passwordSalt", Base64.getEncoder().encodeToString(saltBytes));
            out.put("passwordHash", Base64.getEncoder().encodeToString(hashBytes));
            rewritten.add(out);
        }

        if (changed) {
            plugin.getConfig().set("users", rewritten);
            plugin.saveConfig();
            plugin.getLogger().info("Dashboard: converted plaintext dashboard passwords to salted PBKDF2 hashes.");
        }
        plugin.getLogger().info("Dashboard API loaded " + users.size() + " user(s).");
    }

    public void start() throws Exception {
        String host = plugin.getConfig().getString("host", "127.0.0.1");
        int port = plugin.getConfig().getInt("port", 8765);
        boolean tls = plugin.getConfig().getBoolean("tls.enabled", true);

        if (tls) {
            HttpsServer https = HttpsServer.create(new InetSocketAddress(host, port), 0);
            https.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(buildSslContext()));
            server = https;
        } else {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
        }

        server.setExecutor(Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "IslandCore-Dashboard-HTTP");
            t.setDaemon(true);
            return t;
        }));
        server.createContext("/", this::handle);
        server.start();
    }

    private SSLContext buildSslContext() throws Exception {
        String path = plugin.getConfig().getString("tls.keystore", "plugins/IslandCoreWebDashboard/dashboard.p12");
        String password = plugin.getConfig().getString("tls.keystore-password", "");
        if (password.isBlank()) throw new IllegalStateException("TLS is enabled but tls.keystore-password is blank.");

        File file = new File(path);
        if (!file.isAbsolute()) file = new File(".", path);
        if (!file.isFile()) {
            throw new IOException("TLS keystore not found: " + file.getAbsolutePath() +
                    ". Create a PKCS12 keystore with keytool, or set tls.enabled=false behind a trusted HTTPS reverse proxy.");
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(file.toPath())) {
            ks.load(in, password.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
        return ctx;
    }

    public void stop() {
        if (server != null) server.stop(0);
        sessions.clear();
    }

    public String getDisplayAddress() {
        String host = plugin.getConfig().getString("host", "127.0.0.1");
        int port = plugin.getConfig().getInt("port", 8765);
        return (plugin.getConfig().getBoolean("tls.enabled", true) ? "https://" : "http://") + host + ":" + port;
    }

    private void handle(HttpExchange ex) throws IOException {
        try {
            securityHeaders(ex);
            cors(ex);
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            String path = ex.getRequestURI().getPath();

            if (path.equals("/api/auth/login") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
                if (!validApplicationKey(ex)) { json(ex, "{\"error\":\"unauthorized application\"}", 401); return; }
                login(ex); return;
            }

            // A tiny public health endpoint lets the separate app show whether the API is reachable.
            if (path.equals("/api/health")) {
                json(ex, "{\"ok\":true,\"service\":\"IslandCore Dashboard API\",\"tls\":" +
                        plugin.getConfig().getBoolean("tls.enabled", true) + "}");
                return;
            }

            if (path.startsWith("/api/")) {
                if (!validApplicationKey(ex) || !validSession(ex)) {
                    json(ex, "{\"error\":\"unauthorized\"}", 401); return;
                }
                if (path.equals("/api/status")) json(ex, JsonUtil.status(metrics.get()));
                else if (path.equals("/api/metrics")) json(ex, JsonUtil.metrics(metrics.get()));
                else if (path.equals("/api/chat")) json(ex, JsonUtil.logs(history.chatSnapshot(history.chatCapacity())));
                else if (path.equals("/api/trades")) json(ex, JsonUtil.tradeLogs(tradeLogs.loadAll()));
                else { json(ex, "{\"error\":\"not found\"}", 404); }
                return;
            }

            // Website code is separate now; the plugin only exposes the API.
            json(ex, "{\"error\":\"This server provides the dashboard API only.\"}", 404);
        } catch (Exception e) {
            plugin.getLogger().warning("Dashboard API request failed: " + e.getMessage());
            try { json(ex, "{\"error\":\"internal server error\"}", 500); } catch (Exception ignored) {}
        } finally {
            ex.close();
        }
    }

    private void login(HttpExchange ex) throws IOException {
        String remote = ex.getRemoteAddress() == null ? "unknown" : ex.getRemoteAddress().getAddress().getHostAddress();
        if (tooManyFailures(remote)) { json(ex, "{\"error\":\"too many login attempts\"}", 429); return; }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = body.trim().startsWith("{") ? parseJsonLogin(body) : parseForm(body);
        String username = form.getOrDefault("username", "").trim();
        String password = form.getOrDefault("password", "");

        Credential c = users.get(username);
        boolean ok = c != null && verifyPassword(password, c.salt(), c.hash());
        if (!ok) {
            loginFailures.computeIfAbsent(remote, k -> new AtomicLong()).incrementAndGet();
            json(ex, "{\"error\":\"invalid credentials\"}", 401);
            return;
        }

        loginFailures.remove(remote);
        String token = randomToken(48);
        sessions.put(token, new Session(username, System.currentTimeMillis() + 8 * 60 * 60 * 1000L));
        json(ex, "{\"ok\":true,\"token\":\"" + esc(token) + "\",\"expiresIn\":28800}");
    }

    private boolean tooManyFailures(String ip) {
        AtomicLong n = loginFailures.get(ip);
        return n != null && n.get() >= 8;
    }

    private boolean validApplicationKey(HttpExchange ex) {
        String supplied = ex.getRequestHeaders().getFirst("X-Dashboard-App-Key");
        return supplied != null && constantTime(supplied, applicationKey);
    }

    private boolean validSession(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return false;
        String token = header.substring(7).trim();
        Session s = sessions.get(token);
        if (s == null) return false;
        if (s.expiresAt() < System.currentTimeMillis()) {
            sessions.remove(token);
            return false;
        }
        return true;
    }

    private void securityHeaders(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Referrer-Policy", "no-referrer");
        h.set("Cache-Control", "no-store");
        h.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    }

    private void cors(HttpExchange ex) {
        if (!allowedOrigin.isBlank()) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
            ex.getResponseHeaders().set("Vary", "Origin");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Dashboard-App-Key");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        }
    }

    private static Map<String,String> parseJsonLogin(String body) {
        Map<String,String> m = new HashMap<>();
        // Login fields are deliberately tiny; this avoids adding a JSON runtime dependency.
        MatcherResult u = jsonStringField(body, "username");
        MatcherResult p = jsonStringField(body, "password");
        if (u.value != null) m.put("username", u.value);
        if (p.value != null) m.put("password", p.value);
        return m;
    }

    private record MatcherResult(String value) {}
    private static MatcherResult jsonStringField(String body, String key) {
        String needle = "\"" + key + "\"";
        int k = body.indexOf(needle);
        if (k < 0) return new MatcherResult(null);
        int colon = body.indexOf(':', k + needle.length());
        if (colon < 0) return new MatcherResult(null);
        int first = body.indexOf('"', colon + 1);
        if (first < 0) return new MatcherResult(null);
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = first + 1; i < body.length(); i++) {
            char c = body.charAt(i);
            if (esc) { out.append(c); esc = false; }
            else if (c == '\\') esc = true;
            else if (c == '"') return new MatcherResult(out.toString());
            else out.append(c);
        }
        return new MatcherResult(null);
    }

    private static Map<String,String> parseForm(String body) {
        Map<String,String> result = new HashMap<>();
        for (String part : body.split("&")) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length == 2 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            result.put(k, v);
        }
        return result;
    }

    private static byte[] hashPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static boolean verifyPassword(String password, byte[] salt, byte[] expected) {
        return constantTime(hashPassword(password, salt), expected);
    }

    private static boolean constantTime(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    private static boolean constantTime(String a, String b) {
        return constantTime(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String esc(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }

    private static void json(HttpExchange ex, String body) throws IOException { json(ex, body, 200); }

    private static void json(HttpExchange ex, String body, int status) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(b); }
    }
}
