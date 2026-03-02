package me.bytebeats.mns.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import me.bytebeats.mns.OnSymbolSelectedListener;
import me.bytebeats.mns.tool.StringResUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MainWindow implements ToolWindowFactory, OnSymbolSelectedListener {
    private final CoreIndicesWindow indicesWindow = new CoreIndicesWindow();
    private final StockWindow stockWindow = new StockWindow();
    private final FundWindow fundWindow = new FundWindow();
    private final DigitalCurrencyWindow digitalCurrencyWindow = new DigitalCurrencyWindow();
    private final StockDetailWindow stockDetailWindow = new StockDetailWindow();

    // 改为实例变量，避免多窗口冲突
    private Content stockContent;
    private Content fundContent;
    private Content cryptoContent;
    private Content indicesContent;
    private Content stockDetailContent;

    // 保存 ContentManager 引用，用于刷新显示/隐藏
    private ContentManager contentManager;

    // 保存 MainWindow 实例供外部调用（按 Project 区分）
    private static final java.util.Map<Project, MainWindow> instances = new java.util.HashMap<>();

    public MainWindow() {
    }

    /**
     * 获取指定 Project 对应的 MainWindow 实例
     */
    public static MainWindow getInstance(Project project) {
        return instances.get(project);
    }
    
    /**
     * 获取当前活跃 Project 的 MainWindow 实例
     * 如果没有活跃项目，返回第一个可用的实例
     */
    public static MainWindow getInstance() {
        Project[] openProjects = com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            MainWindow window = instances.get(project);
            if (window != null) {
                return window;
            }
        }
        // 如果没有活跃项目，返回第一个可用的实例
        if (!instances.isEmpty()) {
            return instances.values().iterator().next();
        }
        return null;
    }
    
    public static void registerInstance(Project project, MainWindow window) {
        instances.put(project, window);
    }
    
    public static void unregisterInstance(Project project) {
        instances.remove(project);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 注册当前实例
        registerInstance(project, this);
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        AppSettingState settings = AppSettingState.getInstance();

        // 核心指数始终显示
        indicesContent = contentFactory.createContent(indicesWindow.getJPanel(), StringResUtils.INDICES, true);
        toolWindow.getContentManager().addContent(indicesContent);

        // 股票分组
        stockContent = contentFactory.createContent(stockWindow.getJPanel(), StringResUtils.STOCK, true);
        toolWindow.getContentManager().addContent(stockContent);

        // 基金分组
        fundContent = contentFactory.createContent(fundWindow.getJPanel(), StringResUtils.FUNDS, true);
        toolWindow.getContentManager().addContent(fundContent);

        // 加密货币分组
        cryptoContent = contentFactory.createContent(digitalCurrencyWindow.getJPanel(), StringResUtils.CRYPTO_CURRENCIES, true);
        toolWindow.getContentManager().addContent(cryptoContent);

        // 股票详情始终显示
        stockDetailContent = contentFactory.createContent(stockDetailWindow.getJPanel(), StringResUtils.STOCK_DETAIL, true);
        toolWindow.getContentManager().addContent(stockDetailContent);

        // 保存 ContentManager 引用，用于后续刷新显示/隐藏
        this.contentManager = toolWindow.getContentManager();

        // 添加 ContentManagerListener 监听 tab 切换
        this.contentManager.addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentAdded(@NotNull ContentManagerEvent event) {
            }

            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
            }

            @Override
            public void contentRemoveQuery(@NotNull ContentManagerEvent event) {
            }

            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                Content selectedContent = event.getContent();
                if (selectedContent == null) {
                    return;
                }
                
                // 根据选中的 Content 触发对应的数据请求
                if (selectedContent == indicesContent) {
                    indicesWindow.requestData();
                } else if (selectedContent == stockContent) {
                    stockWindow.requestData();
                } else if (selectedContent == fundContent) {
                    fundWindow.requestData();
                } else if (selectedContent == cryptoContent) {
                    digitalCurrencyWindow.requestData();
                }
                // stockDetailContent 不需要自动请求数据
            }
        });

        // 根据设置显示/隐藏各分组
        updateContentVisibility(settings);
        
        // 延迟隐藏 ToolWindow，使用 SwingUtilities.invokeLater 确保在 UI 线程空闲时执行
        // 这样可以避免启动时"闪一下"的问题
        SwingUtilities.invokeLater(() -> {
            toolWindow.hide(null);
        });
    }

    /**
     * 根据设置更新内容显示/隐藏
     */
    public static void updateContentVisibility(AppSettingState settings) {
        // 获取 ToolWindow 实例并更新内容显示
        // 这里需要通过其他方式通知刷新，因为 ToolWindowFactory 不持有 ToolWindow 引用
        // 实际刷新逻辑在 SettingWindow 中调用
    }

    /**
     * 刷新 ToolWindow 内容的显示/隐藏状态
     */
    public static void refreshToolWindowContent() {
        AppSettingState settings = AppSettingState.getInstance();
        // 通过反射或服务获取 MainWindow 实例来更新内容
        // 这里简化处理：通知各 Window 刷新数据
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        indicesWindow.setOnSymbolSelectedListener(this);
        stockWindow.setOnSymbolSelectedListener(this);
        stockWindow.onInit();
        indicesWindow.onInit();
        fundWindow.onInit();
        digitalCurrencyWindow.onInit();
        stockDetailWindow.onInit();

        // 初始根据设置显示/隐藏内容
        refreshContentVisibility(toolWindow.getContentManager());
        
        // 初始化时请求当前选中 tab 的数据
        ContentManager contentManager = toolWindow.getContentManager();
        if (contentManager != null) {
            Content selectedContent = contentManager.getSelectedContent();
            if (selectedContent != null) {
                if (selectedContent == indicesContent) {
                    indicesWindow.requestData();
                } else if (selectedContent == stockContent) {
                    stockWindow.requestData();
                } else if (selectedContent == fundContent) {
                    fundWindow.requestData();
                } else if (selectedContent == cryptoContent) {
                    digitalCurrencyWindow.requestData();
                }
            }
        }
    }

    /**
     * 刷新内容显示/隐藏（实例方法）
     */
    public void refreshContentVisibility() {
        if (contentManager == null) {
            return;
        }
        refreshContentVisibility(contentManager);
    }

    /**
     * 刷新内容显示/隐藏（带 ContentManager 参数）
     */
    public void refreshContentVisibility(ContentManager contentManager) {
        if (contentManager == null) {
            return;
        }
        AppSettingState settings = AppSettingState.getInstance();

        // 股票分组：美股、港股、A股 任一显示则显示
        boolean showStock = settings.showUSStocks || settings.showHKStocks || settings.showCNStocks;
        toggleContent(contentManager, stockContent, showStock);

        // 基金分组
        toggleContent(contentManager, fundContent, settings.showFunds);

        // 加密货币分组
        toggleContent(contentManager, cryptoContent, settings.showCrypto);
    }

    /**
     * 刷新所有窗口的数据（配置保存后调用）
     * 重新初始化 UI 并刷新数据，确保配置变更立即生效
     */
    public void refreshAllWindows() {
        // 刷新股票窗口 UI
        stockWindow.onInit();
        // 立即刷新股票数据
        stockWindow.requestData();
        
        // 刷新基金窗口 UI
        fundWindow.onInit();
        // 立即刷新基金数据
        fundWindow.requestData();
        
        // 刷新加密货币窗口 UI
        digitalCurrencyWindow.onInit();
        // 立即刷新加密货币数据
        digitalCurrencyWindow.requestData();
        
        // 刷新核心指数窗口 UI
        indicesWindow.onInit();
        // 立即刷新核心指数数据
        indicesWindow.requestData();
        
        // 刷新 ToolWindow 内容的显示/隐藏
        refreshContentVisibility();
    }

    /**
     * 获取 ContentManager 引用
     */
    public ContentManager getContentManager() {
        return contentManager;
    }

    /**
     * 根据设置显示/隐藏指定的内容
     */
    private void toggleContent(ContentManager contentManager, Content targetContent, boolean show) {
        if (targetContent == null) {
            return;
        }

        boolean isInManager = isContentInManager(contentManager, targetContent);
        if (show && !isInManager) {
            // 需要显示但当前不在内容管理器中，添加回去
            contentManager.addContent(targetContent);
        } else if (!show && isInManager) {
            // 需要隐藏但当前在内容管理器中，移除
            contentManager.removeContent(targetContent, true);
        }
    }

    /**
     * 检查内容是否在内容管理器中
     */
    private boolean isContentInManager(ContentManager contentManager, Content content) {
        for (Content c : contentManager.getContents()) {
            if (c == content) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSelected(String symbol) {
        if (symbol != null && symbol.equals(stockDetailWindow.getSymbol())) {
            return;
        }
        stockDetailWindow.setSymbol(symbol);
    }
}
