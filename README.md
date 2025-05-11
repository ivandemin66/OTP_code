# OTP_code

## Описание

Сервис для генерации и отправки одноразовых OTP-кодов через различные каналы: SMS (SMPP), Email, Telegram, файл. Проект реализован с использованием PostgreSQL, JDBC, собственного HTTP API на com.sun.net.httpserver, поддерживает разграничение по ролям и токенную аутентификацию.

---

## Как пользоваться сервисом

1. **Склонируйте репозиторий и перейдите в папку проекта:**
   ```sh
   git clone <repo-url>
   cd OTP_code
   ```
2. **Настройте параметры подключения:**
   - В `src/main/resources/application.properties` укажите параметры PostgreSQL.
   - В `sms.properties` — параметры SMPP.
   - В `EmailNotificationService.java` — email и пароль для отправки писем.
   - В `TelegramNotificationService.java` — токен Telegram-бота.
3. **Создайте таблицы в PostgreSQL:**
   - Выполните скрипт `src/main/resources/create_tables.sql` в вашей базе данных.
4. **Соберите проект:**
   ```sh
   mvn clean install
   ```
5. **Запустите эмулятор SMPP (SMPPSim), если тестируете SMS:**
   - Скачайте [SMPPSim](https://github.com/delhee/SMPPSim/releases/tag/3.0.0), распакуйте и запустите `startsmppsim.bat`.
6. **Запустите приложение:**
   ```sh
   mvn exec:java -Dexec.mainClass=API.HttpServerApp
   ```
   или через IDE (запуск класса `API.HttpServerApp`).

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

---

## Внешние библиотеки и зависимости

- [jSMPP](https://jsmpp.org/) — работа с SMPP
- [org.apache.httpcomponents:httpclient](https://hc.apache.org/httpcomponents-client-4.5.x/) — HTTP-запросы к Telegram API
- [slf4j-simple](https://www.slf4j.org/) — логирование (для jSMPP)
- [jakarta.mail](https://eclipse-ee4j.github.io/mail/) — отправка email
- [PostgreSQL JDBC](https://jdbc.postgresql.org/) — работа с БД

Все зависимости подключаются автоматически через Maven (`pom.xml`).

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
- Для тестирования SMS требуется запущенный SMPPSim.
- Для Telegram требуется токен и chatId.
- Для email — рабочий SMTP и email/пароль приложения.

---

**Если возникнут вопросы по запуску или тестированию — смотрите комментарии в коде или обращайтесь к документации в README.**
