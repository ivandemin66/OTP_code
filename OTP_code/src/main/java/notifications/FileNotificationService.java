package notifications;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class FileNotificationService {
    // Директория для сохранения файлов с кодами подтверждения
    private static final String OUTPUT_DIRECTORY = System.getProperty("user.dir");
    // Логгер для записи информации о работе сервиса
    private static final Logger logger = Logger.getLogger(FileNotificationService.class.getName());

    public void sendCode(String destination, String code) {
        String fileName = "otp_code_" + destination + ".txt";
        Path filePath = Path.of(OUTPUT_DIRECTORY, fileName);

        try {
            Files.write(
                    filePath,
                    List.of("Код подтверждения для " + destination + ": " + code),
                    StandardCharsets.UTF_8
            );
            logger.info("Код сохранен в файл: " + filePath);
        } catch (IOException e) {
            logger.severe("Ошибка сохранения кода в файл: " + e.getMessage());
            throw new RuntimeException("Failed to save code to file", e);
        }
    }
}

