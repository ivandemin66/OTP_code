-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Таблица конфигурации OTP-кода (только одна запись)
CREATE TABLE IF NOT EXISTS otp_config (
    id SERIAL PRIMARY KEY,
    code_length INT NOT NULL,
    lifetime_minutes INT NOT NULL
);

-- Вставляем начальную конфигурацию если таблица пуста
DO $$ 
BEGIN
    IF (SELECT COUNT(*) FROM otp_config) = 0 THEN
        INSERT INTO otp_config (code_length, lifetime_minutes) VALUES (6, 10);
    END IF;
END $$;

-- Таблица OTP-кодов
CREATE TABLE IF NOT EXISTS otp_codes (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    operation_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
); 