CREATE TABLE otp_config (
                            id SERIAL PRIMARY KEY,
                            code_length INT NOT NULL,
                            lifetime_minutes INT NOT NULL
);
