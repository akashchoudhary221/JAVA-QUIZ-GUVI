package com.quizplatform.service;

import com.quizplatform.dao.SystemSettingsDAO;
import com.quizplatform.dao.impl.SystemSettingsDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.SystemSetting;
import com.quizplatform.util.ValidationUtil;

import java.util.List;

/**
 * Manages platform configuration (quiz defaults, timer settings).
 */
public class SystemSettingsService {

    public static final String KEY_DEFAULT_DURATION = "default_duration_minutes";
    public static final String KEY_TIMER_ENABLED = "timer_enabled";
    public static final String KEY_PASS_PERCENTAGE = "pass_percentage";
    public static final String KEY_PLATFORM_NAME = "platform_name";

    private final SystemSettingsDAO settingsDAO;

    public SystemSettingsService() {
        this(new SystemSettingsDAOImpl());
    }

    public SystemSettingsService(SystemSettingsDAO settingsDAO) {
        this.settingsDAO = settingsDAO;
    }

    public int getDefaultDurationMinutes() throws DatabaseException {
        String value = settingsDAO.getSettingValue(KEY_DEFAULT_DURATION, "10");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // fall back to a safe default instead of crashing the GUI
            return 10;
        }
    }

    public String getValue(String key, String defaultValue) throws DatabaseException {
        return settingsDAO.getSettingValue(key, defaultValue);
    }

    public List<SystemSetting> getAllSettings() throws DatabaseException {
        return settingsDAO.getAllSettings();
    }

    /** Used by the admin System Settings panel. */
    public void updateSetting(String key, String value) throws DatabaseException {
        if (ValidationUtil.isBlank(key) || ValidationUtil.isBlank(value)) {
            throw new DatabaseException("Setting key and value must not be empty.");
        }
        settingsDAO.saveSetting(key.trim(), value.trim());
    }
}
