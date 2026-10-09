package com.quizplatform.dao;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.SystemSetting;

import java.util.List;

/**
 * DAO pattern: interface for the system_settings table
 * (quiz defaults, timer settings, platform configuration).
 * Implemented by com.quizplatform.dao.impl.SystemSettingsDAOImpl (JDBC).
 */
public interface SystemSettingsDAO {

    /** Returns the stored value or the given default when the key is unknown. */
    String getSettingValue(String key, String defaultValue) throws DatabaseException;

    SystemSetting getSettingByKey(String key) throws DatabaseException;

    List<SystemSetting> getAllSettings() throws DatabaseException;

    /** Inserts the key when it does not exist yet, otherwise updates the value. */
    void saveSetting(String key, String value) throws DatabaseException;

    void updateSetting(SystemSetting setting) throws DatabaseException;
}
