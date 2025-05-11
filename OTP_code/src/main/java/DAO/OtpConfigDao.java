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
            }
        }
        return null;
    }

    // Обновить конфигурацию
    public boolean updateConfig(int codeLength, int lifetimeMinutes) throws SQLException {
        String sql = "UPDATE otp_config SET code_length = ?, lifetime_minutes = ? WHERE id = 1";
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