package API;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import DAO.UserDao;
import service.OtpService;
import DAO.OtpConfigDao;
import DAO.OtpCodeDao;
import notifications.FileNotificationService;
import notifications.SmsNotificationService;
import notifications.TelegramNotificationService;
import notifications.EmailNotificationService;
import java.time.LocalDateTime;

// Класс для запуска HTTP API
public class HttpServerApp {
    private static final int PORT = 8080;
    private static final Logger logger = Logger.getLogger(HttpServerApp.class.getName());

    // Простое хранилище токенов (в памяти)
    private static final Map<String, Session> sessions = new HashMap<>();
    private static final long TOKEN_TTL_MINUTES = 60;
    static class Session {
        public long userId;
        public String role;
        public long expiresAt;
        public Session(long userId, String role, long expiresAt) {
            this.userId = userId;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка хеширования пароля", e);
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Регистрация и логин
        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());

        // Пользовательские OTP-операции
        server.createContext("/otp/generate", new OtpGenerateHandler());
        server.createContext("/otp/validate", new OtpValidateHandler());

        // Админ-функции
        server.createContext("/admin/config", new AdminConfigHandler());
        server.createContext("/admin/users", new AdminUsersHandler());
        server.createContext("/admin/delete", new AdminDeleteUserHandler());

        logger.info("HTTP сервер запущен на порту " + PORT);
        server.start();
    }

    // Универсальное логирование запроса и ответа
    private static void logRequest(com.sun.net.httpserver.HttpExchange exchange, String body) {
        logger.info(String.format("[%s] %s %s - body: %s", java.time.LocalDateTime.now(), exchange.getRequestMethod(), exchange.getRequestURI(), body));
    }
    private static void logResponse(com.sun.net.httpserver.HttpExchange exchange, int status, String response) {
        logger.info(String.format("[%s] Response %d for %s %s: %s", java.time.LocalDateTime.now(), status, exchange.getRequestMethod(), exchange.getRequestURI(), response));
    }
    private static void logError(Exception e) {
        logger.severe(String.format("[%s] Error: %s", java.time.LocalDateTime.now(), e.getMessage()));
    }

    // Заглушки для хэндлеров (реализуем далее)
    static class RegisterHandler implements HttpHandler {
        private final UserDao userDao = new UserDao();
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            logRequest(exchange, body);
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); logResponse(exchange, 405, ""); return;
            }
            String login = JsonUtils.extractString(body, "login");
            String password = JsonUtils.extractString(body, "password");
            String role = JsonUtils.extractString(body, "role");
            if (login == null || password == null || role == null) {
                sendJsonAndLog(exchange, 400, "{\"error\":\"login, password, role обязательны\"}");
                return;
            }
            if (role.equalsIgnoreCase("ADMIN")) {
                try {
                    if (userDao.adminExists()) {
                        sendJsonAndLog(exchange, 400, "{\"error\":\"Администратор уже существует\"}");
                        return;
                    }
                } catch (Exception e) {
                    logError(e);
                    sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка проверки администратора\"}");
                    return;
                }
            }
            String passwordHash = HttpServerApp.hashPassword(password);
            try {
                boolean ok = userDao.registerUser(login, passwordHash, role.toUpperCase());
                if (ok) {
                    sendJsonAndLog(exchange, 201, "{\"result\":\"Пользователь зарегистрирован\"}");
                } else {
                    sendJsonAndLog(exchange, 409, "{\"error\":\"Пользователь с таким логином уже существует\"}");
                }
            } catch (Exception e) {
                logError(e);
                sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка регистрации пользователя\"}");
            }
        }
    }
    static class LoginHandler implements HttpHandler {
        private final UserDao userDao = new UserDao();
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            logRequest(exchange, body);
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); logResponse(exchange, 405, ""); return;
            }
            String login = JsonUtils.extractString(body, "login");
            String password = JsonUtils.extractString(body, "password");
            if (login == null || password == null) {
                sendJsonAndLog(exchange, 400, "{\"error\":\"login и password обязательны\"}");
                return;
            }
            UserDao.User user;
            try {
                user = userDao.findByLogin(login);
            } catch (Exception e) {
                logError(e);
                sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка поиска пользователя\"}");
                return;
            }
            if (user == null || !HttpServerApp.hashPassword(password).equals(user.passwordHash)) {
                sendJsonAndLog(exchange, 401, "{\"error\":\"Неверный логин или пароль\"}");
                return;
            }
            // Генерируем токен
            String token = UUID.randomUUID().toString();
            long expiresAt = System.currentTimeMillis() + TOKEN_TTL_MINUTES * 60 * 1000;
            sessions.put(token, new Session(user.id, user.role, expiresAt));
            String resp = String.format("{\"token\":\"%s\",\"role\":\"%s\"}", token, user.role);
            sendJsonAndLog(exchange, 200, resp);
        }
    }
    // Проверка токена и возврат сессии (или null)
    private static Session authorize(com.sun.net.httpserver.HttpExchange exchange, String requiredRole) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendJsonAndLog(exchange, 401, "{\"error\":\"Требуется авторизация\"}");
            return null;
        }
        String token = auth.substring(7);
        Session session = sessions.get(token);
        if (session == null || session.expiresAt < System.currentTimeMillis()) {
            sendJsonAndLog(exchange, 401, "{\"error\":\"Недействительный или истёкший токен\"}");
            return null;
        }
        if (requiredRole != null && !session.role.equalsIgnoreCase(requiredRole)) {
            sendJsonAndLog(exchange, 403, "{\"error\":\"Недостаточно прав\"}");
            return null;
        }
        return session;
    }
    // Статический метод для отправки JSON-ответа
    private static void sendJsonStatic(com.sun.net.httpserver.HttpExchange exchange, int code, String json) throws IOException {
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, resp.length);
        exchange.getResponseBody().write(resp);
        exchange.close();
    }
    private static void sendJsonAndLog(com.sun.net.httpserver.HttpExchange exchange, int code, String json) throws IOException {
        sendJsonStatic(exchange, code, json);
        logResponse(exchange, code, json);
    }
    static class OtpGenerateHandler implements HttpHandler {
        private final OtpService otpService = new OtpService(
                new OtpConfigDao(),
                new OtpCodeDao(),
                new FileNotificationService(),
                new SmsNotificationService(),
                new TelegramNotificationService(),
                new EmailNotificationService()
        );
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            logRequest(exchange, body);
            Session session = authorize(exchange, null); // Любой авторизованный пользователь
            if (session == null) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); logResponse(exchange, 405, ""); return;
            }
            String operationId = JsonUtils.extractString(body, "operationId");
            String destination = JsonUtils.extractString(body, "destination");
            String channel = JsonUtils.extractString(body, "channel");
            if (operationId == null || destination == null || channel == null) {
                sendJsonAndLog(exchange, 400, "{\"error\":\"operationId, destination, channel обязательны\"}");
                return;
            }
            try {
                DAO.OtpCodeDao.OtpCode otpCode = otpService.generateAndSendOtpCode(
                        operationId,
                        session.userId,
                        destination,
                        channel
                );
                String resp = String.format("{\"result\":\"OTP-код отправлен\",\"codeId\":%d}", otpCode.id);
                sendJsonAndLog(exchange, 200, resp);
            } catch (Exception e) {
                logError(e);
                sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка генерации или отправки OTP-кода\"}");
            }
        }
    }
    static class OtpValidateHandler implements HttpHandler {
        private final DAO.OtpCodeDao otpCodeDao = new DAO.OtpCodeDao();
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            logRequest(exchange, body);
            Session session = authorize(exchange, null); // Любой авторизованный пользователь
            if (session == null) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); logResponse(exchange, 405, ""); return;
            }
            String code = JsonUtils.extractString(body, JsonKeys.CODE);
            if (code == null) {
                sendJsonAndLog(exchange, 400, "{\"error\":\"code обязателен\"}");
                return;
            }
            try {
                DAO.OtpCodeDao.OtpCode otp = otpCodeDao.findByCodeAndUser(code, session.userId);
                if (otp == null) {
                    sendJsonAndLog(exchange, 404, "{\"error\":\"Код не найден\"}");
                    return;
                }
                if (!"ACTIVE".equals(otp.status)) {
                    sendJsonAndLog(exchange, 400, "{\"error\":\"Код неактивен\"}");
                    return;
                }
                if (otp.expiresAt.isBefore(LocalDateTime.now())) {
                    otpCodeDao.updateStatus(otp.id, "EXPIRED", null);
                    sendJsonAndLog(exchange, 400, "{\"error\":\"Код истёк\"}");
                    return;
                }
                // Валидный код, отмечаем как USED
                otpCodeDao.updateStatus(otp.id, "USED", LocalDateTime.now());
                sendJsonAndLog(exchange, 200, "{\"result\":\"Код подтверждён\"}");
            } catch (Exception e) {
                logError(e);
                sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка валидации OTP-кода\"}");
            }
        }
    }
    static class AdminConfigHandler implements HttpHandler {
        private final DAO.OtpConfigDao otpConfigDao = new DAO.OtpConfigDao();
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            logRequest(exchange, body);
            Session session = authorize(exchange, "ADMIN"); // Только админ
            if (session == null) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); logResponse(exchange, 405, ""); return;
            }
            Integer codeLength = JsonUtils.extractInt(body, "codeLength");
            Integer lifetimeMinutes = JsonUtils.extractInt(body, "lifetimeMinutes");
            if (codeLength == null || lifetimeMinutes == null) {
                sendJsonAndLog(exchange, 400, "{\"error\":\"codeLength и lifetimeMinutes обязательны\"}");
                return;
            }
            try {
                boolean ok = otpConfigDao.updateConfig(codeLength, lifetimeMinutes);
                if (ok) {
                    sendJsonAndLog(exchange, 200, "{\"result\":\"Конфигурация обновлена\"}");
                } else {
                    sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка обновления конфигурации\"}");
                }
            } catch (Exception e) {
                logError(e);
                sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка обновления конфигурации\"}");
            }
        }
    }
    static class AdminUsersHandler implements HttpHandler {
        private final DAO.UserDao userDao = new DAO.UserDao();
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            logRequest(exchange, "");
            Session session = authorize(exchange, "ADMIN"); // Только админ
            if (session == null) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1); logResponse(exchange, 405, ""); return;
            }
            try {
                java.util.List<DAO.UserDao.User> users = userDao.getAllNonAdmins();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < users.size(); i++) {
                    DAO.UserDao.User u = users.get(i);
                    sb.append(String.format("{\"id\":%d,\"login\":\"%s\",\"role\":\"%s\"}", u.id, u.login, u.role));
                    if (i < users.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonAndLog(exchange, 200, sb.toString());
            } catch (Exception e) {
                logError(e);
                sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка получения пользователей\"}");
            }
        }
    }
    static class AdminDeleteUserHandler implements HttpHandler {
        private final DAO.UserDao userDao = new DAO.UserDao();
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            logRequest(exchange, body);
            Session session = authorize(exchange, "ADMIN"); // Только админ
            if (session == null) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                exchange.sendResponseHeaders(405, -1); logResponse(exchange, 405, ""); return;
            }
            Long userId = JsonUtils.extractLong(body, JsonKeys.USER_ID);
            if (userId == null) {
                sendJsonAndLog(exchange, 400, "{\"error\":\"userId обязателен\"}");
                return;
            }
            try {
                boolean ok = userDao.deleteUser(userId);
                if (ok) {
                    sendJsonAndLog(exchange, 200, "{\"result\":\"Пользователь удалён\"}");
                } else {
                    sendJsonAndLog(exchange, 404, "{\"error\":\"Пользователь не найден\"}");
                }
            } catch (Exception e) {
                logError(e);
                sendJsonAndLog(exchange, 500, "{\"error\":\"Ошибка удаления пользователя\"}");
            }
        }
    }
} 