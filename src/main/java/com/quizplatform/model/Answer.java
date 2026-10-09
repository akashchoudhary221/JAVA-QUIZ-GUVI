package com.quizplatform.model;

/**
 * A single selected answer of an attempt.
 * selectedOptionId = -1 means the participant did not answer the question.
 */
public class Answer {

    private int id;
    private int attemptId;
    private int questionId;
    private long selectedOptionId;

    public Answer() {
        this.selectedOptionId = -1;
    }

    public Answer(int questionId, long selectedOptionId) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
    }

    public boolean isAttempted() {
        return selectedOptionId > 0;
    }

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

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public long getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(long selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }
}
