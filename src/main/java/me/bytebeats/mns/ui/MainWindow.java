package me.bytebeats.mns.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import me.bytebeats.mns.OnSymbolSelectedListener;
import me.bytebeats.mns.tool.StringResUtils;
import org.jetbrains.annotations.NotNull;

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
        Content indicesContent = contentFactory.createContent(indicesWindow.getJPanel(), StringResUtils.INDICES, true);
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
        Content stockDetailContent = contentFactory.createContent(stockDetailWindow.getJPanel(), StringResUtils.STOCK_DETAIL, true);
        toolWindow.getContentManager().addContent(stockDetailContent);

        // 保存 ContentManager 引用，用于后续刷新显示/隐藏
        this.contentManager = toolWindow.getContentManager();

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
     * 刷新所有窗口的数据（Profile 切换时调用）
     */
    public void refreshAllWindows() {
        // 刷新股票窗口
        stockWindow.onInit();
        
        // 刷新基金窗口
        fundWindow.onInit();
        
        // 刷新加密货币窗口
        digitalCurrencyWindow.onInit();
        
        // 刷新核心指数窗口
        indicesWindow.onInit();
        
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
