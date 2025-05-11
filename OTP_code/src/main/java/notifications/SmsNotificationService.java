package notifications;

import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

// Сервис для отправки SMS через SMPP
public class SmsNotificationService {
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;
    // Логгер для записи информации о работе сервиса
    private static final Logger logger = Logger.getLogger(SmsNotificationService.class.getName());
    private static final boolean TEST_MODE = true; // Тестовый режим (без реального подключения)

    public SmsNotificationService() {
        Properties config = loadConfig();
        this.host = config.getProperty("smpp.host", "localhost");
        this.port = Integer.parseInt(config.getProperty("smpp.port", "2775"));
        this.systemId = config.getProperty("smpp.system_id", "smppclient1");
        this.password = config.getProperty("smpp.password", "password");
        this.systemType = config.getProperty("smpp.system_type", "OTP");
        this.sourceAddress = config.getProperty("smpp.source_addr", "OTPService");
    }

    // Загрузка конфигурации из файла sms.properties
    private Properties loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("sms.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            logger.warning("Не удалось загрузить sms.properties, используются значения по умолчанию");
        }
        return properties;
    }

    // Отправка SMS через SMPP
    public void sendCode(String destination, String code) {
        if (TEST_MODE) {
            // В тестовом режиме просто логируем сообщение без реальной отправки
            logger.info("Тестовый режим: SMS с кодом " + code + " будет отправлен на номер " + destination);
            return;
        }
        
        SMPPSession session = null;
        try {
            // 1. Установка соединения и привязка
            session = new SMPPSession();
            session.connectAndBind(
                    host,
                    port,
                    new BindParameter(
                            BindType.BIND_TX,
                            systemId,
                            password,
                            systemType,
                            TypeOfNumber.INTERNATIONAL,
                            NumberingPlanIndicator.ISDN,
                            null
                    )
            );

            // 2. Отправка сообщения
            String messageId = session.submitShortMessage(
                    "CMT",
                    TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, sourceAddress,
                    TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, destination,
                    new org.jsmpp.bean.ESMClass(),
                    (byte)0,
                    (byte)1,
                    null,
                    null,
                    new org.jsmpp.bean.RegisteredDelivery(0),
                    (byte)0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte)0,
                    ("Your code: " + code).getBytes()
            );
            logger.info("SMS с кодом успешно отправлено на номер: " + destination + ", messageId: " + messageId);
        } catch (Exception e) {
            logger.severe("Ошибка отправки SMS: " + e.getMessage());
        } finally {
            if (session != null) {
                session.unbindAndClose();
            }
        }
    }
}

