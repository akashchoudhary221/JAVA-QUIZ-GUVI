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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Admin screen: Platform configuration settings
 * (default duration, pass percentage, timer toggle, platform name).
 */
public class SystemSettingsPanel extends JPanel {

    private final SystemSettingsService settingsService = new SystemSettingsService();

    private final JTextField keyField = new JTextField(20);
    private final JTextField valueField = new JTextField(20);

    private final DefaultTableModel tableModel =
            UIUtil.emptyModel("Configuration Key", "Setting Value", "System Description");
    private final JTable table = UIUtil.readOnlyTable(tableModel);

    public SystemSettingsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UIUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildFormAndButtons(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                keyField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                valueField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            }
        });
        refreshTable();
    }

    private JPanel buildHeader() {
        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setBackground(UIUtil.BACKGROUND);
        top.add(UIUtil.heading("Global System Settings"), BorderLayout.NORTH);
        top.add(UIUtil.mutedLabel("Configure default platform behaviors, timer limits, pass thresholds, and branding"), BorderLayout.SOUTH);
        return top;
    }

    private JPanel buildFormAndButtons() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setBackground(UIUtil.BACKGROUND);

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 1),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        formCard.add(UIUtil.fieldLabel("Setting Key:"), gc);
        gc.gridx = 1;
        keyField.setFont(UIUtil.normalFont());
        keyField.setPreferredSize(new Dimension(220, 32));
        formCard.add(keyField, gc);

        gc.gridx = 2;
        formCard.add(UIUtil.fieldLabel("Value:"), gc);
        gc.gridx = 3;
        valueField.setFont(UIUtil.normalFont());
        valueField.setPreferredSize(new Dimension(220, 32));
        formCard.add(valueField, gc);

        JLabel hint = new JLabel("<html><small style='color: #64748B;'>Select any setting in the table above to populate the form for updating.</small></html>");
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 4;
        formCard.add(hint, gc);

        container.add(formCard, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttons.setBackground(UIUtil.BACKGROUND);

        JButton save = UIUtil.successButton("💾 Save Setting");
        JButton clear = UIUtil.secondaryButton("Clear");
        JButton refresh = UIUtil.secondaryButton("Refresh");

        save.addActionListener(e -> saveSetting());
        clear.addActionListener(e -> {
            keyField.setText("");
            valueField.setText("");
        });
        refresh.addActionListener(e -> refreshTable());

        buttons.add(save);
        buttons.add(clear);
        buttons.add(refresh);
        container.add(buttons, BorderLayout.SOUTH);

        return container;
    }

    private void saveSetting() {
        String key = keyField.getText().trim();
        String val = valueField.getText().trim();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Setting key cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            settingsService.updateSetting(key, val);
            JOptionPane.showMessageDialog(this, "Setting '" + key + "' updated successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        try {
            tableModel.setRowCount(0);
            for (SystemSetting setting : settingsService.getAllSettings()) {
                tableModel.addRow(new Object[]{
                        setting.getKey(),
                        setting.getValue(),
                        setting.getDescription() == null ? "" : setting.getDescription()
                });
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, "Could not load settings:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
