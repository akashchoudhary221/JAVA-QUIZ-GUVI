package com.quizplatform.gui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Central styling system and UI helper - maintains a consistent, modern,
 * and professional design system across all Swing windows.
 *
 * Design System Specifications:
 *  - Primary: Deep Navy (#172554)
 *  - Accent: Blue (#3B82F6)
 *  - Background: Soft Neutral (#F5F7FB)
 *  - Card Surface: White (#FFFFFF)
 *  - Text Primary: Dark Slate (#172033)
 *  - Text Secondary / Muted: Slate (#64748B)
 *  - Success: Emerald Green (#10B981)
 *  - Warning: Amber (#F59E0B)
 *  - Danger / Error: Coral Red (#EF4444)
 *  - Borders: Light Neutral Gray (#E2E8F0)
 */
public final class UIUtil {

    // ---- Color Palette ----
    public static final Color PRIMARY = new Color(0x17, 0x25, 0x54);          // Deep navy #172554
    public static final Color PRIMARY_DARK = new Color(0x0F, 0x17, 0x2A);     // Darkest navy #0F172A
    public static final Color PRIMARY_LIGHT = new Color(0x1E, 0x29, 0x3B);    // Slate navy #1E293B
    public static final Color PRIMARY_HOVER = new Color(0x25, 0x36, 0x6E);    // Hover state
    public static final Color ACCENT = new Color(0x3B, 0x82, 0xF6);           // Blue #3B82F6
    public static final Color ACCENT_HOVER = new Color(0x25, 0x63, 0xEB);     // #2563EB
    public static final Color BACKGROUND = new Color(0xF5, 0xF7, 0xFB);       // Soft neutral #F5F7FB
    public static final Color CARD = Color.WHITE;                             // #FFFFFF
    public static final Color TEXT = new Color(0x17, 0x20, 0x33);             // Primary dark slate #172033
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);       // Secondary muted slate #64748B
    public static final Color SUCCESS = new Color(0x10, 0xB9, 0x81);          // Emerald green #10B981
    public static final Color SUCCESS_LIGHT = new Color(0xEC, 0xFD, 0xF5);    // Tint for success badges
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);          // Amber #F59E0B
    public static final Color WARNING_LIGHT = new Color(0xFF, 0xFB, 0xEB);    // Tint for warning badges
    public static final Color DANGER = new Color(0xEF, 0x44, 0x44);           // Red #EF4444
    public static final Color DANGER_LIGHT = new Color(0xFE, 0xF2, 0xF2);     // Tint for danger badges
    public static final Color BORDER = new Color(0xE2, 0xE8, 0xF0);           // Light neutral border #E2E8F0
    public static final Color HOVER_ROW = new Color(0xF1, 0xF5, 0xF9);        // Table hover row tint
    public static final Color SELECTED_ROW = new Color(0xDB, 0xEA, 0xFE);     // Table selected row tint

    private static boolean themeInitialized = false;

    private UIUtil() {
    }

    /**
     * Initializes the modern FlatLaf Look and Feel with custom design tokens.
     * Safe to call multiple times (only runs once).
     */
    public static synchronized void setupTheme() {
        if (themeInitialized) {
            return;
        }
        try {
            // Apply FlatLaf Light
            FlatLightLaf.setup();

            // Set global component styling
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.gridColor", BORDER);
            UIManager.put("Table.selectionBackground", SELECTED_ROW);
            UIManager.put("Table.selectionForeground", TEXT);
            UIManager.put("TableHeader.bottomSeparatorColor", BORDER);
            UIManager.put("ScrollBar.thumbArc", 6);
            UIManager.put("ScrollBar.width", 10);
            themeInitialized = true;
        } catch (Exception e) {
            // Fallback to system Look & Feel if FlatLaf fails
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }
    }

    // ---- Typography ----
    public static Font titleFont() {
        return new Font("Segoe UI", Font.BOLD, 22);
    }

    public static Font subTitleFont() {
        return new Font("Segoe UI", Font.BOLD, 18);
    }

    public static Font headingFont() {
        return new Font("Segoe UI", Font.BOLD, 15);
    }

    public static Font normalFont() {
        return new Font("Segoe UI", Font.PLAIN, 14);
    }

    public static Font boldFont() {
        return new Font("Segoe UI", Font.BOLD, 14);
    }

    public static Font smallFont() {
        return new Font("Segoe UI", Font.PLAIN, 12);
    }

    public static Font smallBoldFont() {
        return new Font("Segoe UI", Font.BOLD, 12);
    }

    // ---- Labels ----
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
        label.setFont(boldFont());
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallFont());
        label.setForeground(TEXT_MUTED);
        return label;
    }

    // ---- Buttons with Hover and Elevation ----
    public static JButton primaryButton(String text) {
        return styledButton(text, ACCENT, ACCENT_HOVER, Color.WHITE);
    }

    public static JButton dangerButton(String text) {
        return styledButton(text, DANGER, new Color(0xDC, 0x26, 0x26), Color.WHITE);
    }

    public static JButton successButton(String text) {
        return styledButton(text, SUCCESS, new Color(0x05, 0x96, 0x69), Color.WHITE);
    }

    public static JButton neutralButton(String text) {
        return styledButton(text, new Color(0x64, 0x74, 0x8B), new Color(0x47, 0x55, 0x69), Color.WHITE);
    }

    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(normalFont());
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        btn.setPreferredSize(new Dimension(Math.max(105, text.length() * 9 + 30), 36));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(0xF1, 0xF5, 0xF9));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
            }
        });
        return btn;
    }

    private static JButton styledButton(String text, Color background, Color hoverColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(boldFont());
        button.setBackground(background);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setPreferredSize(new Dimension(Math.max(110, text.length() * 9 + 30), 36));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
        return button;
    }

    // ---- Modern Card Surface ----
    public static JPanel card(String title, JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setBackground(CARD);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        if (title != null && !title.isBlank()) {
            JLabel header = heading(title);
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            wrapper.add(header, BorderLayout.NORTH);
        }
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    // ---- KPI Metric Card ----
    public static JPanel metricCard(String caption, JLabel valueLabel, String subtitle, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(CARD);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(smallBoldFont());
        captionLabel.setForeground(TEXT_MUTED);
        top.add(captionLabel, BorderLayout.WEST);

        // Accent colored small indicator pill/bar on top right
        JPanel indicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentColor != null ? accentColor : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        indicator.setPreferredSize(new Dimension(28, 4));
        indicator.setOpaque(false);
        top.add(indicator, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valueLabel.setForeground(TEXT);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(valueLabel, BorderLayout.CENTER);

        if (subtitle != null && !subtitle.isBlank()) {
            JLabel subLabel = new JLabel(subtitle);
            subLabel.setFont(smallFont());
            subLabel.setForeground(TEXT_MUTED);
            card.add(subLabel, BorderLayout.SOUTH);
        }

        return card;
    }

    // ---- Status Badges ----
    public static JLabel statusBadge(String status) {
        String labelText = status != null ? status : "UNKNOWN";
        Color bg = new Color(0xED, 0xF2, 0xF7);
        Color fg = TEXT_MUTED;

        String upper = labelText.toUpperCase();
        if (upper.contains("APPROVED") || upper.contains("PUBLISHED") || upper.contains("COMPLETED") || upper.contains("PASS")) {
            bg = SUCCESS_LIGHT;
            fg = new Color(0x04, 0x78, 0x57);
        } else if (upper.contains("PENDING")) {
            bg = WARNING_LIGHT;
            fg = new Color(0xB4, 0x53, 0x09);
        } else if (upper.contains("REJECTED") || upper.contains("FAIL") || upper.contains("AUTO_SUBMITTED")) {
            bg = DANGER_LIGHT;
            fg = new Color(0xB9, 0x1C, 0x1C);
        } else if (upper.contains("DRAFT")) {
            bg = new Color(0xF1, 0xF5, 0xF9);
            fg = new Color(0x47, 0x55, 0x69);
        }

        return badge(labelText, bg, fg);
    }

    public static JLabel badge(String text, Color bgColor, Color fgColor) {
        JLabel badge = new JLabel("  " + text + "  ", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(smallBoldFont());
        badge.setForeground(fgColor);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return badge;
    }

    // ---- Modern JTable with Padded Rows and Clean Headers ----
    public static JTable readOnlyTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;   // read-only display
            }
        };
        table.setFont(normalFont());
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setSelectionBackground(SELECTED_ROW);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(boldFont());
        header.setBackground(new Color(0xF8, 0xFA, 0xFC));
        header.setForeground(TEXT);
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        // Default cell renderer with padding
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel label) {
                    label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                }
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF8, 0xFA, 0xFC));
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        return table;
    }

    public static JScrollPane scroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.getViewport().setBackground(CARD);
        return scroll;
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

    // ---- Dialogs ----
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm Action",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
