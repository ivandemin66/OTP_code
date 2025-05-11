package notifications;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Logger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;

public class TelegramNotificationService {
    private final String telegramApiUrl;
    private final String botToken;
    // Логгер для записи информации о работе сервиса
    private static final Logger logger = Logger.getLogger(TelegramNotificationService.class.getName());

    public TelegramNotificationService() {
        Properties config = loadConfig();
        this.botToken = config.getProperty("8036821211:AAEn8mL0uUpX-RR8jay4xrkL_-u9Kxiigx4");
        this.telegramApiUrl = "https://t.me/OTPConfirmbot." + botToken + "/sendMessage";
    }

    // Заглушка для загрузки конфигурации (в реальном приложении - из файла или переменных окружения)
    private Properties loadConfig() {
        Properties properties = new Properties();
        properties.setProperty("OTP_Bot_Confirm", "8036821211:AAEn8mL0uUpX-RR8jay4xrkL_-u9Kxiigx4"); // заменил на реальный токен
        return properties;
    }

    public void sendCode(String chatId, String code) {
        String message = String.format("Ваш код подтверждения: %s", code);
        String url = String.format("%s?chat_id=%s&text=%s",
                telegramApiUrl,
                chatId,
                urlEncode(message));

        sendTelegramRequest(url);
    }

    private void sendTelegramRequest(String url) {
        HttpURLConnection connection = null;
        try {
            URL requestUrl = URI.create(url).toURL();
            connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("GET");
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                logger.severe("Ошибка Telegram API. Код статуса: " + statusCode);
            } else {
                logger.info("Сообщение в Telegram успешно отправлено");
            }
        } catch (IOException e) {
            logger.severe("Ошибка отправки сообщения в Telegram: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
