package me.bytebeats.mns.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * A lightweight multi-select dropdown controller built on top of an editable {@link JComboBox}.
 *
 * <p>设计思路（便于小白理解）：
 * <ul>
 *   <li>视图层仍然使用普通的 {@code JComboBox}，方便在 GUI 设计器中拖拽使用；</li>
 *   <li>这个类只做“控制器”：接管下拉弹窗，换成带复选框的 {@code JList}；</li>
 *   <li>用户选择的结果会同步回编辑框文本，用固定分隔符（默认 {@code ;}）拼接，</li>
 *   <li>因此外部代码只需要读/写文本，就能拿到所有已选项，无需关心多选的内部实现。</li>
 * </ul>
 * 这种模式在很多桌面应用中常见：用简单控件做“壳”，用控制器增强行为。
 */
public final class MultiSelectComboBox {
    private final JComboBox<String> comboBox;
    private final String delimiter;
    private final DefaultListModel<CheckItem> listModel = new DefaultListModel<>();
    private final JList<CheckItem> list = new JList<>(listModel);
    private final JPopupMenu popup = new JPopupMenu();
    private final JTextField editor;

    public MultiSelectComboBox(JComboBox<String> comboBox, Collection<String> options, String delimiter) {
        this.comboBox = comboBox;
        this.delimiter = delimiter == null || delimiter.isEmpty() ? ";" : delimiter;
        this.comboBox.setEditable(true);
        this.editor = (JTextField) this.comboBox.getEditor().getEditorComponent();

        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        if (options != null) {
            for (String opt : options) {
                if (opt != null) {
                    String v = opt.trim();
                    if (!v.isEmpty()) dedup.add(v);
                }
            }
        }
        for (String opt : dedup) {
            listModel.addElement(new CheckItem(opt, false));
        }

        initPopup();
        installListeners();
    }

    public void setText(String text) {
        editor.setText(text == null ? "" : text);
        syncFromEditor();
    }

    public void syncFromEditor() {
        Set<String> wanted = parseTokens(editor.getText());
        if (wanted.isEmpty()) {
            setSelected(Collections.emptySet());
            return;
        }
        ensureOptionsExist(wanted);
        setSelected(wanted);
    }

    public Set<String> getSelected() {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (int i = 0; i < listModel.size(); i++) {
            CheckItem item = listModel.get(i);
            if (item.selected) {
                selected.add(item.value);
            }
        }
        return selected;
    }

    private void initPopup() {
        list.setCellRenderer((jList, value, index, isSelected, cellHasFocus) -> {
            JCheckBox box = new JCheckBox(value.value, value.selected);
            box.setOpaque(true);
            box.setBorder(new EmptyBorder(2, 6, 2, 6));
            if (isSelected) {
                box.setBackground(jList.getSelectionBackground());
                box.setForeground(jList.getSelectionForeground());
            } else {
                box.setBackground(jList.getBackground());
                box.setForeground(jList.getForeground());
            }
            return box;
        });

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(Math.min(12, Math.max(6, listModel.size())));

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(Math.max(260, comboBox.getWidth()), 240));

        popup.setBorder(BorderFactory.createLineBorder(UIManager.getColor("PopupMenu.border")));
        popup.add(scrollPane);
    }

    private void installListeners() {
        // Show popup when user clicks the combo box (including arrow area).
        comboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                togglePopup();
            }
        });

        // Also allow clicking editor to open the popup.
        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    togglePopup();
                }
            }
        });

        // Keep selection state in sync if user types manually.
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                syncFromEditor();
            }
        });
        editor.addActionListener(e -> syncFromEditor());

        // Toggle selection on click without closing.
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;
                Rectangle bounds = list.getCellBounds(index, index);
                if (bounds != null && !bounds.contains(e.getPoint())) return;
                toggleAt(index);
            }
        });

        // Keyboard toggles.
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    int index = list.getSelectedIndex();
                    if (index >= 0) toggleAt(index);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    popup.setVisible(false);
                    e.consume();
                }
            }
        });
    }

    private void toggleAt(int index) {
        CheckItem item = listModel.get(index);
        item.selected = !item.selected;
        list.repaint(list.getCellBounds(index, index));
        updateEditorFromSelected();
    }

    private void togglePopup() {
        if (popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        editor.requestFocusInWindow();
        popup.show(comboBox, 0, comboBox.getHeight());
    }

    private void updateEditorFromSelected() {
        String text = String.join(delimiter, getSelected());
        editor.setText(text);
        editor.setCaretPosition(Math.min(editor.getText().length(), editor.getCaretPosition()));
    }

    private void ensureOptionsExist(Set<String> options) {
        Set<String> existing = new HashSet<>();
        for (int i = 0; i < listModel.size(); i++) {
            existing.add(listModel.get(i).value);
        }
        for (String opt : options) {
            if (!existing.contains(opt)) {
                listModel.addElement(new CheckItem(opt, false));
            }
        }
    }

    private void setSelected(Set<String> selected) {
        for (int i = 0; i < listModel.size(); i++) {
            CheckItem item = listModel.get(i);
            item.selected = selected.contains(item.value);
        }
        list.repaint();
        updateEditorFromSelected();
    }

    private static Set<String> parseTokens(String raw) {
        if (raw == null) return Collections.emptySet();
        String text = raw.trim();
        if (text.isEmpty()) return Collections.emptySet();
        String[] parts = text.split("[,;\\s]+");
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String v = p.trim();
            if (!v.isEmpty()) tokens.add(v);
        }
        return tokens;
    }

    private static final class CheckItem {
        private final String value;
        private boolean selected;

        private CheckItem(String value, boolean selected) {
            this.value = value;
            this.selected = selected;
        }
    }
}

