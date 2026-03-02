package me.bytebeats.mns.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import me.bytebeats.mns.ui.swing.MultiSelectComboBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public class SettingWindow implements Configurable {
    private JPanel mns_setting;
    private JTextField us_stock_input;
    private JTextField hk_stock_input;
    private JTextField sh_stock_input;
    private JLabel us_stock;
    private JLabel hk_stock;
    private JLabel sh_stock;
    private JRadioButton red_rise_green_fall;
    private JRadioButton red_fall_green_rise;
    private JPanel mkt_setting_radio;
    private JLabel hide_mode_desc;
    private JCheckBox hide_mode_setting;
    private JCheckBox show_pinyin_name;
    private JCheckBox show_us_stocks;
    private JCheckBox show_hk_stocks;
    private JCheckBox show_cn_stocks;
    private JCheckBox show_funds;
    private JCheckBox show_crypto;
    private JLabel sz_stock;
    private JTextField sz_stock_input;
    private JLabel idx_label;
    private JComboBox<String> idx_input;
    private JLabel daily_fund;
    private JTextField daily_fund_input;
    private JLabel mkt_setting_label;
    private JTextField crypto_currency_input;
    private JLabel crypto_currency;
    private JLabel refresh_frequency_label;
    private JLabel refresh_frequency_stock;
    private JComboBox<String> refresh_frequency_stock_list;
    private JLabel refresh_frequency_fund;
    private JComboBox<String> refresh_frequency_fund_list;
    private JLabel refresh_frequency_digital;
    private JComboBox<String> refresh_frequency_digital_list;
    private JLabel refresh_frequency_indices;
    private JComboBox<String> refresh_frequency_indices_list;

    private JLabel profile_label;
    private JPanel profile_panel;
    private JPanel buttonPanel;
    private JComboBox<String> profile_list;
    private JButton profile_add;
    private JButton profile_duplicate;
    private JButton profile_rename;
    private JButton profile_delete;

    private MultiSelectComboBox coreIndicesMultiSelect;

    /**
     * profileDrafts 保存的是“界面上的草稿”，避免用户在点 Apply 之前就把数据写回全局 {@link AppSettingState}。
     * 应用配置时，再把草稿整体覆盖回去，这样用户可以放心地在多个配置方案之间来回切换编辑。
     */
    private final List<AppSettingState.Profile> profileDrafts = new ArrayList<>();
    private String activeProfileDraftName = AppSettingState.DEFAULT_PROFILE_NAME;
    private boolean profileSwitching = false;

    private final String[] INDICES_FREQUENCIES = {"2", "5", "10", "20", "30"};
    private final String[] STOCK_FREQUENCIES = {"1", "3", "5", "8", "10"};
    private final String[] FUND_FREQUENCIES = {"5", "10", "20", "30", "60"};
    private final String[] DIGITAL_FREQUENCIES = {"2", "5", "10", "15", "20"};

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return mns_setting.getToolTipText();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        setupCoreIndicesComboBox();
        setupProfiles();
        for (String frequency : INDICES_FREQUENCIES) {
            refresh_frequency_indices_list.addItem(frequency);
        }
        for (String frequency : STOCK_FREQUENCIES) {
            refresh_frequency_stock_list.addItem(frequency);
        }
        for (String frequency : FUND_FREQUENCIES) {
            refresh_frequency_fund_list.addItem(frequency);
        }
        for (String frequency : DIGITAL_FREQUENCIES) {
            refresh_frequency_digital_list.addItem(frequency);
        }
        red_rise_green_fall.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                red_fall_green_rise.setSelected(false);
            }
        });
        red_fall_green_rise.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                red_rise_green_fall.setSelected(false);
            }
        });
        hide_mode_setting.addItemListener(e -> {
            boolean hidden = e.getStateChange() == ItemEvent.SELECTED;
            red_rise_green_fall.setEnabled(!hidden);
            red_fall_green_rise.setEnabled(!hidden);
            // 隐秘模式开启时才能勾选名称拼音显示
            show_pinyin_name.setEnabled(hidden);
            if (!hidden) {
                show_pinyin_name.setSelected(false);
            }
        });
        return mns_setting;
    }

    private void setupCoreIndicesComboBox() {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        Arrays.stream(AppSettingState.ALL_INDICES.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(options::add);

        coreIndicesMultiSelect = new MultiSelectComboBox(idx_input, options, ";");
    }

    private void setupProfiles() {
        profileSwitching = true;
        try {
            // 移除已有的事件监听器，避免重复添加导致对话框弹出多次
            for (java.awt.event.ActionListener listener : profile_list.getActionListeners()) {
                profile_list.removeActionListener(listener);
            }
            for (java.awt.event.ActionListener listener : profile_add.getActionListeners()) {
                profile_add.removeActionListener(listener);
            }
            for (java.awt.event.ActionListener listener : profile_duplicate.getActionListeners()) {
                profile_duplicate.removeActionListener(listener);
            }
            for (java.awt.event.ActionListener listener : profile_rename.getActionListeners()) {
                profile_rename.removeActionListener(listener);
            }
            for (java.awt.event.ActionListener listener : profile_delete.getActionListeners()) {
                profile_delete.removeActionListener(listener);
            }

            profile_list.removeAllItems();
            profileDrafts.clear();

            AppSettingState settings = AppSettingState.getInstance();
            settings.ensureProfiles();

            // 将持久化的 Profile 拷贝到草稿中，避免直接修改全局状态。
            if (settings.profiles != null) {
                for (AppSettingState.Profile p : settings.profiles) {
                    if (p == null) continue;
                    AppSettingState.Profile copy = new AppSettingState.Profile(p);
                    if (copy.name == null || copy.name.trim().isEmpty()) {
                        copy.name = AppSettingState.DEFAULT_PROFILE_NAME;
                    }
                    profileDrafts.add(copy);
                }
            }
            if (profileDrafts.isEmpty()) {
                profileDrafts.add(new AppSettingState.Profile());
            }

            activeProfileDraftName = settings.activeProfileName;
            if (activeProfileDraftName == null || activeProfileDraftName.trim().isEmpty()) {
                activeProfileDraftName = profileDrafts.get(0).name;
            }

            for (AppSettingState.Profile p : profileDrafts) {
                profile_list.addItem(p.name);
            }
            profile_list.setSelectedItem(activeProfileDraftName);
            applyProfileToUI(requireDraft(activeProfileDraftName));
        } finally {
            profileSwitching = false;
        }

        profile_list.addActionListener(e -> {
            if (profileSwitching) return;
            Object selected = profile_list.getSelectedItem();
            if (selected == null) return;
            String name = selected.toString();
            if (Objects.equals(name, activeProfileDraftName)) return;

            profileSwitching = true;
            try {
                // 先把当前 UI 上的改动写回“旧的”草稿，再切换到新的方案并刷新 UI。
                saveUIToProfile(requireDraft(activeProfileDraftName));
                activeProfileDraftName = name;
                applyProfileToUI(requireDraft(activeProfileDraftName));
                
                // 切换方案后，同步刷新 AppSettingState 和 ToolWindow 显示
                syncToAppSettingStateAndRefreshUI();
            } finally {
                profileSwitching = false;
            }
        });

        profile_add.addActionListener(e -> {
            // 禁用按钮防止重复点击
            profile_add.setEnabled(false);
            try {
                // 获取正确的顶级窗口，确保对话框能正确模态显示
                Window window = SwingUtilities.getWindowAncestor(mns_setting);
                String name = JOptionPane.showInputDialog(window, "请输入配置方案名称：", "新增配置方案", JOptionPane.PLAIN_MESSAGE);
                if (name == null || name.trim().isEmpty()) return;
                name = name.trim();
                if (findDraft(name) != null) {
                    JOptionPane.showMessageDialog(window, "已存在同名配置方案。", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // 新增前，先保存当前方案的界面改动。
                saveUIToProfile(requireDraft(activeProfileDraftName));
                AppSettingState.Profile p = new AppSettingState.Profile();
                p.name = name;
                profileDrafts.add(p);
                // 直接添加并选中，不触发 profile_list 的事件
                profileSwitching = true;
                try {
                    profile_list.addItem(name);
                    profile_list.setSelectedItem(name);
                    activeProfileDraftName = name;
                    applyProfileToUI(requireDraft(name));
                } finally {
                    profileSwitching = false;
                }
            } finally {
                // 重新启用按钮
                profile_add.setEnabled(true);
            }
        });

        profile_duplicate.addActionListener(e -> {
            AppSettingState.Profile current = requireDraft(activeProfileDraftName);
            saveUIToProfile(current);
            AppSettingState.Profile copy = new AppSettingState.Profile(current);
            copy.name = uniqueName(current.name + " 副本");
            profileDrafts.add(copy);
            refreshProfileCombo(copy.name);
        });

        profile_rename.addActionListener(e -> {
            AppSettingState.Profile current = requireDraft(activeProfileDraftName);
            saveUIToProfile(current);
            // 获取正确的顶级窗口，确保对话框能正确模态显示
            Window window = SwingUtilities.getWindowAncestor(mns_setting);
            String name = JOptionPane.showInputDialog(window, "请输入新名称：", "重命名配置方案", JOptionPane.PLAIN_MESSAGE);
            if (name == null) return;
            name = name.trim();
            if (name.isEmpty()) return;
            if (!Objects.equals(name, current.name) && findDraft(name) != null) {
                JOptionPane.showMessageDialog(window, "已存在同名配置方案。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            current.name = name;
            activeProfileDraftName = name;
            refreshProfileCombo(name);
        });

        profile_delete.addActionListener(e -> {
            if (profileDrafts.size() <= 1) {
                Window window = SwingUtilities.getWindowAncestor(mns_setting);
                JOptionPane.showMessageDialog(window, "至少需要保留 1 个配置方案。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Window window = SwingUtilities.getWindowAncestor(mns_setting);
            int res = JOptionPane.showConfirmDialog(window, "确定删除当前配置方案吗？", "确认删除", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;
            AppSettingState.Profile current = requireDraft(activeProfileDraftName);
            profileDrafts.remove(current);
            String next = profileDrafts.get(0).name;
            activeProfileDraftName = next;
            refreshProfileCombo(next);
        });
    }

    private void refreshProfileCombo(String selectName) {
        profileSwitching = true;
        try {
            profile_list.removeAllItems();
            for (AppSettingState.Profile p : profileDrafts) {
                profile_list.addItem(p.name);
            }
            profile_list.setSelectedItem(selectName);
            activeProfileDraftName = selectName;
            applyProfileToUI(requireDraft(selectName));
        } finally {
            profileSwitching = false;
        }
    }

    private String uniqueName(String base) {
        String name = base;
        int i = 2;
        while (findDraft(name) != null) {
            name = base + " " + i;
            i++;
        }
        return name;
    }

    private AppSettingState.Profile findDraft(String name) {
        if (name == null) return null;
        for (AppSettingState.Profile p : profileDrafts) {
            if (p != null && name.equals(p.name)) return p;
        }
        return null;
    }

    private AppSettingState.Profile requireDraft(String name) {
        AppSettingState.Profile p = findDraft(name);
        if (p != null) return p;
        // fallback to first draft
        AppSettingState.Profile first = profileDrafts.get(0);
        activeProfileDraftName = first.name;
        return first;
    }

    private void applyProfileToUI(AppSettingState.Profile p) {
        us_stock_input.setText(p.usStocks);
        hk_stock_input.setText(p.hkStocks);
        sh_stock_input.setText(p.shStocks);
        sz_stock_input.setText(p.szStocks);
        daily_fund_input.setText(p.dailyFunds);
        crypto_currency_input.setText(p.cryptoCurrencies);

        red_rise_green_fall.setSelected(p.isRedRise);
        red_fall_green_rise.setSelected(!p.isRedRise);
        hide_mode_setting.setSelected(p.isHiddenMode);
        show_pinyin_name.setSelected(p.showPinyinName);
        show_pinyin_name.setEnabled(p.isHiddenMode);
        red_rise_green_fall.setEnabled(!p.isHiddenMode);
        red_fall_green_rise.setEnabled(!p.isHiddenMode);

        // 显示/隐藏各分组开关
        show_us_stocks.setSelected(p.showUSStocks);
        show_hk_stocks.setSelected(p.showHKStocks);
        show_cn_stocks.setSelected(p.showCNStocks);
        show_funds.setSelected(p.showFunds);
        show_crypto.setSelected(p.showCrypto);

        String coreIndices = p.coreIndices;
        if (coreIndices == null || coreIndices.trim().isEmpty()) {
            coreIndices = AppSettingState.ALL_INDICES;
        }
        coreIndicesMultiSelect.setText(coreIndices);

        refresh_frequency_indices_list.setSelectedItem(String.valueOf(p.indicesFrequency));
        refresh_frequency_stock_list.setSelectedItem(String.valueOf(p.stockFrequency));
        refresh_frequency_fund_list.setSelectedItem(String.valueOf(p.fundFrequency));
        refresh_frequency_digital_list.setSelectedItem(String.valueOf(p.cryptoFrequency));
    }

    private void saveUIToProfile(AppSettingState.Profile p) {
        p.usStocks = us_stock_input.getText();
        p.hkStocks = hk_stock_input.getText();
        p.shStocks = sh_stock_input.getText();
        p.szStocks = sz_stock_input.getText();
        p.dailyFunds = daily_fund_input.getText();
        p.cryptoCurrencies = crypto_currency_input.getText();
        p.isRedRise = red_rise_green_fall.isSelected();
        p.isHiddenMode = hide_mode_setting.isSelected();
        p.showPinyinName = show_pinyin_name.isSelected();

        // 保存显示/隐藏各分组开关
        p.showUSStocks = show_us_stocks.isSelected();
        p.showHKStocks = show_hk_stocks.isSelected();
        p.showCNStocks = show_cn_stocks.isSelected();
        p.showFunds = show_funds.isSelected();
        p.showCrypto = show_crypto.isSelected();

        Object coreIndicesValue = idx_input.getEditor().getItem();
        String coreIndices = coreIndicesValue == null ? "" : coreIndicesValue.toString().trim();
        if (coreIndices.isEmpty()) {
            coreIndices = AppSettingState.ALL_INDICES;
        }
        p.coreIndices = coreIndices;

        p.indicesFrequency = Integer.parseInt(refresh_frequency_indices_list.getSelectedItem().toString());
        p.stockFrequency = Integer.parseInt(refresh_frequency_stock_list.getSelectedItem().toString());
        p.fundFrequency = Integer.parseInt(refresh_frequency_fund_list.getSelectedItem().toString());
        p.cryptoFrequency = Integer.parseInt(refresh_frequency_digital_list.getSelectedItem().toString());
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        final AppSettingState settings = AppSettingState.getInstance();
        saveUIToProfile(requireDraft(activeProfileDraftName));

        settings.profiles = new ArrayList<>();
        for (AppSettingState.Profile p : profileDrafts) {
            settings.profiles.add(new AppSettingState.Profile(p));
        }
        settings.activeProfileName = activeProfileDraftName;
        settings.applyActiveProfileToFields();

        // 刷新 ToolWindow 内容的显示/隐藏
        refreshToolWindowContent();
        
        // 刷新所有窗口的数据
        MainWindow mainWindow = MainWindow.getInstance();
        if (mainWindow != null) {
            mainWindow.refreshAllWindows();
        }
    }

    /**
     * 同步当前草稿到 AppSettingState 并刷新 UI
     */
    private void syncToAppSettingStateAndRefreshUI() {
        final AppSettingState settings = AppSettingState.getInstance();
        
        // 将当前草稿同步到 AppSettingState
        settings.profiles = new ArrayList<>();
        for (AppSettingState.Profile p : profileDrafts) {
            settings.profiles.add(new AppSettingState.Profile(p));
        }
        settings.activeProfileName = activeProfileDraftName;
        settings.applyActiveProfileToFields();

        // 刷新所有窗口的数据（包含刷新 ToolWindow 内容的显示/隐藏）
        MainWindow mainWindow = MainWindow.getInstance();
        if (mainWindow != null) {
            mainWindow.refreshAllWindows();
        }
    }

    /**
     * 刷新 ToolWindow 内容的显示/隐藏状态
     */
    private void refreshToolWindowContent() {
        // 使用 MainWindow 实例调用刷新方法
        MainWindow mainWindow = MainWindow.getInstance();
        if (mainWindow != null) {
            mainWindow.refreshContentVisibility();
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return us_stock_input;
    }

    @Override
    public void reset() {
        // reload drafts from persisted settings
        setupProfiles();
    }

    @Override
    public void disposeUIResources() {
        mns_setting = null;
    }

    @Override
    public void cancel() {

    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
