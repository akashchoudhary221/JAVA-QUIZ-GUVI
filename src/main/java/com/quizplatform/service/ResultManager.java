package com.quizplatform.service;

import com.quizplatform.model.Result;
import com.quizplatform.repository.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Holds the shared result/statistics state of the platform.
 *
 * WHY SYNCHRONIZATION? Quiz submissions do NOT arrive from a single
 * thread: a normal submission runs on the Swing Event Dispatch Thread,
 * while the AUTO submission (timer reached 00:00) is triggered by the
 * QuizTimer background thread. Both call saveResult() and both read/
 * modify the same fields (totalSubmissions, percentageSum, best...).
 * Without synchronized, two threads could interleave and update the
 * statistics inconsistently (lost update).
 *
 * Demonstrates SYNCHRONIZATION (thread safety of shared mutable state).
 */
public final class ResultManager {

    private static final ResultManager INSTANCE = new ResultManager();

    // Demonstrates Generics - one repository instance typed with Result
    private final Repository<Result> resultCache = new Repository<>();

    private int totalSubmissions;
    private double percentageSum;
    private double bestPercentage;

    private ResultManager() {
        // singleton - everybody shares this one statistics object
    }

    public static ResultManager getInstance() {
        return INSTANCE;
    }

    /**
     * Stores a freshly calculated result and updates the shared statistics.
     * Called after the database TRANSACTION has been committed.
     * Demonstrates SYNCHRONIZED METHOD on shared state.
     */
    public synchronized void saveResult(Result result) {
        if (result == null) {
            throw new IllegalArgumentException("Result must not be null");
        }
        resultCache.add(result.getId(), result);
        totalSubmissions++;
        percentageSum += result.getPercentage();
        if (result.getPercentage() > bestPercentage) {
            bestPercentage = result.getPercentage();
        }
    }

    public synchronized double getAveragePercentage() {
        return totalSubmissions == 0 ? 0.0 : Math.round((percentageSum / totalSubmissions) * 100.0) / 100.0;
    }

    public synchronized int getTotalSubmissions() {
        return totalSubmissions;
    }

    public synchronized double getBestPercentage() {
        return bestPercentage;
    }

    /** Most recent results first - used by dashboards. */
    public synchronized List<Result> getRecentResults(int limit) {
        List<Result> all = new ArrayList<>(resultCache.findAll());
        all.sort(Comparator.comparing(Result::getId).reversed());
        return all.size() <= limit ? all : all.subList(0, limit);
    }
}
