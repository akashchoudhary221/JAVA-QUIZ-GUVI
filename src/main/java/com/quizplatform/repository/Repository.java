package com.quizplatform.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A small, reusable, generic in-memory repository.
 *
 * WHY GENERICS? One single class (Repository&lt;T&gt;) is reused for many
 * different entity types - there is no need to write (and maintain) a
 * separate UserList / QuizList / ResultList class, and the compiler
 * guarantees type safety (no casts, no possibility of inserting a
 * Quiz into a list of Users).
 *
 * Usage in this project:
 *   Repository&lt;Quiz&gt;   quizCache   (QuizService)
 *   Repository&lt;Result&gt; resultCache (ResultManager)
 *   Repository&lt;Question&gt; questionCache (QuizService)
 *
 * The methods are synchronized because the repository is shared between
 * the Swing Event Dispatch Thread and background timer threads.
 */
public class Repository<T> {

    private final Map<Integer, T> store = new LinkedHashMap<>();

    /** Inserts a new element under the given id. */
    public synchronized void add(int id, T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        store.put(id, item);
    }

    /** Replaces the element with the given id (only if it already exists). */
    public synchronized boolean update(int id, T item) {
        if (!store.containsKey(id) || item == null) {
            return false;
        }
        store.put(id, item);
        return true;
    }

    /** Removes the element with the given id. */
    public synchronized boolean delete(int id) {
        return store.remove(id) != null;
    }

    /** Finds one element by id - returns null when not found. */
    public synchronized T findById(int id) {
        return store.get(id);
    }

    /** Returns every element as a new List&lt;T&gt; (insertion order kept). */
    public synchronized List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Example of passing a generic Predicate&lt;T&gt; into a generic class. */
    public synchronized List<T> findMatching(Predicate<T> filter) {
        List<T> matches = new ArrayList<>();
        for (T item : store.values()) {
            if (filter.test(item)) {
                matches.add(item);
            }
        }
        return matches;
    }

    public synchronized boolean exists(int id) {
        return store.containsKey(id);
    }

    public synchronized int count() {
        return store.size();
    }

    public synchronized boolean isEmpty() {
        return store.isEmpty();
    }

    public synchronized void clear() {
        store.clear();
    }
}
