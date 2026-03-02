package me.bytebeats.mns.ui;

import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.awt.RelativePoint;
import me.bytebeats.mns.SymbolParser;
import me.bytebeats.mns.enumation.FundChartType;
import me.bytebeats.mns.handler.TianTianFundHandler;
import me.bytebeats.mns.listener.OnItemRightClickListener;
import me.bytebeats.mns.listener.WindowSwitchListener;
import me.bytebeats.mns.tool.PopupsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="https://github.com/bytebeats">bytebeats</a>
 * @email <bvzgong@gmail.com>
 * @since 2020/8/25 11:32
 */
public class FundWindow implements SymbolParser {
    private JPanel fund_window;
    private JScrollPane fund_scroll;
    private JTable fund_table;
    private JLabel fund_timestamp;
    private JButton fund_sync;
    private JButton fund_search;
    private JComboBox<String> fund_profile_list;

    private final TianTianFundHandler handler;

    private FundSearchDialog fundSearchDialog;

    public FundWindow() {
        handler = new TianTianFundHandler(fund_table, fund_timestamp);
        handler.setOnItemDoubleClickListener((s, xOnScreen, yOnScreen) -> PopupsUtil.INSTANCE.popFundChart(s, FundChartType.EstimatedNetWorth, new Point(xOnScreen, yOnScreen)));
        handler.setOnItemRightClickListener(new OnItemRightClickListener<String>() {
            @Override
            public void onItemRightClick(String s, int xOnScreen, int yOnScreen) {
                JBPopupFactory.getInstance()
                        .createListPopup(new BaseListPopupStep<FundChartType>("K线图", FundChartType.values()) {
                            @Override
                            public @NotNull
                            String getTextFor(FundChartType value) {
                                return value.getDescription();
                            }

                            @Override
                            public @Nullable
                            PopupStep<?> onChosen(FundChartType selectedValue, boolean finalChoice) {
                                PopupsUtil.INSTANCE.popFundChart(s, selectedValue, new Point(xOnScreen, yOnScreen));
                                return super.onChosen(selectedValue, finalChoice);
                            }
                        })
                        .show(RelativePoint.fromScreen(new Point(xOnScreen, yOnScreen)));
            }
        });
    }

    public JPanel getJPanel() {
        return fund_window;
    }

    public void onInit() {
        // 先停止旧的定时器，避免多个定时器同时运行
        handler.stop();
        
        // 初始化 Profile 下拉框
        initProfileComboBox();
        
        fund_sync.addActionListener(e -> {
            handler.stop();
            requestData();
        });
        fund_search.addActionListener(e -> popSearchDialog());
        // 不再自动请求数据，只在 tab 切换到对应栏目时请求
    }
    
    /**
     * 请求数据（仅在 tab 切换到对应栏目时调用）
     */
    public void requestData() {
        syncRefresh();
    }
    
    /**
     * 初始化 Profile 下拉框
     */
    private void initProfileComboBox() {
        AppSettingState settings = AppSettingState.getInstance();
        settings.ensureProfiles();
        
        // 始终显示 Profile 下拉框
        fund_profile_list.setVisible(true);
        fund_profile_list.removeAllItems();
        
        // 添加所有 Profile 名称到下拉框
        for (AppSettingState.Profile profile : settings.profiles) {
            if (profile != null && profile.name != null) {
                fund_profile_list.addItem(profile.name);
            }
        }
        
        // 设置当前选中的 Profile
        String activeProfileName = settings.activeProfileName;
        if (activeProfileName != null) {
            fund_profile_list.setSelectedItem(activeProfileName);
        }
        
        // 添加切换监听器
        fund_profile_list.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                handler.stop();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                String selectedProfile = (String) fund_profile_list.getSelectedItem();
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
        return AppSettingState.getInstance().dailyFunds;
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
            fundSearchDialog.addWindowListener(new WindowSwitchListener() {
                @Override
                public void windowOpened(WindowEvent e) {
                    handler.stop();
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    syncRefresh();
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