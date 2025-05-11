package DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

// Класс для управления соединением с БД PostgreSQL
public class Database {
    private static String url;
    private static String user;
    private static String password;

    static {
        try (InputStream input = Database.class.getClassLoader().getResourceAsStream("application.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                url = prop.getProperty("db.url");
                user = prop.getProperty("db.user");
                password = prop.getProperty("db.password");
            } else {
                throw new RuntimeException("Не найден файл application.properties");
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки application.properties", e);
        }
    }

    // Получить соединение с БД
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
} 