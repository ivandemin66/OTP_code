package service;

import DAO.OtpCodeDao;
import DAO.OtpConfigDao;
import notifications.FileNotificationService;
import notifications.SmsNotificationService;
import notifications.TelegramNotificationService;
import notifications.EmailNotificationService;

import java.time.LocalDateTime;
import java.util.Random;

// Сервис для генерации и отправки OTP-кодов
public class OtpService {
    private final OtpConfigDao otpConfigDao;
    private final OtpCodeDao otpCodeDao;
    private final FileNotificationService fileNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final TelegramNotificationService telegramNotificationService;
    private final EmailNotificationService emailNotificationService;

    // Инъекция зависимостей через конструктор (SOLID)
    public OtpService(OtpConfigDao otpConfigDao,
                      OtpCodeDao otpCodeDao,
                      FileNotificationService fileNotificationService,
                      SmsNotificationService smsNotificationService,
                      TelegramNotificationService telegramNotificationService,
                      EmailNotificationService emailNotificationService) {
        this.otpConfigDao = otpConfigDao;
        this.otpCodeDao = otpCodeDao;
        this.fileNotificationService = fileNotificationService;
        this.smsNotificationService = smsNotificationService;
        this.telegramNotificationService = telegramNotificationService;
        this.emailNotificationService = emailNotificationService;
    }

    // Генерация и отправка OTP-кода
    public OtpCodeDao.OtpCode generateAndSendOtpCode(String operationId, Long userId, String destination, String channel) {
        DAO.OtpConfigDao.OtpConfig config;
        try {
            config = otpConfigDao.getConfig();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения конфигурации OTP", e);
        }
        String code = generateRandomCode(config.codeLength);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(config.lifetimeMinutes);

        DAO.OtpCodeDao.OtpCode otpCode = new DAO.OtpCodeDao.OtpCode(
            0L, code, operationId, userId, "ACTIVE", now, expiry, null
        );
        try {
            long id = otpCodeDao.createCode(otpCode);
            otpCode.id = id;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения OTP-кода", e);
        }

        // Отправка кода через выбранный канал
        sendCodeViaChannel(destination, code, channel);

        return otpCode;
    }

    // Генерация случайного кода
    private String generateRandomCode(int length) {
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // Отправка кода через выбранный канал
    private void sendCodeViaChannel(String destination, String code, String channel) {
        switch (channel.toLowerCase()) {
            case "sms":
                try {
                    smsNotificationService.sendCode(destination, code);
                } catch (Exception e) {
                    // Логируем ошибку отправки SMS
                    System.err.println("Ошибка отправки SMS: " + e.getMessage());
                }
                break;
            case "telegram":
                telegramNotificationService.sendCode(destination, code);
                break;
            case "email":
                emailNotificationService.sendCode(destination, code);
                break;
            case "file":
            default:
                fileNotificationService.sendCode(destination, code);
                break;
        }
    }
}
