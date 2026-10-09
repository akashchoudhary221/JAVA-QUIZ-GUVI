package com.quizplatform.model;

import java.time.LocalDateTime;

/**
 * Calculated result of one quiz attempt (marks, percentage, feedback).
 */
public class Result {

    private int id;
    private int attemptId;
    private int quizId;
    private String quizTitle;
    private int participantId;
    private String participantName;

    private int totalQuestions;
    private int attemptedQuestions;
    private int correctAnswers;
    private int incorrectAnswers;

    private int totalMarks;
    private int obtainedMarks;
    private double percentage;
    private long timeTakenSeconds;

    private String feedback;
    private LocalDateTime submittedAt;

    public Result() {
        this.submittedAt = LocalDateTime.now();
    }

    // ---- getters / setters ----
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getAttemptedQuestions() {
        return attemptedQuestions;
    }

    public void setAttemptedQuestions(int attemptedQuestions) {
        this.attemptedQuestions = attemptedQuestions;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public void setIncorrectAnswers(int incorrectAnswers) {
        this.incorrectAnswers = incorrectAnswers;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(int obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public long getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setTimeTakenSeconds(long timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    /** "MM:SS" representation of the time spent on the quiz. */
    public String getFormattedTimeTaken() {
        long minutes = timeTakenSeconds / 60;
        long seconds = timeTakenSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public boolean isPassed() {
        return percentage >= 60.0;
    }

    @Override
    public String toString() {
        return "Result{id=" + id + ", quiz='" + quizTitle + "', score=" + obtainedMarks
                + "/" + totalMarks + ", percentage=" + percentage + "%}";
    }
}
