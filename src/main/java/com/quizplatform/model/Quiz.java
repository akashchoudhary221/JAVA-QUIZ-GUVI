package com.quizplatform.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A quiz created by a Quiz Creator.
 *
 * Demonstrates Java Collections: questions are stored in a List<Question>.
 */
public class Quiz {

    private int id;
    private String title;
    private String description;
    private int durationMinutes;
    private int totalMarks;
    private QuizStatus status;
    private int createdBy;
    private String creatorName;
    private LocalDateTime createdAt;

    // Demonstrates Java Collections - generic List<Question>
    private final List<Question> questions = new ArrayList<>();

    public Quiz() {
        this.status = QuizStatus.DRAFT;
        this.durationMinutes = 10;
        this.createdAt = LocalDateTime.now();
    }

    public Quiz(String title, String description, int durationMinutes) {
        this();
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
    }

    // ---- helper methods that operate on the internal collection ----
    public void addQuestion(Question question) {
        questions.add(question);
        recalculateTotalMarks();
    }

    public boolean removeQuestion(int questionId) {
        boolean removed = questions.removeIf(q -> q.getId() == questionId);
        if (removed) {
            recalculateTotalMarks();
        }
        return removed;
    }

    /** Recomputes total marks from all questions (Collections + generics). */
    public void recalculateTotalMarks() {
        this.totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public void setQuestions(List<Question> questions) {
        this.questions.clear();
        if (questions != null) {
            this.questions.addAll(questions);
        }
        recalculateTotalMarks();
    }

    public boolean isApproved() {
        return status == QuizStatus.APPROVED || status == QuizStatus.PUBLISHED;
    }

    // ---- getters / setters ----
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public QuizStatus getStatus() {
        return status;
    }

    public void setStatus(QuizStatus status) {
        this.status = status;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Quiz{id=" + id + ", title='" + title + "', status=" + status
                + ", duration=" + durationMinutes + " min, marks=" + totalMarks + "}";
    }
}
