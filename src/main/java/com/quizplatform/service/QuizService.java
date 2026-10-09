package com.quizplatform.service;

import com.quizplatform.dao.OptionDAO;
import com.quizplatform.dao.QuestionDAO;
import com.quizplatform.dao.QuizDAO;
import com.quizplatform.dao.impl.OptionDAOImpl;
import com.quizplatform.dao.impl.QuestionDAOImpl;
import com.quizplatform.dao.impl.QuizDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.QuizNotFoundException;
import com.quizplatform.interfaces.QuizOperations;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.repository.Repository;
import com.quizplatform.util.ValidationUtil;

import java.util.List;

/**
 * Business logic of the quiz lifecycle:
 * create -> add questions -> submit for approval -> admin approves/rejects.
 *
 * Implements the QuizOperations interface.
 * Uses a generic Repository&lt;Quiz&gt; as an in-memory cache
 * (Demonstrates Generics + Java Collections).
 */
public class QuizService implements QuizOperations {

    private final QuizDAO quizDAO;
    private final QuestionDAO questionDAO;
    private final OptionDAO optionDAO;

    // Demonstrates Generics: Repository<Quiz> cache, shared by all methods
    private final Repository<Quiz> quizCache = new Repository<>();
    // Demonstrates Generics: Repository<Question> holding the loaded questions
    private final Repository<Question> questionCache = new Repository<>();

    public QuizService() {
        this(new QuizDAOImpl(), new QuestionDAOImpl(), new OptionDAOImpl());
    }

    public QuizService(QuizDAO quizDAO, QuestionDAO questionDAO, OptionDAO optionDAO) {
        this.quizDAO = quizDAO;
        this.questionDAO = questionDAO;
        this.optionDAO = optionDAO;
    }

    // Demonstrates INTERFACE IMPLEMENTATION
    @Override
    public Quiz createQuiz(Quiz quiz) throws InvalidQuizException, DatabaseException {
        validateQuizHeader(quiz);
        quiz.setStatus(QuizStatus.DRAFT);
        if (quiz.getTotalMarks() <= 0) {
            quiz.setTotalMarks(0);   // computed later when questions are added
        }
        quizDAO.createQuiz(quiz);
        quizCache.add(quiz.getId(), quiz);
        return quiz;
    }

    @Override
    public Quiz updateQuiz(Quiz quiz) throws InvalidQuizException, DatabaseException {
        if (quiz == null || quiz.getId() <= 0) {
            throw new InvalidQuizException("Please select a quiz to update.");
        }
        validateQuizHeader(quiz);
        quizDAO.updateQuiz(quiz);
        quizCache.update(quiz.getId(), quiz);
        return quiz;
    }

    @Override
    public void deleteQuiz(int quizId) throws DatabaseException {
        questionDAO.deleteQuestionsByQuiz(quizId);   // child rows first (foreign keys)
        quizDAO.deleteQuiz(quizId);
        quizCache.delete(quizId);
    }

    /** Validates title / description / duration - throws a custom exception. */
    private void validateQuizHeader(Quiz quiz) throws InvalidQuizException {
        if (quiz == null) {
            throw new InvalidQuizException("Quiz data must not be empty.");
        }
        if (ValidationUtil.isBlank(quiz.getTitle())) {
            throw new InvalidQuizException("Quiz title is required.");
        }
        if (quiz.getTitle().trim().length() < 3) {
            throw new InvalidQuizException("Quiz title must contain at least 3 characters.");
        }
        if (ValidationUtil.isBlank(quiz.getDescription())) {
            throw new InvalidQuizException("Quiz description is required.");
        }
        // Demonstrates custom exception handling: invalid duration
        if (!ValidationUtil.isValidDuration(quiz.getDurationMinutes())) {
            throw new InvalidQuizException("Invalid quiz duration: " + quiz.getDurationMinutes()
                    + " minutes (allowed: 1 - 480).");
        }
    }

    /**
     * Adds a complete question (text + options + correct answer + marks).
     * Every validation failure raises InvalidQuizException with a clear message.
     */
    public Question addQuestion(Question question) throws InvalidQuizException, DatabaseException {
        validateQuestion(question);
        questionDAO.addQuestion(question);
        saveOptions(question);
        refreshTotalMarks(question.getQuizId());
        questionCache.add(question.getId(), question);
        return question;
    }

    /** Replaces text/marks/options of an existing question. */
    public void updateQuestion(Question question) throws InvalidQuizException, DatabaseException {
        if (question == null || question.getId() <= 0) {
            throw new InvalidQuizException("Please select a question to update.");
        }
        validateQuestion(question);
        questionDAO.updateQuestion(question);
        // replace all options (simple and safe)
        optionDAO.deleteOptionsByQuestion(question.getId());
        saveOptions(question);
        refreshTotalMarks(question.getQuizId());
        questionCache.update(question.getId(), question);
    }

    public void deleteQuestion(int questionId, int quizId) throws DatabaseException {
        questionDAO.deleteQuestion(questionId);
        questionCache.delete(questionId);
        refreshTotalMarks(quizId);
    }

    private void saveOptions(Question question) throws DatabaseException {
        int order = 1;
        for (Option option : question.getOptions()) {
            option.setQuestionId(question.getId());
            option.setOptionOrder(order++);
            optionDAO.addOption(option);
        }
    }

    /** Validates question text / marks / options / correct answer. */
    private void validateQuestion(Question question) throws InvalidQuizException {
        if (question == null) {
            throw new InvalidQuizException("Question data must not be empty.");
        }
        if (ValidationUtil.isBlank(question.getText())) {
            throw new InvalidQuizException("Question text is required.");
        }
        if (!ValidationUtil.isValidMarks(question.getMarks())) {
            throw new InvalidQuizException("Question marks must be between 1 and 100.");
        }
        List<Option> options = question.getOptions();
        if (options == null || options.size() < 2) {
            throw new InvalidQuizException("A question needs at least 2 answer options.");
        }
        for (Option option : options) {
            if (ValidationUtil.isBlank(option.getOptionText())) {
                throw new InvalidQuizException("Answer options must not be empty.");
            }
        }
        long correctCount = options.stream().filter(Option::isCorrect).count();
        if (correctCount != 1) {
            throw new InvalidQuizException("Exactly one option must be marked as the correct answer.");
        }
    }

    /**
     * Creator sends the quiz to the admin - only allowed with >= 1 question.
     * Demonstrates custom exception handling: empty quiz cannot be submitted.
     */
    public void submitForApproval(int quizId)
            throws InvalidQuizException, QuizNotFoundException, DatabaseException {
        Quiz quiz = getQuizByIdOrThrow(quizId);
        int questionCount = questionDAO.countQuestionsByQuiz(quizId);
        if (questionCount == 0) {
            throw new InvalidQuizException("Cannot submit: quiz '" + quiz.getTitle()
                    + "' has no questions. Add at least one question first.");
        }
        quiz.setStatus(QuizStatus.PENDING_APPROVAL);
        quizDAO.updateQuizStatus(quizId, QuizStatus.PENDING_APPROVAL.name());
        quizCache.update(quizId, quiz);
    }

    /** Admin approval - the quiz becomes visible for participants. */
    public void approveQuiz(int quizId) throws DatabaseException {
        quizDAO.updateQuizStatus(quizId, QuizStatus.APPROVED.name());
        refreshCache(quizId);
    }

    /** Admin rejection - the creator can edit and submit again. */
    public void rejectQuiz(int quizId) throws DatabaseException {
        quizDAO.updateQuizStatus(quizId, QuizStatus.REJECTED.name());
        refreshCache(quizId);
    }

    /** Recomputes quizzes.total_marks from all of its questions. */
    private void refreshTotalMarks(int quizId) throws DatabaseException {
        Quiz quiz = quizDAO.getQuizById(quizId);
        if (quiz == null) {
            return;
        }
        int total = questionDAO.getQuestionsByQuiz(quizId).stream().mapToInt(Question::getMarks).sum();
        quiz.setTotalMarks(total);
        quizDAO.updateQuiz(quiz);
        quizCache.update(quizId, quiz);
    }

    private void refreshCache(int quizId) {
        try {
            Quiz quiz = quizDAO.getQuizById(quizId);
            if (quiz != null) {
                quizCache.update(quizId, quiz);
            } else {
                quizCache.delete(quizId);
            }
        } catch (DatabaseException e) {
            System.err.println("[QuizService] cache refresh failed: " + e.getMessage());
        }
    }

    public Quiz getQuizByIdOrThrow(int quizId) throws QuizNotFoundException, DatabaseException {
        Quiz quiz = quizDAO.getQuizById(quizId);
        if (quiz == null) {
            // Demonstrates custom exception handling
            throw new QuizNotFoundException("Quiz #" + quizId + " was not found in the database.");
        }
        return quiz;
    }

    /** Loads all questions WITH their options - used when starting a quiz. */
    public List<Question> getQuestionsForQuiz(int quizId) throws DatabaseException {
        List<Question> questions = questionDAO.getQuestionsByQuiz(quizId);
        questionCache.clear();
        for (Question question : questions) {
            questionCache.add(question.getId(), question);
        }
        return questions;
    }

    /** Quiz header + questions + options in one object. */
    public Quiz getQuizWithQuestions(int quizId) throws QuizNotFoundException, DatabaseException {
        Quiz quiz = getQuizByIdOrThrow(quizId);
        quiz.setQuestions(getQuestionsForQuiz(quizId));
        return quiz;
    }

    public Quiz getQuizById(int quizId) throws DatabaseException {
        return quizDAO.getQuizById(quizId);
    }

    /** Demonstrates Repository<Quiz>: read-through cache lookup. */
    public Quiz getCachedQuiz(int quizId) {
        return quizCache.findById(quizId);
    }

    public List<Quiz> getAllQuizzes() throws DatabaseException {
        return refresh(quizDAO.getAllQuizzes());
    }

    /** Only APPROVED quizzes are offered to participants. */
    public List<Quiz> getApprovedQuizzes() throws DatabaseException {
        return refresh(quizDAO.getApprovedQuizzes());
    }

    public List<Quiz> getQuizzesByCreator(int creatorId) throws DatabaseException {
        return refresh(quizDAO.getQuizzesByCreator(creatorId));
    }

    public List<Quiz> getQuizzesByStatus(String status) throws DatabaseException {
        return refresh(quizDAO.getQuizzesByStatus(status));
    }

    private List<Quiz> refresh(List<Quiz> quizzes) {
        for (Quiz quiz : quizzes) {
            quizCache.add(quiz.getId(), quiz);
        }
        return quizzes;
    }

    public int countAllQuizzes() throws DatabaseException {
        return quizDAO.countAllQuizzes();
    }

    public int countQuizzesByStatus(String status) throws DatabaseException {
        return quizDAO.countQuizzesByStatus(status);
    }

    public int countQuestionsByQuiz(int quizId) throws DatabaseException {
        return questionDAO.countQuestionsByQuiz(quizId);
    }

    public int getQuestionCacheSize() {
        return questionCache.count();
    }
}