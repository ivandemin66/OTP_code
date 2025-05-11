# OTP_code

## Описание

Сервис для генерации и отправки одноразовых OTP-кодов через различные каналы: SMS (SMPP), Email, Telegram, файл. Проект реализован с использованием PostgreSQL, JDBC, собственного HTTP API на com.sun.net.httpserver, поддерживает разграничение по ролям и токенную аутентификацию.

---

## Требования к окружению

- JDK 11 или выше
- PostgreSQL 12 или выше
- Maven (для сборки проекта)
- Доступ к интернету (для Telegram API)
- Опционально: SMPPSim для тестирования SMS 
- Опционально: настроенный SMTP сервер для Email

---

## Как пользоваться сервисом

1. **Склонируйте репозиторий и перейдите в папку проекта:**
   ```sh
   git clone <repo-url>
   cd OTP_code
   ```

2. **Настройте базу данных PostgreSQL:**
   - Убедитесь, что PostgreSQL запущен и доступен
   - Создайте базу данных (можно использовать существующую)
   - В `src/main/resources/application.properties` укажите параметры подключения:
     ```properties
     db.url=jdbc:postgresql://localhost:5432/postgres
     db.user=postgres
     db.password=your_password
     ```
   - Если PostgreSQL работает на нестандартном порту, укажите нужный порт в URL

3. **Инициализируйте базу данных:**
   - Выполните SQL-скрипт `init_database.sql` в вашей базе данных:
     ```sh
     psql -U postgres -d postgres -f init_database.sql
     ```
   - Или используйте pgAdmin/другой клиент для выполнения скрипта

4. **Настройте параметры каналов отправки:**
   - **SMS**: В `sms.properties` укажите параметры SMPP-сервера (или оставьте тестовый режим)
   - **Email**: В `src/main/java/notifications/EmailNotificationService.java` укажите email и пароль
   - **Telegram**: В `src/main/java/notifications/TelegramNotificationService.java` укажите токен бота

5. **Соберите проект:**
   ```sh
   mvn clean package
   ```

6. **Запустите приложение:**
   - **Используя Maven:**
     ```sh
     mvn exec:java -Dexec.mainClass=API.HttpServerApp
     ```
   - **Или через IDE**: запустите класс `API.HttpServerApp`
   - **Или напрямую через Java:**
     ```sh
     java -cp target/classes API.HttpServerApp
     ```

7. **Тестирование базовой функциональности**
   - Можно запустить отдельно класс `Main.java`, который создаст тестового пользователя и отправит OTP-коды через все доступные каналы

---

## Тестовый режим и упрощенная настройка

По умолчанию в проекте активирован тестовый режим для сервисов рассылки:

- **SMS**: Отправка SMS эмулируется, реальных сообщений не отправляется (настройка в `SmsNotificationService.java`)
- **Telegram**: Для реальной отправки требуется указать действующий токен бота и chatId получателя
- **Email**: В тестовом режиме, для реальной отправки нужно указать SMTP-параметры и учетные данные
- **Файл**: Работает всегда, сохраняет код в файл `otp_code_НОМЕР.txt` в корне проекта

Для перехода в боевой режим нужно:
1. Установить `TEST_MODE = false` в соответствующих сервисах
2. Указать реальные параметры подключения к службам отправки уведомлений

---

## API и поддерживаемые команды

### Аутентификация и регистрация
- **POST /register** — регистрация пользователя (только один админ на систему)
  - `{ "login": "user", "password": "pass", "role": "USER|ADMIN" }`
- **POST /login** — логин, возвращает токен
  - `{ "login": "user", "password": "pass" }`
  - Ответ: `{ "token": "...", "role": "USER|ADMIN" }`

### Пользовательские операции (требуется токен)
- **POST /otp/generate** — генерация и отправка OTP-кода
  - `{ "operationId": "op1", "destination": "79991234567|email|chatId", "channel": "sms|email|telegram|file" }`
- **POST /otp/validate** — валидация OTP-кода
  - `{ "code": "123456" }`

### Админ-функции (требуется токен администратора)
- **POST /admin/config** — смена конфигурации OTP-кода
  - `{ "codeLength": 6, "lifetimeMinutes": 5 }`
- **GET /admin/users** — список всех пользователей (кроме админов)
- **DELETE /admin/delete** — удалить пользователя
  - `{ "userId": 2 }`

### Авторизация
- Для всех защищённых эндпоинтов требуется заголовок:
  - `Authorization: Bearer <token>`

---

## Примеры запросов (curl)

**Регистрация:**
```sh
curl -X POST http://localhost:8080/register -d '{"login":"admin","password":"1234","role":"ADMIN"}'
```
**Логин:**
```sh
curl -X POST http://localhost:8080/login -d '{"login":"admin","password":"1234"}'
```
**Генерация OTP:**
```sh
curl -X POST http://localhost:8080/otp/generate -H 'Authorization: Bearer <token>' -d '{"operationId":"op1","destination":"79991234567","channel":"sms"}'
```
**Валидация OTP:**
```sh
curl -X POST http://localhost:8080/otp/validate -H 'Authorization: Bearer <token>' -d '{"code":"123456"}'
```
**Смена конфигурации (админ):**
```sh
curl -X POST http://localhost:8080/admin/config -H 'Authorization: Bearer <token>' -d '{"codeLength":6,"lifetimeMinutes":5}'
```
**Список пользователей (админ):**
```sh
curl -X GET http://localhost:8080/admin/users -H 'Authorization: Bearer <token>'
```
**Удаление пользователя (админ):**
```sh
curl -X DELETE http://localhost:8080/admin/delete -H 'Authorization: Bearer <token>' -d '{"userId":2}'
```

---

## Структура проекта

- `src/main/java/API` — HTTP API (контроллеры, запуск сервера)
- `src/main/java/service` — бизнес-логика (OtpService, модели)
- `src/main/java/DAO` — DAO-слой для работы с БД
- `src/main/java/notifications` — сервисы отправки (SMS, Email, Telegram, файл)
- `src/main/resources` — конфиги, SQL-скрипты
- `sms.properties` — параметры SMPP
- `pom.xml` — зависимости Maven
- `init_database.sql` — скрипт инициализации базы данных

---

## Внешние библиотеки и зависимости

- [jSMPP](https://jsmpp.org/) — работа с SMPP
- [org.apache.httpcomponents:httpclient](https://hc.apache.org/httpcomponents-client-4.5.x/) — HTTP-запросы к Telegram API
- [slf4j-simple](https://www.slf4j.org/) — логирование (для jSMPP)
- [jakarta.mail](https://eclipse-ee4j.github.io/mail/) — отправка email
- [PostgreSQL JDBC](https://jdbc.postgresql.org/) — работа с БД

Все зависимости подключаются автоматически через Maven (`pom.xml`).

---

## Возможные проблемы и их решения

### Проблемы с базой данных:
- **Ошибка «отношение "otp_config" не существует»** - выполните скрипт init_database.sql для создания таблиц
- **Ошибка подключения к PostgreSQL** - проверьте, запущен ли сервер и правильно ли указаны параметры в application.properties
- **Ошибка «нарушает ограничение внешнего ключа»** - убедитесь, что в таблице users есть запись перед созданием OTP-кода

### Проблемы с отправкой уведомлений:
- **SLF4J: Failed to load class** - некритичное предупреждение, можно игнорировать или добавить зависимость slf4j-simple в pom.xml
- **Ошибка отправки SMS: Connection refused** - включите тестовый режим или настройте подключение к SMPP-серверу
- **Telegram API: 401 Unauthorized** - указан неверный токен бота в TelegramNotificationService.java
- **Email: Connection error** - проверьте настройки SMTP-сервера и учетные данные

---

## Документация по каждому модулю

- **API** — HTTP API, обработчики запросов, авторизация, маршрутизация.
- **service** — бизнес-логика генерации, отправки и валидации OTP-кодов.
- **DAO** — слой доступа к данным (работа с PostgreSQL через JDBC).
- **notifications** — сервисы отправки OTP-кодов через разные каналы.

---

## Подготовка к отправке проекта на проверку

- Все функции реализованы и протестированы.
- Код структурирован, снабжён комментариями.
- В репозитории есть README.md с актуальными инструкциями.
- Все внешние библиотеки подключаются через Maven.
- Для тестирования SMS требуется запущенный SMPPSim или включенный тестовый режим.
- Для Telegram требуется токен и chatId.
- Для email — рабочий SMTP и email/пароль приложения.

---

**Если возникнут вопросы по запуску или тестированию — смотрите комментарии в коде или обращайтесь к документации в README.**
