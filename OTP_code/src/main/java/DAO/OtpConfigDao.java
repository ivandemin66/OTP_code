package DAO;

import java.sql.*;

// DAO для работы с конфигурацией OTP-кода
public class OtpConfigDao {
    // Получить текущую конфигурацию (всегда одна запись)
    public OtpConfig getConfig() throws SQLException {
        String sql = "SELECT * FROM otp_config LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new OtpConfig(
                    rs.getLong("id"),
                    rs.getInt("code_length"),
                    rs.getInt("lifetime_minutes")
                );
            } else {
                // Если записи нет, создаем её с дефолтными значениями
                return createDefaultConfig();
            }
        }
    }

    // Создание дефолтной конфигурации, если она отсутствует
    private OtpConfig createDefaultConfig() throws SQLException {
        String sql = "INSERT INTO otp_config (code_length, lifetime_minutes) VALUES (6, 10) RETURNING id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new OtpConfig(rs.getLong("id"), 6, 10);
            }
            throw new SQLException("Не удалось создать конфигурацию OTP");
        }
    }

    // Обновить конфигурацию
    public boolean updateConfig(int codeLength, int lifetimeMinutes) throws SQLException {
        String sql = "UPDATE otp_config SET code_length = ?, lifetime_minutes = ? WHERE id = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codeLength);
            ps.setInt(2, lifetimeMinutes);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // Если нет записи с id=1, создаем новую запись
                return createInitialConfig(codeLength, lifetimeMinutes);
            }
            return true;
        }
    }

    // Создание начальной конфигурации с заданными параметрами
    private boolean createInitialConfig(int codeLength, int lifetimeMinutes) throws SQLException {
        String sql = "INSERT INTO otp_config (code_length, lifetime_minutes) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codeLength);
            ps.setInt(2, lifetimeMinutes);
            return ps.executeUpdate() > 0;
        }
    }

    // Вспомогательный класс OtpConfig (можно вынести отдельно)
    public static class OtpConfig {
        public long id;
        public int codeLength;
        public int lifetimeMinutes;
        public OtpConfig(long id, int codeLength, int lifetimeMinutes) {
            this.id = id;
            this.codeLength = codeLength;
            this.lifetimeMinutes = lifetimeMinutes;
        }
    }
} 