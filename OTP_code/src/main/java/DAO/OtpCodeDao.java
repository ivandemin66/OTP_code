package DAO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// DAO для работы с OTP-кодами
public class OtpCodeDao {
    // Создать новый OTP-код
    public long createCode(OtpCode code) throws SQLException {
        String sql = "INSERT INTO otp_codes (code, operation_id, user_id, status, created_at, expires_at, used_at) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.code);
            ps.setString(2, code.operationId);
            ps.setLong(3, code.userId);
            ps.setString(4, code.status);
            ps.setTimestamp(5, Timestamp.valueOf(code.createdAt));
            ps.setTimestamp(6, Timestamp.valueOf(code.expiresAt));
            if (code.usedAt != null) {
                ps.setTimestamp(7, Timestamp.valueOf(code.usedAt));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    // Найти OTP-код по коду и userId
    public OtpCode findByCodeAndUser(String code, long userId) throws SQLException {
        String sql = "SELECT * FROM otp_codes WHERE code = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(rs);
                }
            }
        }
        return null;
    }

    // Обновить статус OTP-кода
    public boolean updateStatus(long id, String status, LocalDateTime usedAt) throws SQLException {
        String sql = "UPDATE otp_codes SET status = ?, used_at = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (usedAt != null) {
                ps.setTimestamp(2, Timestamp.valueOf(usedAt));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Удалить все OTP-коды пользователя
    public void deleteByUserId(long userId) throws SQLException {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    // Найти все коды со статусом ACTIVE и истёкшим сроком
    public List<OtpCode> findExpiredActiveCodes(LocalDateTime now) throws SQLException {
        String sql = "SELECT * FROM otp_codes WHERE status = 'ACTIVE' AND expires_at < ?";
        List<OtpCode> expired = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expired.add(fromResultSet(rs));
                }
            }
        }
        return expired;
    }

    // Вспомогательный класс OtpCode (можно вынести отдельно)
    public static class OtpCode {
        public long id;
        public String code;
        public String operationId;
        public long userId;
        public String status;
        public LocalDateTime createdAt;
        public LocalDateTime expiresAt;
        public LocalDateTime usedAt;
        public OtpCode(long id, String code, String operationId, long userId, String status, LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime usedAt) {
            this.id = id;
            this.code = code;
            this.operationId = operationId;
            this.userId = userId;
            this.status = status;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.usedAt = usedAt;
        }
    }

    private OtpCode fromResultSet(ResultSet rs) throws SQLException {
        return new OtpCode(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("operation_id"),
            rs.getLong("user_id"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("expires_at").toLocalDateTime(),
            rs.getTimestamp("used_at") != null ? rs.getTimestamp("used_at").toLocalDateTime() : null
        );
    }
} 