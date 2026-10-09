package com.quizplatform.service;

import com.quizplatform.dao.AttemptDAO;
import com.quizplatform.dao.FeedbackDAO;
import com.quizplatform.dao.ResultDAO;
import com.quizplatform.dao.impl.AttemptDAOImpl;
import com.quizplatform.dao.impl.FeedbackDAOImpl;
import com.quizplatform.dao.impl.ResultDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.TimeExpiredException;
import com.quizplatform.model.Answer;
import com.quizplatform.model.AttemptStatus;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.Result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates a complete quiz submission:
 *   1. validate quiz + timer
 *   2. calculate the score (ScoreService)
 *   3. persist attempt + answers + result inside ONE transaction (AttemptDAO)
 *   4. update the shared statistics (synchronized ResultManager)
 */
public class ResultService {

    private final AttemptDAO attemptDAO;
    private final ResultDAO resultDAO;
    private final FeedbackDAO feedbackDAO;
    private final ScoreService scoreService;
    private final ResultManager resultManager;

    public ResultService() {
        this(new AttemptDAOImpl(), new ResultDAOImpl(), new FeedbackDAOImpl(),
                new ScoreService(), ResultManager.getInstance());
    }

    public ResultService(AttemptDAO attemptDAO, ResultDAO resultDAO, FeedbackDAO feedbackDAO,
                         ScoreService scoreService, ResultManager resultManager) {
        this.attemptDAO = attemptDAO;
        this.resultDAO = resultDAO;
        this.feedbackDAO = feedbackDAO;
        this.scoreService = scoreService;
        this.resultManager = resultManager;
    }

    /**
     * Stores a finished attempt.
     *
     * @param timedOut true when the QuizTimer thread auto-submitted at 00:00
     * @throws TimeExpiredException when the participant tries to continue after expiry
     * @throws InvalidQuizException when the quiz is empty / has no answers
     * @throws DatabaseException    when the transaction fails (it is rolled back)
     */
    public Result submitAttempt(Quiz quiz, List<Question> questions, int participantId,
                                String participantName, List<Answer> answers,
                                long timeTakenSeconds, boolean timedOut)
            throws TimeExpiredException, InvalidQuizException, DatabaseException {

        if (quiz == null) {
            throw new InvalidQuizException("No quiz selected for submission.");
        }
        if (questions == null || questions.isEmpty()) {
            throw new InvalidQuizException("This quiz has no questions and cannot be submitted.");
        }
        if (answers == null || answers.isEmpty()) {
            throw new InvalidQuizException("No answers were provided for this submission.");
        }
        // Demonstrates custom exception: timer expiration
        long allowedSeconds = quiz.getDurationMinutes() * 60L;
        if (timeTakenSeconds > allowedSeconds) {
            throw new TimeExpiredException("Time for quiz '" + quiz.getTitle() + "' has expired.", 0);
        }

        QuizAttempt attempt = new QuizAttempt(quiz.getId(), participantId);
        attempt.setQuizTitle(quiz.getTitle());
        attempt.setParticipantName(participantName);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setTimeTakenSeconds(timeTakenSeconds);
        attempt.setStatus(timedOut ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.COMPLETED);
        attempt.setAnswers(answers);

        Result result = scoreService.calculateScore(quiz, questions, attempt);

        // TRANSACTION: attempt + answers + result are committed together
        attemptDAO.submitAttempt(attempt, answers, result);

        // SYNCHRONIZED: shared statistics may be updated by the EDT and the timer thread
        resultManager.saveResult(result);
        return result;
    }

    public List<Result> getHistory(int participantId) throws DatabaseException {
        return resultDAO.getResultsByParticipant(participantId);
    }

    /** Loads the stored answers of one attempt (used by the history detail view). */
    public java.util.List<com.quizplatform.model.Answer> getAnswersByAttempt(int attemptId)
            throws DatabaseException {
        return attemptDAO.getAnswersByAttempt(attemptId);
    }

    public List<Result> getResultsForCreator(int creatorId) throws DatabaseException {
        return resultDAO.getResultsByCreator(creatorId);
    }

    public List<Result> getAllResults() throws DatabaseException {
        return resultDAO.getAllResults();
    }

    public Result getResultByAttempt(int attemptId) throws DatabaseException {
        return resultDAO.getResultByAttempt(attemptId);
    }

    /** Human readable report (also used by the Servlets). */
    public String generateReport(Result result) {
        return scoreService.generateReport(result);
    }

    /** Creator / admin gives written feedback on one result. */
    public void addFeedback(Feedback feedback) throws DatabaseException {
        feedbackDAO.addFeedback(feedback);
    }

    public List<Feedback> getFeedbackForResult(int resultId) throws DatabaseException {
        return feedbackDAO.getFeedbackByResult(resultId);
    }

    public List<Feedback> getFeedbackByAuthor(int authorId) throws DatabaseException {
        return feedbackDAO.getFeedbackByAuthor(authorId);
    }
}
