package com.quizplatform.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Central styling helper - keeps the look of all Swing windows consistent
 * ("clean academic dashboard design" without any external framework).
 */
public final class UIUtil {

    // Color palette
    public static final Color PRIMARY = new Color(36, 59, 85);      // dark blue sidebar
    public static final Color ACCENT = new Color(41, 128, 185);     // buttons / links
    public static final Color BACKGROUND = new Color(236, 240, 241);
    public static final Color CARD = Color.WHITE;
    public static final Color SUCCESS = new Color(39, 174, 96);
    public static final Color DANGER = new Color(192, 57, 43);
    public static final Color TEXT = new Color(44, 62, 80);

    private UIUtil() {
    }

    // ---- fonts ----
    public static Font titleFont() {
        return new Font("Segoe UI", Font.BOLD, 22);
    }

    public static Font headingFont() {
        return new Font("Segoe UI", Font.BOLD, 16);
    }

    public static Font normalFont() {
        return new Font("Segoe UI", Font.PLAIN, 14);
    }

    public static Font smallFont() {
        return new Font("Segoe UI", Font.PLAIN, 12);
    }

    // ---- components ----
    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(titleFont());
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(headingFont());
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(normalFont());
        label.setForeground(TEXT);
        return label;
    }

    public static JButton primaryButton(String text) {
        return styledButton(text, ACCENT);
    }

    public static JButton dangerButton(String text) {
        return styledButton(text, DANGER);
    }

    public static JButton successButton(String text) {
        return styledButton(text, SUCCESS);
    }

    public static JButton neutralButton(String text) {
        return styledButton(text, new Color(108, 122, 137));
    }

    private static JButton styledButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(normalFont());
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setPreferredSize(new Dimension(Math.max(110, text.length() * 9 + 30), 36));
        return button;
    }

    /** White rounded card used to group a section of the screen. */
    public static JPanel card(String title, JComponent content) {
        JPanel wrapper = new JPanel(new java.awt.BorderLayout(0, 8));
        wrapper.setBackground(CARD);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 228)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        if (title != null && !title.isBlank()) {
            wrapper.add(heading(title), java.awt.BorderLayout.NORTH);
        }
        wrapper.add(content, java.awt.BorderLayout.CENTER);
        return wrapper;
    }

    /** Read-only, non-editable JTable with nice header and row selection. */
    public static JTable readOnlyTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;   // tables are for display only
            }
        };
        table.setFont(normalFont());
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        return table;
    }

    public static JScrollPane scroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 228)));
        return scroll;
    }

    // ---- dialogs (used to display exception messages) ----
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static DefaultTableModel emptyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTableHeader header(JTable table) {
        return table.getTableHeader();
    }
}
