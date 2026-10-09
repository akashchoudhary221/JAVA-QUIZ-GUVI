package com.quizplatform.model;

/**
 * One answer option (choice) of a multiple-choice question.
 */
public class Option {

    private int id;
    private int questionId;
    private String optionText;
    private int optionOrder;
    private boolean correct;

    public Option() {
    }

    public Option(String optionText, int optionOrder, boolean correct) {
        this.optionText = optionText;
        this.optionOrder = optionOrder;
        this.correct = correct;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public int getOptionOrder() {
        return optionOrder;
    }

    public void setOptionOrder(int optionOrder) {
        this.optionOrder = optionOrder;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    /** Letter label A, B, C, D used by the GUI radio buttons. */
    public String getLetter() {
        return String.valueOf((char) ('A' + (optionOrder - 1)));
    }

    @Override
    public String toString() {
        return getLetter() + ") " + optionText + (correct ? " [correct]" : "");
    }
}
