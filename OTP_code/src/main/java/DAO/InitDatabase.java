package DAO;

import java.sql.*;

/**
 * Класс для инициализации базы данных
 * Создает необходимые таблицы, если они не существуют
 */
public class InitDatabase {
    public static void main(String[] args) {
        try {
            initDatabase();
            System.out.println("База данных успешно инициализирована");
        } catch (Exception e) {
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initDatabase() throws SQLException {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();
            
            // Создание таблицы пользователей
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "login VARCHAR(255) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL" +
                    ")");
            
            // Создание таблицы конфигурации OTP
            stmt.execute("CREATE TABLE IF NOT EXISTS otp_config (" +
                    "id SERIAL PRIMARY KEY, " +
                    "code_length INT NOT NULL, " +
                    "lifetime_minutes INT NOT NULL" +
                    ")");
            
            // Проверяем, есть ли записи в таблице otp_config, если нет - создаем
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM otp_config");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO otp_config (code_length, lifetime_minutes) VALUES (6, 10)");
                System.out.println("Добавлена стандартная конфигурация OTP");
            }
            
            // Создание таблицы OTP-кодов
            stmt.execute("CREATE TABLE IF NOT EXISTS otp_codes (" +
                    "id SERIAL PRIMARY KEY, " +
                    "code VARCHAR(50) NOT NULL, " +
                    "operation_id VARCHAR(255) NOT NULL, " +
                    "user_id BIGINT NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "expires_at TIMESTAMP NOT NULL, " +
                    "used_at TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")");
        } finally {
            // Закрываем ресурсы
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
} 