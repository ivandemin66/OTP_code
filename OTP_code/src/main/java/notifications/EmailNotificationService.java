package notifications;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

// Сервис для отправки OTP-кода по email
public class EmailNotificationService {
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private static final Logger logger = Logger.getLogger(EmailNotificationService.class.getName());

    public EmailNotificationService() {
        // В реальном проекте параметры берутся из конфига
        this.username = "your_email@example.com"; // Замените на свой email
        this.password = "your_password"; // Замените на свой пароль
        this.host = "smtp.yandex.ru"; // SMTP сервер (пример: smtp.yandex.ru, smtp.gmail.com)
        this.port = 465; // Порт SMTP (465 для SSL, 587 для TLS)
    }

    public void sendCode(String to, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Ваш OTP-код");
            message.setText("Ваш код подтверждения: " + code);
            Transport.send(message);
            logger.info("OTP-код успешно отправлен на email: " + to);
        } catch (MessagingException e) {
            logger.severe("Ошибка отправки email: " + e.getMessage());
        }
    }
} 