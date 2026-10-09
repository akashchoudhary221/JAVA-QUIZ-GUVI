package com.quizplatform;

import com.quizplatform.model.Quiz;
import com.quizplatform.model.Result;
import com.quizplatform.repository.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test of the GENERIC repository.
 * Demonstrates how one Repository&lt;T&gt; is reused for different entity types
 * (Repository&lt;Quiz&gt; and Repository&lt;Result&gt;) with compile time type safety.
 */
class RepositoryTest {

    @Test
    @DisplayName("Repository<Quiz> supports add / findById / update / delete / findAll")
    void repositoryOfQuizWorks() {
        Repository<Quiz> quizzes = new Repository<>();

        Quiz quiz1 = new Quiz("Quiz One", "desc", 10);
        quiz1.setId(1);
        Quiz quiz2 = new Quiz("Quiz Two", "desc", 5);
        quiz2.setId(2);

        quizzes.add(1, quiz1);
        quizzes.add(2, quiz2);

        assertEquals(2, quizzes.count());
        assertEquals("Quiz One", quizzes.findById(1).getTitle());
        assertTrue(quizzes.exists(2));

        quiz1.setTitle("Quiz One Updated");
        assertTrue(quizzes.update(1, quiz1));
        assertEquals("Quiz One Updated", quizzes.findById(1).getTitle());

        List<Quiz> matches = quizzes.findMatching(q -> q.getDurationMinutes() == 5);
        assertEquals(1, matches.size());
        assertEquals("Quiz Two", matches.get(0).getTitle());

        assertTrue(quizzes.delete(2));
        assertFalse(quizzes.delete(2));
        assertNull(quizzes.findById(2));
        assertEquals(1, quizzes.findAll().size());
    }

    @Test
    @DisplayName("The same generic class also works for Repository<Result>")
    void repositoryOfResultWorks() {
        Repository<Result> results = new Repository<>();
        Result result = new Result();
        result.setId(7);
        result.setPercentage(88.5);

        results.add(7, result);

        assertEquals(1, results.count());
        assertEquals(88.5, results.findById(7).getPercentage(), 0.001);

        results.clear();
        assertTrue(results.isEmpty());
    }
}
