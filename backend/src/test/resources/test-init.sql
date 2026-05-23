
CREATE TABLE IF NOT EXISTS tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

INSERT INTO tests (test_name, email)
VALUES
    ('lkld1909', 'tester1@gmail.com'),
    ('2cpk', 'tester2@gmail.com'),
    ('waterboy', 'tester3@gmail.com'),
    ('kbkaiser', 'tester4@gmail.com');