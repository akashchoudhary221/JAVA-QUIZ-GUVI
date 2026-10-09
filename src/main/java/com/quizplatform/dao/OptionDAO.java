package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Option;

import java.util.List;

/**
 * DAO pattern: interface for all answer-option table operations.
 * Implemented by com.quizplatform.dao.impl.OptionDAOImpl (JDBC).
 */
public interface OptionDAO {

    int addOption(Option option) throws DatabaseException;

    /** All options of one question, ordered A, B, C, D. */
    List<Option> getOptionsByQuestion(int questionId) throws DatabaseException;

    void deleteOptionsByQuestion(int questionId) throws DatabaseException;
}
