package me.bytebeats.mns.ui;

import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.awt.RelativePoint;
import me.bytebeats.mns.OnSymbolSelectedListener;
import me.bytebeats.mns.enumation.StockChartType;
import me.bytebeats.mns.handler.AbsStockHandler;
import me.bytebeats.mns.handler.TencentStockHandler;
import me.bytebeats.mns.listener.OnItemRightClickListener;
import me.bytebeats.mns.tool.PopupsUtil;
import me.bytebeats.mns.tool.StringResUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StockWindow {
    private JPanel stock_window;
    private JScrollPane stock_scroll;
    private JTable stock_table;
    private JLabel stock_timestamp;
    private JButton stock_refresh;
    private JComboBox<String> stock_market_list;
    private JComboBox<String> stock_profile_list;
    private JLabel profile_label;
    private JLabel market_label;

    private final TencentStockHandler handler;

    public StockWindow() {
        handler = new TencentStockHandler(stock_table, stock_timestamp) {
            @Override
            protected String getTipText() {
                return Objects.requireNonNull(stock_market_list.getSelectedItem()).toString();
            }
        };
        handler.setOnItemDoubleClickListener((s, xOnScreen, yOnScreen) -> PopupsUtil.INSTANCE.popupStockChart(s, StockChartType.Minute, new Point(xOnScreen, yOnScreen)));
        handler.setOnItemRightClickListener(new OnItemRightClickListener<String>() {
            @Override
            public void onItemRightClick(String s, int xOnScreen, int yOnScreen) {
                JBPopupFactory.getInstance()
                        .createListPopup(new BaseListPopupStep<StockChartType>("K线图", StockChartType.values()) {
                            @Override
                            public @NotNull
                            String getTextFor(StockChartType value) {
                                return value.getDescription();
                            }

                            @Override
                            public @Nullable
                            PopupStep<?> onChosen(StockChartType selectedValue, boolean finalChoice) {
                                PopupsUtil.INSTANCE.popupStockChart(s, selectedValue, new Point(xOnScreen, yOnScreen));
                                return super.onChosen(selectedValue, finalChoice);
                            }
                        })
                        .show(RelativePoint.fromScreen(new Point(xOnScreen, yOnScreen)));
            }
        });
    }

    public void setOnSymbolSelectedListener(OnSymbolSelectedListener listener) {
        handler.setOnSymbolSelectedListener(listener);
    }

    public JPanel getJPanel() {
        return stock_window;
    }

    public void onInit() {
        // 先停止旧的定时器，避免多个定时器同时运行
        handler.stop();
        
        // 初始化 Profile 下拉框
        initProfileComboBox();
        
        // 初始化市场选择下拉框（只显示有配置股票的市场）
        initMarketComboBox();
        stock_market_list.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                handler.stop();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                requestData();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        stock_market_list.setSelectedIndex(0);
        stock_refresh.addActionListener(e -> {
            handler.stop();
            requestData();
        });
        // 不再自动请求数据，只在 tab 切换到对应栏目时请求
    }
    
    /**
     * 请求数据（仅在 tab 切换到对应栏目时调用）
     */
    public void requestData() {
        // 根据实际股票代码自动判断市场类型
        autoDetectMarketType();
        syncRefresh();
    }
    
    /**
     * 根据用户配置的股票代码自动判断市场类型
     * 优先使用手动选择的市场类型，只有选择"全部"时才自动检测
     */
    private void autoDetectMarketType() {
        AppSettingState settings = AppSettingState.getInstance();
        
        // 获取选中的市场名称
        String selectedMarket = (String) stock_market_list.getSelectedItem();
        
        // 手动选择特定市场时，优先使用手动选择
        TencentStockHandler.MarketType marketType;
        
        if (StringResUtils.STOCK_US.equals(selectedMarket)) {
            marketType = TencentStockHandler.MarketType.US;
            handler.setMarketType(marketType);
            return;
        } else if (StringResUtils.STOCK_HK.equals(selectedMarket)) {
            marketType = TencentStockHandler.MarketType.HK;
            handler.setMarketType(marketType);
            return;
        } else if (StringResUtils.STOCK_CN.equals(selectedMarket)) {
            marketType = TencentStockHandler.MarketType.CN;
            handler.setMarketType(marketType);
            return;
        }
        
        // 全部或默认：根据配置判断
        boolean hasCN = false;
        boolean hasHK = false;
        boolean hasUS = false;
        
        if (settings.showUSStocks && !isStockListEmpty(settings.usStocks)) {
            hasUS = true;
        }
        if (settings.showHKStocks && !isStockListEmpty(settings.hkStocks)) {
            hasHK = true;
        }
        if (settings.showCNStocks && (!isStockListEmpty(settings.shStocks) || !isStockListEmpty(settings.szStocks))) {
            hasCN = true;
        }
        
        // 根据实际有的市场类型设置
        if (hasCN && !hasHK && !hasUS) {
            marketType = TencentStockHandler.MarketType.CN;
        } else if (!hasCN && hasHK && !hasUS) {
            marketType = TencentStockHandler.MarketType.HK;
        } else if (!hasCN && !hasHK && hasUS) {
            marketType = TencentStockHandler.MarketType.US;
        } else {
            marketType = TencentStockHandler.MarketType.ALL;
        }
        
        handler.setMarketType(marketType);
    }
    
    /**
     * 初始化 Profile 下拉框
     */
    private void initProfileComboBox() {
        AppSettingState settings = AppSettingState.getInstance();
        settings.ensureProfiles();
        
        // 始终显示 Profile 下拉框
        stock_profile_list.setVisible(true);
        stock_profile_list.removeAllItems();
        
        // 添加所有 Profile 名称到下拉框
        for (AppSettingState.Profile profile : settings.profiles) {
            if (profile != null && profile.name != null) {
                stock_profile_list.addItem(profile.name);
            }
        }
        
        // 设置当前选中的 Profile
        String activeProfileName = settings.activeProfileName;
        if (activeProfileName != null) {
            stock_profile_list.setSelectedItem(activeProfileName);
        }
        
        // 添加切换监听器
        stock_profile_list.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                handler.stop();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                String selectedProfile = (String) stock_profile_list.getSelectedItem();
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
     * 初始化市场选择下拉框
     * 只显示有配置股票的市场选项
     */
    private void initMarketComboBox() {
        AppSettingState settings = AppSettingState.getInstance();
        
        stock_market_list.removeAllItems();
        
        // 始终添加"全部"选项
        stock_market_list.addItem(StringResUtils.STOCK_ALL);
        
        // 检查各市场是否有配置股票，只添加有配置的市场
        // US市场：需要showUSStocks开启且usStocks不为空
        if (settings.showUSStocks && !isStockListEmpty(settings.usStocks)) {
            stock_market_list.addItem(StringResUtils.STOCK_US);
        }
        
        // HK市场：需要showHKStocks开启且hkStocks不为空
        if (settings.showHKStocks && !isStockListEmpty(settings.hkStocks)) {
            stock_market_list.addItem(StringResUtils.STOCK_HK);
        }
        
        // CN市场：需要showCNStocks开启且shStocks或szStocks不为空
        if (settings.showCNStocks && (!isStockListEmpty(settings.shStocks) || !isStockListEmpty(settings.szStocks))) {
            stock_market_list.addItem(StringResUtils.STOCK_CN);
        }
        
        // 添加PopupMenuListener
        stock_market_list.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                handler.stop();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                requestData();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        
        // 设置默认选中第一项
        if (stock_market_list.getItemCount() > 0) {
            stock_market_list.setSelectedIndex(0);
        }
    }
    
    /**
     * 检查股票列表是否为空
     */
    private boolean isStockListEmpty(String stocks) {
        return stocks == null || stocks.trim().isEmpty();
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

    public List<String> parse() {
        List<String> symbols = new ArrayList<>();
        AppSettingState settings = AppSettingState.getInstance();
        
        // 获取选中的市场名称
        String selectedMarket = (String) stock_market_list.getSelectedItem();
        
        if (StringResUtils.STOCK_US.equals(selectedMarket)) {
            // 美股
            if (settings.showUSStocks) {
                Arrays.stream(settings.usStocks.split("[,; ]")).filter(s -> !s.isEmpty()).forEach(s -> symbols.add("s_us" + s));
            }
        } else if (StringResUtils.STOCK_HK.equals(selectedMarket)) {
            // 港股
            if (settings.showHKStocks) {
                Arrays.stream(settings.hkStocks.split("[,; ]")).filter(s -> !s.isEmpty()).forEach(s -> symbols.add("s_hk" + s));
            }
        } else if (StringResUtils.STOCK_CN.equals(selectedMarket)) {
            // A股
            if (settings.showCNStocks) {
                String cnStocks = settings.shStocks + ";" + settings.szStocks;
                Arrays.stream(cnStocks.split("[,; ]")).filter(s -> !s.isEmpty()).forEach(s -> symbols.add(determineCnMarketPrefix(s) + s));
            }
        } else {
            // 全部或默认
            if (settings.showUSStocks) {
                Arrays.stream(settings.usStocks.split("[,; ]")).filter(s -> !s.isEmpty()).forEach(s -> symbols.add("s_us" + s));
            }
            if (settings.showHKStocks) {
                Arrays.stream(settings.hkStocks.split("[,; ]")).filter(s -> !s.isEmpty()).forEach(s -> symbols.add("s_hk" + s));
            }
            if (settings.showCNStocks) {
                String allCnStocks = settings.shStocks + ";" + settings.szStocks;
                Arrays.stream(allCnStocks.split("[,; ]")).filter(s -> !s.isEmpty()).forEach(s -> symbols.add(determineCnMarketPrefix(s) + s));
            }
        }
        return symbols;
    }

    /**
     * 根据股票代码前缀确定市场前缀
     * 60xxx / 68xxx → 上证 (s_sh)
     * 00xxx / 30xxx → 深证 (s_sz)
     */
    private String determineCnMarketPrefix(String code) {
        if (code.startsWith("60") || code.startsWith("68")) {
            return "s_sh";
        } else if (code.startsWith("00") || code.startsWith("30")) {
            return "s_sz";
        }
        // 默认返回深证（兼容旧代码）
        return "s_sz";
    }
}
