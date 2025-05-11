import DAO.InitDatabase;
import DAO.OtpConfigDao;
import DAO.UserDao;
import service.OtpService;

public class Main {
    public static void main(String[] args) {
        try {
            // Инициализация базы данных (создание таблиц)
            InitDatabase.initDatabase();
            
            // Создаем тестового пользователя, если он еще не существует
            UserDao userDao = new UserDao();
            Long userId = createTestUser(userDao);
            
            // Инициализация DAO и сервисов уведомлений
            DAO.OtpConfigDao otpConfigDao = new DAO.OtpConfigDao();
            OtpService otpService = getOtpService(otpConfigDao);
    
            // Пример генерации и отправки OTP-кода через разные каналы
            String operationId = "login";
            String destination = "79991234567"; // телефон, или chatId, или email
    
            // Отправка через SMS
            otpService.generateAndSendOtpCode(operationId, userId, destination, "sms");
            // Отправка через Telegram
            otpService.generateAndSendOtpCode(operationId, userId, destination, "telegram");
            // Отправка в файл
            otpService.generateAndSendOtpCode(operationId, userId, destination, "file");
            
            System.out.println("OTP-коды успешно сгенерированы и отправлены");
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            // Используем более подробное логирование ошибки
            System.err.println("Критическая ошибка при выполнении программы:");
            System.err.println("Сообщение: " + e.getMessage());
            System.err.println("Класс исключения: " + e.getClass().getName());
            System.err.println("Стек вызовов:");
            for (StackTraceElement element : e.getStackTrace()) {
                System.err.println("\tв " + element.toString());
            }
        }
    }
    
    // Создание тестового пользователя, если он еще не существует
    private static Long createTestUser(UserDao userDao) throws Exception {
        DAO.UserDao.User testUser = userDao.findByLogin("test");
        if (testUser != null) {
            System.out.println("Используется существующий тестовый пользователь с id: " + testUser.id);
            return testUser.id;
        }
        
        // Захешированный пароль "password" (можно использовать API.HttpServerApp.hashPassword)
        String passwordHash = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"; // SHA-256 хеш для "password"
        boolean success = userDao.registerUser("test", passwordHash, "USER");
        if (success) {
            testUser = userDao.findByLogin("test");
            System.out.println("Создан новый тестовый пользователь с id: " + testUser.id);
            return testUser.id;
        } else {
            throw new Exception("Не удалось создать тестового пользователя");
        }
    }

    private static OtpService getOtpService(OtpConfigDao otpConfigDao) {
        DAO.OtpCodeDao otpCodeDao = new DAO.OtpCodeDao();
        notifications.FileNotificationService fileNotificationService = new notifications.FileNotificationService();
        notifications.SmsNotificationService smsNotificationService = new notifications.SmsNotificationService();
        notifications.TelegramNotificationService telegramNotificationService = new notifications.TelegramNotificationService();
        notifications.EmailNotificationService emailNotificationService = new notifications.EmailNotificationService();

        // Инициализация основного сервиса OTP
        return new OtpService(
                otpConfigDao,
                otpCodeDao,
                fileNotificationService,
                smsNotificationService,
                telegramNotificationService,
                emailNotificationService
        );
    }
}