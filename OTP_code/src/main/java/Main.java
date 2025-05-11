import DAO.OtpConfigDao;
import service.OtpService;

public class Main {
    public static void main(String[] args) {
        // Инициализация DAO и сервисов уведомлений
        DAO.OtpConfigDao otpConfigDao = new DAO.OtpConfigDao();
        OtpService otpService = getOtpService(otpConfigDao);

        // Пример генерации и отправки OTP-кода через разные каналы
        String operationId = "login";
        Long userId = 1L;
        String destination = "79991234567"; // телефон или chatId или email

        // Отправка через SMS
        otpService.generateAndSendOtpCode(operationId, userId, destination, "sms");
        // Отправка через Telegram
        otpService.generateAndSendOtpCode(operationId, userId, destination, "telegram");
        // Отправка в файл
        otpService.generateAndSendOtpCode(operationId, userId, destination, "file");
    }

    private static OtpService getOtpService(OtpConfigDao otpConfigDao) {
        DAO.OtpCodeDao otpCodeDao = new DAO.OtpCodeDao();
        notifications.FileNotificationService fileNotificationService = new notifications.FileNotificationService();
        notifications.SmsNotificationService smsNotificationService = new notifications.SmsNotificationService();
        notifications.TelegramNotificationService telegramNotificationService = new notifications.TelegramNotificationService();
        notifications.EmailNotificationService emailNotificationService = new notifications.EmailNotificationService();

        // Инициализация основного сервиса OTP
        OtpService otpService = new OtpService(
                otpConfigDao,
                otpCodeDao,
                fileNotificationService,
                smsNotificationService,
                telegramNotificationService,
                emailNotificationService
        );
        return otpService;
    }
}