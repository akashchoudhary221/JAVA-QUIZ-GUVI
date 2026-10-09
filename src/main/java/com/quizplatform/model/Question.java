package com.quizplatform.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One multiple-choice question inside a quiz.
 * Demonstrates Java Collections: List&lt;Option&gt; holds the choices A-D.
 */
public class Question {

    private int id;
    private int quizId;
    private String text;
    private int marks;
    private String explanation;

    // Demonstrates Java Collections
    private final List<Option> options = new ArrayList<>();

    public Question() {
        this.marks = 1;
    }

    public Question(String text, int marks, String explanation) {
        this();
        this.text = text;
        this.marks = marks;
        this.explanation = explanation;
    }

    public void addOption(Option option) {
        options.add(option);
    }

    /** Returns the option marked as correct, or null when none is marked. */
    public Option getCorrectOption() {
        for (Option option : options) {
            if (option.isCorrect()) {
                return option;
            }
        }
        return null;
    }

    /** Convenience: id of the correct option (-1 when not available). */
    public long getCorrectOptionId() {
        Option correct = getCorrectOption();
        return correct == null ? -1 : correct.getId();
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options.clear();
        if (options != null) {
            this.options.addAll(options);
        }
    }

    @Override
    public String toString() {
        return "Question{id=" + id + ", text='" + text + "', marks=" + marks + "}";
    }
}
