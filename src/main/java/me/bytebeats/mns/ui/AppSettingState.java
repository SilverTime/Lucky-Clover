package me.bytebeats.mns.ui;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.xmlb.XmlSerializerUtil;
import me.bytebeats.mns.tool.NotificationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@State(
        name = "me.bytebeats.mns.ui.AppSettingState",
        storages = {@Storage("mns_plugin_setting.xml")}
)
public class AppSettingState implements PersistentStateComponent<AppSettingState> {

    public final static boolean IS_RED_RISE = true;
    public final static boolean IS_HIDDEN_MODE = false;
    public final static String US_STOCKS = "AAPL;TSLA;NFLX;MSFT";
    public final static String HK_STOCKS = "00981;09988;09618";
    public final static String SH_STOCKS = "600036";
    public final static String SZ_STOCKS = "002352";
    public final static String DAILY_FUNDS = "320003;002621;519674";
    public final static String ALL_INDICES = "usDJI;usIXIC;usINX;usNDX;hkHSI;hkHSTECH;hkHSCEI;hkHSCCI;sh000001;sh588090;sz399001;sz399006;sh000300;sh000016;sz399903;sh000011;sz399103;sz399330";
    public final static String STOCK_SYMBOL = "sh600519";
    public final static String FUND_SYMBOL = "570008";
    public final static String CRYPTO_CURRENCIES = "BTC;DOGE";
    public final static String DEFAULT_PROFILE_NAME = "默认";

    /**
     * Profile 表示一整套可切换的配置方案。
     *
     * <p>之所以引入 Profile 而不是简单再加一堆字段，是为了：
     * <ul>
     *   <li>支持多套“自选列表 + 刷新频率 + 显示样式”的组合配置；</li>
     *   <li>方便在设置界面里新增/复制/重命名/删除整套配置，而不是单个字段；</li>
     *   <li>序列化到 {@code mns_plugin_setting.xml} 时结构清晰，便于今后扩展。</li>
     * </ul>
     */
    public static class Profile {
        public String name = DEFAULT_PROFILE_NAME;

        public boolean isRedRise = IS_RED_RISE;
        public boolean isHiddenMode = IS_HIDDEN_MODE;
        // 隐秘模式下是否显示拼音名称，默认不显示
        public boolean showPinyinName = false;

        // 显示/隐藏各分组开关，默认全部勾选（显示）
        public boolean showUSStocks = true;
        public boolean showHKStocks = true;
        public boolean showCNStocks = true;
        public boolean showFunds = true;
        public boolean showCrypto = true;

        public String usStocks = US_STOCKS;
        public String hkStocks = HK_STOCKS;
        public String shStocks = SH_STOCKS;
        public String szStocks = SZ_STOCKS;
        public String dailyFunds = DAILY_FUNDS;
        public String cryptoCurrencies = CRYPTO_CURRENCIES;
        public String coreIndices = ALL_INDICES;

        public int indicesFrequency = 5;
        public int stockFrequency = 3;
        public int fundFrequency = 20;
        public int cryptoFrequency = 5;

        public Profile() {
        }

        public Profile(Profile other) {
            if (other == null) return;
            this.name = other.name;
            this.isRedRise = other.isRedRise;
            this.isHiddenMode = other.isHiddenMode;
            this.showPinyinName = other.showPinyinName;
            this.showUSStocks = other.showUSStocks;
            this.showHKStocks = other.showHKStocks;
            this.showCNStocks = other.showCNStocks;
            this.showFunds = other.showFunds;
            this.showCrypto = other.showCrypto;
            this.usStocks = other.usStocks;
            this.hkStocks = other.hkStocks;
            this.shStocks = other.shStocks;
            this.szStocks = other.szStocks;
            this.dailyFunds = other.dailyFunds;
            this.cryptoCurrencies = other.cryptoCurrencies;
            this.coreIndices = other.coreIndices;
            this.indicesFrequency = other.indicesFrequency;
            this.stockFrequency = other.stockFrequency;
            this.fundFrequency = other.fundFrequency;
            this.cryptoFrequency = other.cryptoFrequency;
        }
    }

    public boolean isRedRise = true;
    public boolean isHiddenMode = false;
    // 隐秘模式下是否显示拼音名称，默认不显示
    public boolean showPinyinName = false;

    // 显示/隐藏各分组开关，默认全部勾选（显示）
    public boolean showUSStocks = true;
    public boolean showHKStocks = true;
    public boolean showCNStocks = true;
    public boolean showFunds = true;
    public boolean showCrypto = true;

    public String usStocks = "AAPL;TSLA;NFLX;MSFT";
    public String hkStocks = "00981;09988;09618";
    public String shStocks = "600036";
    public String szStocks = "002352";
    public String dailyFunds = "320003;002621;519674";
    public String cryptoCurrencies = "BTC;DOGE";
    public String coreIndices = ALL_INDICES;
    public String stockSymbol = "usTSLA";
    public String fundSymbol = "570008";
    public int indicesFrequency = 5;
    public int stockFrequency = 3;
    public int fundFrequency = 20;
    public int cryptoFrequency = 5;

    // 股票复权类型：0-不复权，1-前复权，2-后复权
    public int stockAdjustedType = 0;

    public List<Profile> profiles = new ArrayList<>();
    public String activeProfileName = DEFAULT_PROFILE_NAME;

    public String localVersion = "0.0.0";
    public String version = "1.8.4";

    @Override
    public void initializeComponent() {
        PersistentStateComponent.super.initializeComponent();
        version = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId("me.bytebeats.mns"))).getVersion();
        if (isNewVersion()) {
            updateLocalVersion();
            NotificationUtil.infoToolWindow("修复了新浪财经接口被禁止调用的问题; 近 4 年来的工程层面最大的 Upgrade! ");
        }
    }

    private boolean isNewVersion() {
        String[] subLocalVersions = localVersion.split("\\.");
        String[] subVersions = version.split("\\.");
        if (subLocalVersions.length != subVersions.length) {
            return false;
        }
        int idx = 0;
        do {
            int localVersion = Integer.parseInt(subLocalVersions[idx]);
            int version = Integer.parseInt(subVersions[idx]);
            if (version > localVersion) {
                return true;
            }
            idx++;
        } while (idx < subLocalVersions.length);
        return false;
    }

    private void updateLocalVersion() {
        localVersion = version;
    }

    public static AppSettingState getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingState.class);
    }

    public void reset() {
        isRedRise = IS_RED_RISE;
        isHiddenMode = IS_HIDDEN_MODE;
        usStocks = US_STOCKS;
        hkStocks = HK_STOCKS;
        shStocks = SH_STOCKS;
        szStocks = SZ_STOCKS;
        dailyFunds = DAILY_FUNDS;
        cryptoCurrencies = CRYPTO_CURRENCIES;
        coreIndices = ALL_INDICES;
        stockSymbol = STOCK_SYMBOL;
        fundSymbol = FUND_SYMBOL;

        profiles = new ArrayList<>();
        Profile p = new Profile();
        p.name = DEFAULT_PROFILE_NAME;
        profiles.add(p);
        activeProfileName = DEFAULT_PROFILE_NAME;
    }

    @Nullable
    @Override
    public AppSettingState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AppSettingState appSettingState) {
        XmlSerializerUtil.copyBean(appSettingState, this);
        ensureProfiles();
        applyActiveProfileToFields();
    }

    public void ensureProfiles() {
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        if (profiles.isEmpty()) {
            Profile p = new Profile();
            // 首次从老版本升级：把"单一配置"迁移到默认 Profile 中，避免老用户丢配置。
            p.name = DEFAULT_PROFILE_NAME;
            p.isRedRise = isRedRise;
            p.isHiddenMode = isHiddenMode;
            p.showPinyinName = showPinyinName;
            p.showUSStocks = showUSStocks;
            p.showHKStocks = showHKStocks;
            p.showCNStocks = showCNStocks;
            p.showFunds = showFunds;
            p.showCrypto = showCrypto;
            p.usStocks = usStocks;
            p.hkStocks = hkStocks;
            p.shStocks = shStocks;
            p.szStocks = szStocks;
            p.dailyFunds = dailyFunds;
            p.cryptoCurrencies = cryptoCurrencies;
            p.coreIndices = coreIndices;
            p.indicesFrequency = indicesFrequency;
            p.stockFrequency = stockFrequency;
            p.fundFrequency = fundFrequency;
            p.cryptoFrequency = cryptoFrequency;
            profiles.add(p);
            activeProfileName = DEFAULT_PROFILE_NAME;
        }
        if (activeProfileName == null || activeProfileName.trim().isEmpty()) {
            activeProfileName = profiles.get(0).name;
        }
    }

    @Nullable
    public Profile findProfile(String name) {
        if (profiles == null || profiles.isEmpty() || name == null) return null;
        for (Profile p : profiles) {
            if (p != null && name.equals(p.name)) {
                return p;
            }
        }
        return null;
    }

    @NotNull
    public Profile getActiveProfile() {
        ensureProfiles();
        Profile p = findProfile(activeProfileName);
        if (p == null) {
            p = profiles.get(0);
            activeProfileName = p.name;
        }
        return p;
    }

    public void applyActiveProfileToFields() {
        // 将当前激活 Profile 的内容"投影"到旧字段上，保证老代码只读这些字段也能正常工作。
        Profile p = getActiveProfile();
        isRedRise = p.isRedRise;
        isHiddenMode = p.isHiddenMode;
        showPinyinName = p.showPinyinName;
        showUSStocks = p.showUSStocks;
        showHKStocks = p.showHKStocks;
        showCNStocks = p.showCNStocks;
        showFunds = p.showFunds;
        showCrypto = p.showCrypto;
        usStocks = p.usStocks;
        hkStocks = p.hkStocks;
        shStocks = p.shStocks;
        szStocks = p.szStocks;
        dailyFunds = p.dailyFunds;
        cryptoCurrencies = p.cryptoCurrencies;
        coreIndices = p.coreIndices;
        indicesFrequency = p.indicesFrequency;
        stockFrequency = p.stockFrequency;
        fundFrequency = p.fundFrequency;
        cryptoFrequency = p.cryptoFrequency;
    }

    public void updateActiveProfileFromFields() {
        // 将当前字段上的值回写到激活的 Profile，用于部分增量更新（例如添加基金代码）。
        Profile p = getActiveProfile();
        p.isRedRise = isRedRise;
        p.isHiddenMode = isHiddenMode;
        p.showPinyinName = showPinyinName;
        p.showUSStocks = showUSStocks;
        p.showHKStocks = showHKStocks;
        p.showCNStocks = showCNStocks;
        p.showFunds = showFunds;
        p.showCrypto = showCrypto;
        p.usStocks = usStocks;
        p.hkStocks = hkStocks;
        p.shStocks = shStocks;
        p.szStocks = szStocks;
        p.dailyFunds = dailyFunds;
        p.cryptoCurrencies = cryptoCurrencies;
        p.coreIndices = coreIndices;
        p.indicesFrequency = indicesFrequency;
        p.stockFrequency = stockFrequency;
        p.fundFrequency = fundFrequency;
        p.cryptoFrequency = cryptoFrequency;
    }

    public void setActiveProfileName(String name) {
        if (name == null || name.trim().isEmpty()) return;
        activeProfileName = name.trim();
        ensureProfiles();
        applyActiveProfileToFields();
    }

    public boolean addFundSymbol(String fundSymbol) {
        if (dailyFunds.contains(fundSymbol)) {
            return false;
        }
        String df = dailyFunds.trim();
        if (df.endsWith(";")) {
            dailyFunds = df + fundSymbol;
        } else {
            dailyFunds = df + ";" + fundSymbol;
        }
        updateActiveProfileFromFields();
        return true;
    }

    public boolean deleteFundSymbol(String fundSymbol) {
        if (!dailyFunds.contains(fundSymbol)) {
            return false;
        }
        String df = dailyFunds.trim();
        dailyFunds = df.replace(fundSymbol, "");
        updateActiveProfileFromFields();
        return true;
    }
}
