package DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// DAO для работы с пользователями
public class UserDao {
    private static final Logger logger = Logger.getLogger(UserDao.class.getName());

    // Регистрация нового пользователя
    public boolean registerUser(String login, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, passwordHash);
            ps.setString(3, role);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException | org.postgresql.util.PSQLException e) {
            // Логин уже существует
            logger.severe(String.format("[%s] Error: %s", java.time.LocalDateTime.now(), e.getMessage()));
            return false;
        }
    }

    // Поиск пользователя по логину
    public User findByLogin(String login) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getLong("id"),
                        rs.getString("login"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                    );
                }
            }
        }
        return null;
    }

    // Получить всех пользователей, кроме администраторов
    public List<User> getAllNonAdmins() throws SQLException {
        String sql = "SELECT * FROM users WHERE role <> 'ADMIN'";
        List<User> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                    rs.getLong("id"),
                    rs.getString("login"),
                    rs.getString("password_hash"),
                    rs.getString("role")
                ));
            }
        }
        return users;
    }

    // Удалить пользователя и его OTP-коды
    public boolean deleteUser(long userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // Проверить, есть ли хотя бы один администратор
    public boolean adminExists() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Вспомогательный класс User (можно вынести отдельно)
    public static class User {
        public long id;
        public String login;
        public String passwordHash;
        public String role;
        public User(long id, String login, String passwordHash, String role) {
            this.id = id;
            this.login = login;
            this.passwordHash = passwordHash;
            this.role = role;
        }
    }
} 