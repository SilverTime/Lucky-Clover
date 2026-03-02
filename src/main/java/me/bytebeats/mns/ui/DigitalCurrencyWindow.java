package me.bytebeats.mns.ui;

import me.bytebeats.mns.SymbolParser;
import me.bytebeats.mns.handler.SinaDigitalCurrencyHandler;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="https://github.com/bytebeats">bytebeats</a>
 * @email <bvzgong@gmail.com>
 * @since 2021/9/17 11:32
 */
public class DigitalCurrencyWindow implements SymbolParser {
    private JPanel digital_currency_window;
    private JScrollPane digital_currency_scroll;
    private JTable digital_currency_table;
    private JLabel digital_currency_timestamp;
    private JButton digital_currency_sync;
    private JButton digital_currency_search;
    private JComboBox<String> digital_currency_profile_list;

    private final SinaDigitalCurrencyHandler handler;

    private FundSearchDialog fundSearchDialog;

    public DigitalCurrencyWindow() {
        handler = new SinaDigitalCurrencyHandler(digital_currency_table, digital_currency_timestamp);
    }

    public JPanel getJPanel() {
        return digital_currency_window;
    }

    public void onInit() {
        // 先停止旧的定时器，避免多个定时器同时运行
        handler.stop();
        
        // 初始化 Profile 下拉框
        initProfileComboBox();
        
        digital_currency_sync.addActionListener(e -> {
            handler.stop();
            syncRefresh();
        });
        digital_currency_search.addActionListener(e -> popSearchDialog());
        syncRefresh();
    }
    
    /**
     * 初始化 Profile 下拉框
     */
    private void initProfileComboBox() {
        AppSettingState settings = AppSettingState.getInstance();
        settings.ensureProfiles();
        
        // 始终显示 Profile 下拉框
        digital_currency_profile_list.setVisible(true);
        digital_currency_profile_list.removeAllItems();
        
        // 添加所有 Profile 名称到下拉框
        for (AppSettingState.Profile profile : settings.profiles) {
            if (profile != null && profile.name != null) {
                digital_currency_profile_list.addItem(profile.name);
            }
        }
        
        // 设置当前选中的 Profile
        String activeProfileName = settings.activeProfileName;
        if (activeProfileName != null) {
            digital_currency_profile_list.setSelectedItem(activeProfileName);
        }
        
        // 添加切换监听器
        digital_currency_profile_list.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                handler.stop();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                String selectedProfile = (String) digital_currency_profile_list.getSelectedItem();
                if (selectedProfile != null && !selectedProfile.equals(settings.activeProfileName)) {
                    switchProfile(selectedProfile);
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }
    
    /**
     * 切换 Profile
     */
    private void switchProfile(String profileName) {
        AppSettingState settings = AppSettingState.getInstance();
        settings.setActiveProfileName(profileName);
        
        // 刷新所有窗口
        MainWindow.getInstance().refreshAllWindows();
    }

    private void syncRefresh() {
        handler.load(parse());
    }

    @Override
    public String prefix() {
        return "";
    }

    @Override
    public String raw() {
        return AppSettingState.getInstance().cryptoCurrencies;
    }

    @Override
    public List<String> parse() {
        List<String> symbols = new ArrayList<>();
        String raw = raw();
        assert raw != null;
        if (!raw.isEmpty()) {
            Arrays.stream(raw.split("[,; ]")).filter(s -> !s.isEmpty()).forEach(s -> symbols.add(prefix() + s));
        }
        return symbols;
    }

    private void popSearchDialog() {
        if (fundSearchDialog == null) {
            fundSearchDialog = new FundSearchDialog();
            fundSearchDialog.setCallback(() -> {
                // do nothing here
            });
            fundSearchDialog.addWindowListener(new WindowListener() {
                @Override
                public void windowOpened(WindowEvent e) {
                    handler.stop();
                }

                @Override
                public void windowClosing(WindowEvent e) {

                }

                @Override
                public void windowClosed(WindowEvent e) {
                    syncRefresh();
                }

                @Override
                public void windowIconified(WindowEvent e) {

                }

                @Override
                public void windowDeiconified(WindowEvent e) {

                }

                @Override
                public void windowActivated(WindowEvent e) {

                }

                @Override
                public void windowDeactivated(WindowEvent e) {

                }
            });
        }
        if (fundSearchDialog.isVisible()) {
            return;
        }
        fundSearchDialog.pack();
        Dimension screenerSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) (screenerSize.getWidth() / 2 - fundSearchDialog.getWidth() / 2);
        int y = (int) (screenerSize.getHeight() / 2 - fundSearchDialog.getHeight() / 2);
        fundSearchDialog.setLocation(x, y);
        fundSearchDialog.setVisible(true);
    }
}