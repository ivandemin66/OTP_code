CREATE TABLE otp_codes (
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
