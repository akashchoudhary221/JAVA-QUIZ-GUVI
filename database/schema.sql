-- =====================================================================
-- Java-Based Online Quiz Platform
-- MySQL schema  (database: java_quiz_platform)
--
-- Run with:  mysql -u root -p < database/schema.sql
-- Relationships:
--   users ----< quizzes (created_by)      quizzes ----< questions
--   questions ----< options               quizzes ----< quiz_attempts
--   users ----< quiz_attempts             quiz_attempts ----< answers
--   quiz_attempts ----(1:1)---- results   results ----< feedback
-- =====================================================================

CREATE DATABASE IF NOT EXISTS java_quiz_platform
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE java_quiz_platform;

-- child tables first so that no foreign key blocks the drop
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS results;
DROP TABLE IF EXISTS answers;
DROP TABLE IF EXISTS quiz_attempts;
DROP TABLE IF EXISTS options;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS quizzes;
DROP TABLE IF EXISTS system_settings;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

-- ---------------------------------------------------------------- roles
CREATE TABLE roles (
    role_id   INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------- users
CREATE TABLE users (
    user_id    INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(120) NOT NULL UNIQUE,
    password   VARCHAR(64)  NOT NULL,                 -- SHA-256 hash (never plain text)
    role       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role FOREIGN KEY (role) REFERENCES roles (role_name),
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'QUIZ_CREATOR', 'PARTICIPANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------- quizzes
CREATE TABLE quizzes (
    quiz_id          INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200)  NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    duration_minutes INT           NOT NULL DEFAULT 10,
    total_marks      INT           NOT NULL DEFAULT 0,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_by       INT           NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quiz_creator FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_quiz_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'PUBLISHED')),
    CONSTRAINT chk_quiz_duration CHECK (duration_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------ questions
CREATE TABLE questions (
    question_id   INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id       INT           NOT NULL,
    question_text VARCHAR(1000) NOT NULL,
    marks         INT           NOT NULL DEFAULT 1,
    explanation   VARCHAR(500)  NULL,
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT chk_question_marks CHECK (marks > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------- options
CREATE TABLE options (
    option_id   INT AUTO_INCREMENT PRIMARY KEY,
    question_id INT           NOT NULL,
    option_text VARCHAR(500)  NOT NULL,
    option_order INT          NOT NULL,
    is_correct  BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------- quiz_attempts
CREATE TABLE quiz_attempts (
    attempt_id         INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id            INT       NOT NULL,
    participant_id     INT       NOT NULL,
    started_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at       TIMESTAMP NULL DEFAULT NULL,
    time_taken_seconds INT       NOT NULL DEFAULT 0,
    status             VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_user FOREIGN KEY (participant_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_attempt_status CHECK (status IN ('COMPLETED', 'AUTO_SUBMITTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------- answers
CREATE TABLE answers (
    answer_id         INT AUTO_INCREMENT PRIMARY KEY,
    attempt_id        INT NOT NULL,
    question_id       INT NOT NULL,
    selected_option_id INT NULL,                      -- NULL = not answered
    CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_option FOREIGN KEY (selected_option_id) REFERENCES options (option_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------- results
CREATE TABLE results (
    result_id           INT AUTO_INCREMENT PRIMARY KEY,
    attempt_id          INT           NOT NULL UNIQUE,
    quiz_id             INT           NOT NULL,
    participant_id      INT           NOT NULL,
    total_questions     INT           NOT NULL,
    attempted_questions INT           NOT NULL,
    correct_answers     INT           NOT NULL,
    incorrect_answers   INT           NOT NULL,
    total_marks         INT           NOT NULL,
    obtained_marks      INT           NOT NULL,
    percentage          DECIMAL(5,2)  NOT NULL,
    time_taken_seconds  INT           NOT NULL,
    feedback            VARCHAR(1000) NULL,
    submitted_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_result_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_result_user FOREIGN KEY (participant_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------- feedback
CREATE TABLE feedback (
    feedback_id     INT AUTO_INCREMENT PRIMARY KEY,
    result_id       INT           NOT NULL,
    quiz_id         INT           NOT NULL,
    participant_id  INT           NOT NULL,
    commented_by    INT           NOT NULL,
    comment_text    VARCHAR(1000) NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_result FOREIGN KEY (result_id) REFERENCES results (result_id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_participant FOREIGN KEY (participant_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_author FOREIGN KEY (commented_by) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------- system_settings
CREATE TABLE system_settings (
    setting_id    INT AUTO_INCREMENT PRIMARY KEY,
    setting_key   VARCHAR(60)  NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description   VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

