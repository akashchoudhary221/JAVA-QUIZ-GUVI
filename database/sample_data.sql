-- =====================================================================
-- Java-Based Online Quiz Platform - sample data
--
-- Run AFTER database/schema.sql on a FRESH database:
--   mysql -u root -p < database/sample_data.sql
--
-- Default passwords are SHA-256 hashes of:
--   admin123 / creator123 / student123
-- =====================================================================
USE java_quiz_platform;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE feedback;
TRUNCATE TABLE results;
TRUNCATE TABLE answers;
TRUNCATE TABLE quiz_attempts;
TRUNCATE TABLE options;
TRUNCATE TABLE questions;
TRUNCATE TABLE quizzes;
TRUNCATE TABLE system_settings;
TRUNCATE TABLE users;
TRUNCATE TABLE roles;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------- roles
INSERT INTO roles (role_id, role_name) VALUES
(1, 'ADMIN'),
(2, 'QUIZ_CREATOR'),
(3, 'PARTICIPANT');

-- ---------------------------------------------------------------- users
INSERT INTO users (user_id, name, email, password, role) VALUES
(1, 'System Admin',  'admin@quiz.com',   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN'),
(2, 'Quiz Creator',  'creator@quiz.com', '756bf2effa2464565bab831d15a6a12d0fb1e7701294480c379b17f9c1064d0d', 'QUIZ_CREATOR'),
(3, 'Demo Student',  'student@quiz.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'PARTICIPANT');

-- ------------------------------------------------------- system settings
INSERT INTO system_settings (setting_id, setting_key, setting_value, description) VALUES
(1, 'default_duration_minutes', '10',  'Default quiz duration in minutes'),
(2, 'timer_enabled',             'true','Countdown timer is enabled for quizzes'),
(3, 'pass_percentage',           '60',  'Minimum percentage considered as a pass'),
(4, 'platform_name',             'Java-Based Online Quiz Platform', 'Name shown on the platform');

-- --------------------------------------------------------------- quizzes
INSERT INTO quizzes (quiz_id, title, description, duration_minutes, total_marks, status, created_by, created_at) VALUES
(1, 'Core Java & OOP Fundamentals',
   'Test your knowledge of classes, inheritance, polymorphism, interfaces and exception handling.',
   10, 10, 'APPROVED', 2, '2026-04-01 09:00:00'),
(2, 'Collections, Generics & Multithreading',
   'Lists, sets, maps, generic type safety, threads, sleep/interrupt and synchronization.',
   15, 10, 'APPROVED', 2, '2026-04-02 09:00:00'),
(3, 'JDBC, SQL & Servlet Web Development',
   'JDBC connections, PreparedStatement, transactions and Java Servlet basics.',
   12, 10, 'APPROVED', 2, '2026-04-03 09:00:00'),
(4, 'Java 17 Quick Test (Pending Approval)',
   'Short quiz about modern Java features - used to demonstrate the admin approval workflow.',
   5, 3, 'PENDING_APPROVAL', 2, '2026-04-04 09:00:00');

-- ------------------------------------------------------------ questions
INSERT INTO questions (question_id, quiz_id, question_text, marks, explanation) VALUES
(1, 1, 'Which keyword is used in Java to inherit a class?', 1, 'A subclass uses extends to inherit another class.'),
(2, 1, 'Which of the following is NOT a Java access modifier?', 1, 'Only public private and protected are modifiers.'),
(3, 1, 'Method overriding occurs when a subclass provides an implementation of a method that is:', 1, 'Overriding redefines a method that exists in the superclass.'),
(4, 1, 'Which of the following is true about an abstract class?', 1, 'An abstract class may have abstract methods and cannot be instantiated.'),
(5, 1, 'Which exception is thrown when an int is divided by zero?', 1, 'Integer division by zero raises ArithmeticException.'),
(6, 1, 'What is the implicit parent class of every class in Java?', 1, 'java.lang.Object is the root of the class hierarchy.'),
(7, 1, 'Which keyword is used to declare an interface?', 1, 'The interface keyword declares an interface.'),
(8, 1, 'Polymorphism in Java is achieved mainly through:', 1, 'Overloading at compile time and overriding at runtime.'),
(9, 1, 'Which block is guaranteed to run even if an exception is thrown?', 1, 'finally always executes unless the JVM exits.'),
(10, 1, 'Which of these is a checked exception?', 1, 'IOException is checked while RuntimeException types are unchecked.'),
(11, 2, 'Which class implements a resizable array-backed list?', 1, 'ArrayList is backed by a dynamic array.'),
(12, 2, 'Which collection does not allow duplicate elements?', 1, 'Set implementations such as HashSet reject duplicates.'),
(13, 2, 'Map in Java stores data as:', 1, 'A Map maps keys to values.'),
(14, 2, 'What does the generic declaration List of Integer enforce at compile time?', 1, 'Only Integer objects may be added - type safety without casts.'),
(15, 2, 'Which method starts a thread (schedules run on a new thread)?', 1, 'start creates a new thread while run stays on the current thread.'),
(16, 2, 'What does Thread sleep of 1000 do?', 1, 'It pauses the current thread for roughly one second.'),
(17, 2, 'Which keyword prevents two threads from entering a critical section at the same time?', 1, 'synchronized provides mutual exclusion.'),
(18, 2, 'Which collection class is thread-safe (synchronized) in Java?', 1, 'Hashtable is synchronized; ArrayList and HashMap are not.'),
(19, 2, 'A Callable task typically:', 1, 'Callable call may return a value and throw checked exceptions.'),
(20, 2, 'What happens when a sleeping thread is interrupted?', 1, 'InterruptedException is thrown and the sleep ends.'),
(21, 3, 'Which package contains the core JDBC API?', 1, 'java.sql contains Connection PreparedStatement and ResultSet.'),
(22, 3, 'Which JDBC object should be used to execute parameterised SQL?', 1, 'PreparedStatement prevents SQL injection through parameters.'),
(23, 3, 'What does ResultSet next do?', 1, 'It moves the cursor to the next row and returns false at the end.'),
(24, 3, 'Which call starts a transaction in JDBC?', 1, 'setAutoCommit false starts manual transaction control.'),
(25, 3, 'What does Connection rollback do?', 1, 'It undoes all changes made since the last commit.'),
(26, 3, 'Which servlet method handles an HTTP GET request?', 1, 'HttpServlet doGet processes GET requests.'),
(27, 3, 'HttpSession is used to:', 1, 'It stores per-user data across requests on the server.'),
(28, 3, 'Which annotation maps a servlet to a URL pattern?', 1, 'WebServlet annotation maps the servlet to a URL.'),
(29, 3, 'The MySQL JDBC driver class is a:', 1, 'Type-4 driver: pure Java speaking the database network protocol.'),
(30, 3, 'Which SQL clause filters rows before grouping?', 1, 'WHERE filters rows while HAVING filters groups.'),
(31, 4, 'The var keyword (Java 10+) is used for:', 1, 'Local variable type inference.'),
(32, 4, 'Java text blocks are delimited with:', 1, 'Triple double quotes.'),
(33, 4, 'Which symbol separates case labels in a switch expression?', 1, 'Java 14 switch expressions use the arrow form.');

-- ------------------------------------------------- options (choices A-D)
INSERT INTO options (option_id, question_id, option_text, option_order, is_correct) VALUES
(1, 1, 'implements', 1, 0), (2, 1, 'extends', 2, 1), (3, 1, 'inherits', 3, 0), (4, 1, 'super', 4, 0),
(5, 2, 'public', 1, 0), (6, 2, 'private', 2, 0), (7, 2, 'protected', 3, 0), (8, 2, 'subclass', 4, 1),
(9, 3, 'declared in the same class with different parameters', 1, 0), (10, 3, 'already defined in its superclass', 2, 1), (11, 3, 'declared static', 3, 0), (12, 3, 'declared private', 4, 0),
(13, 4, 'It can be instantiated directly', 1, 0), (14, 4, 'It can contain abstract methods', 2, 1), (15, 4, 'It cannot have constructors', 3, 0), (16, 4, 'It must contain only abstract methods', 4, 0),
(17, 5, 'NullPointerException', 1, 0), (18, 5, 'ArithmeticException', 2, 1), (19, 5, 'NumberFormatException', 3, 0), (20, 5, 'ArrayIndexOutOfBoundsException', 4, 0),
(21, 6, 'Object', 1, 1), (22, 6, 'Class', 2, 0), (23, 6, 'Super', 3, 0), (24, 6, 'Main', 4, 0),
(25, 7, 'class', 1, 0), (26, 7, 'interface', 2, 1), (27, 7, 'abstract', 3, 0), (28, 7, 'implements', 4, 0),
(29, 8, 'method overloading and overriding', 1, 1), (30, 8, 'final methods', 2, 0), (31, 8, 'static methods', 3, 0), (32, 8, 'private constructors', 4, 0),
(33, 9, 'try', 1, 0), (34, 9, 'catch', 2, 0), (35, 9, 'finally', 3, 1), (36, 9, 'throw', 4, 0),
(37, 10, 'RuntimeException', 1, 0), (38, 10, 'NullPointerException', 2, 0), (39, 10, 'IOException', 3, 1), (40, 10, 'ArithmeticException', 4, 0),
(41, 11, 'ArrayList', 1, 1), (42, 11, 'HashMap', 2, 0), (43, 11, 'HashSet', 3, 0), (44, 11, 'TreeSet', 4, 0),
(45, 12, 'ArrayList', 1, 0), (46, 12, 'HashSet', 2, 1), (47, 12, 'LinkedList', 3, 0), (48, 12, 'ArrayDeque', 4, 0),
(49, 13, 'key-value pairs', 1, 1), (50, 13, 'only values', 2, 0), (51, 13, 'queues', 3, 0), (52, 13, 'stacks', 4, 0),
(53, 14, 'Only Integer objects can be stored', 1, 1), (54, 14, 'Any object can be stored', 2, 0), (55, 14, 'Type checking happens only at runtime', 3, 0), (56, 14, 'Autoboxing is disabled', 4, 0),
(57, 15, 'run()', 1, 0), (58, 15, 'start()', 2, 1), (59, 15, 'execute()', 3, 0), (60, 15, 'begin()', 4, 0),
(61, 16, 'It terminates the thread', 1, 0), (62, 16, 'It pauses execution for about one second', 2, 1), (63, 16, 'It waits for keyboard input', 3, 0), (64, 16, 'It restarts the thread', 4, 0),
(65, 17, 'volatile', 1, 0), (66, 17, 'synchronized', 2, 1), (67, 17, 'static', 3, 0), (68, 17, 'final', 4, 0);

INSERT INTO options (option_id, question_id, option_text, option_order, is_correct) VALUES
(69, 18, 'ArrayList', 1, 0), (70, 18, 'HashMap', 2, 0), (71, 18, 'Hashtable', 3, 1), (72, 18, 'HashSet', 4, 0),
(73, 19, 'It returns a value and may throw checked exceptions', 1, 1), (74, 19, 'It can never throw an exception', 2, 0), (75, 19, 'It must be a static method', 3, 0), (76, 19, 'It is an enum type', 4, 0),
(77, 20, 'InterruptedException is thrown', 1, 1), (78, 20, 'Nothing happens', 2, 0), (79, 20, 'The thread restarts automatically', 3, 0), (80, 20, 'A compilation error occurs', 4, 0),
(81, 21, 'java.sql', 1, 1), (82, 21, 'java.jdbc', 2, 0), (83, 21, 'java.db', 3, 0), (84, 21, 'java.mysql', 4, 0),
(85, 22, 'Statement', 1, 0), (86, 22, 'PreparedStatement', 2, 1), (87, 22, 'ResultSet', 3, 0), (88, 22, 'DriverManager', 4, 0),
(89, 23, 'It closes the connection', 1, 0), (90, 23, 'It moves the cursor to the next row', 2, 1), (91, 23, 'It commits the transaction', 3, 0), (92, 23, 'It creates a table', 4, 0),
(93, 24, 'conn.commit()', 1, 0), (94, 24, 'conn.setAutoCommit(false)', 2, 1), (95, 24, 'conn.rollback()', 3, 0), (96, 24, 'conn.begin()', 4, 0),
(97, 25, 'It saves the changes permanently', 1, 0), (98, 25, 'It undoes changes since the last commit', 2, 1), (99, 25, 'It closes the ResultSet', 3, 0), (100, 25, 'It starts a new thread', 4, 0),
(101, 26, 'doPost()', 1, 0), (102, 26, 'doGet()', 2, 1), (103, 26, 'doDelete()', 3, 0), (104, 26, 'destroy()', 4, 0),
(105, 27, 'It stores data per user session on the server', 1, 1), (106, 27, 'It compiles Java code', 2, 0), (107, 27, 'It creates new threads', 3, 0), (108, 27, 'It manages heap memory', 4, 0),
(109, 28, '@Route', 1, 0), (110, 28, '@WebServlet', 2, 1), (111, 28, '@Controller', 3, 0), (112, 28, '@UrlPath', 4, 0),
(113, 29, 'Type-1 (JDBC-ODBC bridge)', 1, 0), (114, 29, 'Type-4 (100% pure Java socket)', 2, 1), (115, 29, 'Type-2 (native API)', 3, 0), (116, 29, 'Type-3 (middleware)', 4, 0),
(117, 30, 'ORDER BY', 1, 0), (118, 30, 'GROUP BY', 2, 0), (119, 30, 'WHERE', 3, 1), (120, 30, 'HAVING', 4, 0),
(121, 31, 'Local variable type inference', 1, 1), (122, 31, 'A new collection class', 2, 0), (123, 31, 'A JVM garbage collector flag', 3, 0), (124, 31, 'A build tool', 4, 0),
(125, 32, 'Triple double quotes (""")', 1, 1), (126, 32, 'Backticks', 2, 0), (127, 32, 'Triple percent signs', 3, 0), (128, 32, 'Hash markers', 4, 0),
(129, 33, '->', 1, 1), (130, 33, '=>', 2, 0), (131, 33, '::', 3, 0), (132, 33, '--', 4, 0);

-- --------------------------------------------------- sample attempts
INSERT INTO quiz_attempts (attempt_id, quiz_id, participant_id, started_at, submitted_at, time_taken_seconds, status) VALUES
(1, 1, 3, '2026-04-10 10:00:00', '2026-04-10 10:05:00', 300, 'COMPLETED'),
(2, 2, 3, '2026-04-12 14:00:00', '2026-04-12 14:07:00', 420, 'COMPLETED');

-- answers of attempt 1 (quiz 1): questions 1-8 correct, 9-10 wrong
INSERT INTO answers (answer_id, attempt_id, question_id, selected_option_id) VALUES
(1, 1, 1, 2), (2, 1, 2, 8), (3, 1, 3, 10), (4, 1, 4, 14), (5, 1, 5, 18),
(6, 1, 6, 21), (7, 1, 7, 26), (8, 1, 8, 29), (9, 1, 9, 33), (10, 1, 10, 37);

-- answers of attempt 2 (quiz 2): questions 11-15 correct, 16-20 wrong
INSERT INTO answers (answer_id, attempt_id, question_id, selected_option_id) VALUES
(11, 2, 11, 41), (12, 2, 12, 46), (13, 2, 13, 49), (14, 2, 14, 53), (15, 2, 15, 58),
(16, 2, 16, 61), (17, 2, 17, 65), (18, 2, 18, 69), (19, 2, 19, 74), (20, 2, 20, 78);

-- ------------------------------------------------------------ results
INSERT INTO results (result_id, attempt_id, quiz_id, participant_id, total_questions,
                     attempted_questions, correct_answers, incorrect_answers, total_marks,
                     obtained_marks, percentage, time_taken_seconds, feedback, submitted_at) VALUES
(1, 1, 1, 3, 10, 10, 8, 2, 10, 8, 80.00, 300,
 'Excellent! You have a strong command of this topic (8/10 correct).', '2026-04-10 10:05:00'),
(2, 2, 2, 3, 10, 10, 5, 5, 10, 5, 50.00, 420,
 'Average performance. Focus on the concepts behind the incorrect answers (5/10 correct).', '2026-04-12 14:07:00');

-- ----------------------------------------------------------- feedback
INSERT INTO feedback (feedback_id, result_id, quiz_id, participant_id, commented_by, comment_text, created_at) VALUES
(1, 1, 1, 3, 2,
 'Very good attempt! Revise the finally block and the checked/unchecked exception question.',
 '2026-04-11 09:00:00');


