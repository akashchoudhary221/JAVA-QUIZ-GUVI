package com.quizplatform.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One attempt of a participant on a quiz (with all selected answers).
 * Demonstrates Java Collections: List&lt;Answer&gt;.
 */
public class QuizAttempt {

    private int id;
    private int quizId;
    private String quizTitle;
    private int participantId;
    private String participantName;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private long timeTakenSeconds;
    private AttemptStatus status;

    // Demonstrates Java Collections
    private final List<Answer> answers = new ArrayList<>();

    public QuizAttempt() {
        this.startedAt = LocalDateTime.now();
        this.status = AttemptStatus.COMPLETED;
    }

    public QuizAttempt(int quizId, int participantId) {
        this();
        this.quizId = quizId;
        this.participantId = participantId;
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public long getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setTimeTakenSeconds(long timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers.clear();
        if (answers != null) {
            this.answers.addAll(answers);
        }
    }
}
