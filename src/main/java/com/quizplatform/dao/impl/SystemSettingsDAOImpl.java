package com.quizplatform.dao.impl;

import com.quizplatform.dao.SystemSettingsDAO;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.SystemSetting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of SystemSettingsDAO
 * (quiz defaults, timer settings, platform configuration).
 */
public class SystemSettingsDAOImpl implements SystemSettingsDAO {

    private static final String BASE_SELECT =
            "SELECT setting_id, setting_key, setting_value, description FROM system_settings";

    @Override
    public String getSettingValue(String key, String defaultValue) throws DatabaseException {
        SystemSetting setting = getSettingByKey(key);
        return setting == null ? defaultValue : setting.getValue();
    }

    @Override
    public SystemSetting getSettingByKey(String key) throws DatabaseException {
        String sql = BASE_SELECT + " WHERE setting_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SystemSetting setting = new SystemSetting();
                    setting.setId(rs.getInt("setting_id"));
                    setting.setKey(rs.getString("setting_key"));
                    setting.setValue(rs.getString("setting_value"));
                    setting.setDescription(rs.getString("description"));
                    return setting;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load setting '" + key + "': " + e.getMessage(), e);
        }
    }

    @Override
    public List<SystemSetting> getAllSettings() throws DatabaseException {
        String sql = BASE_SELECT + " ORDER BY setting_key";
        List<SystemSetting> settings = new ArrayList<>();     // Demonstrates Java Collections
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SystemSetting setting = new SystemSetting();
                setting.setId(rs.getInt("setting_id"));
                setting.setKey(rs.getString("setting_key"));
                setting.setValue(rs.getString("setting_value"));
                setting.setDescription(rs.getString("description"));
                settings.add(setting);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load settings: " + e.getMessage(), e);
        }
        return settings;
    }

    /** INSERT when the key is new, UPDATE when it already exists (upsert). */
    @Override
    public void saveSetting(String key, String value) throws DatabaseException {
        if (getSettingByKey(key) == null) {
            String sql = "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to create setting '" + key + "': " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, value);
                ps.setString(2, key);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update setting '" + key + "': " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void updateSetting(SystemSetting setting) throws DatabaseException {
        String sql = "UPDATE system_settings SET setting_value = ?, description = ? WHERE setting_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, setting.getValue());
            ps.setString(2, setting.getDescription());
            ps.setString(3, setting.getKey());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Setting '" + setting.getKey() + "' does not exist.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update setting: " + e.getMessage(), e);
        }
    }
}
