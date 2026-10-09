package com.quizplatform.gui;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.SystemSetting;
import com.quizplatform.service.SystemSettingsService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Admin screen: platform configuration
 * (quiz defaults, timer settings, platform name, pass percentage).
 * Data lives in the system_settings table (SystemSettingsDAO).
 */
public class SystemSettingsPanel extends JPanel {

    private final SystemSettingsService settingsService = new SystemSettingsService();

    private final JTextField keyField = new JTextField(20);
    private final JTextField valueField = new JTextField(20);

    private final DefaultTableModel tableModel =
            UIUtil.emptyModel("Setting Key", "Value", "Description");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    public SystemSettingsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                keyField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                valueField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            }
        });
        refreshTable();
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(UIUtil.BACKGROUND);
        wrapper.add(UIUtil.heading("System Settings"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 6, 5, 6);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        form.add(UIUtil.fieldLabel("Setting Key:"), gc);
        gc.gridx = 1;
        form.add(keyField, gc);
        gc.gridx = 2;
        form.add(UIUtil.fieldLabel("Value:"), gc);
        gc.gridx = 3;
        form.add(valueField, gc);

        JLabel hint = new JLabel("<html><small>Keys: default_duration_minutes | timer_enabled | "
                + "pass_percentage | platform_name</small></html>");
        hint.setFont(UIUtil.smallFont());
        hint.setForeground(Color.GRAY);
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 4;
        form.add(hint, gc);

        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        panel.setBackground(UIUtil.BACKGROUND);
        JButton save = UIUtil.successButton("Save Setting");
        JButton clear = UIUtil.neutralButton("Clear");
        JButton refresh = UIUtil.primaryButton("Refresh");
        save.addActionListener(e -> saveSetting());
        clear.addActionListener(e -> {
            keyField.setText("");
            valueField.setText("");
        });
        refresh.addActionListener(e -> refreshTable());
        panel.add(save);
        panel.add(clear);
        panel.add(refresh);
        return panel;
    }

    private void saveSetting() {
        try {
            settingsService.updateSetting(keyField.getText(), valueField.getText());
            JOptionPane.showMessageDialog(this, "Setting saved.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        try {
            tableModel.setRowCount(0);
            for (SystemSetting setting : settingsService.getAllSettings()) {
                tableModel.addRow(new Object[]{setting.getKey(), setting.getValue(),
                        setting.getDescription() == null ? "" : setting.getDescription()});
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load settings:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
