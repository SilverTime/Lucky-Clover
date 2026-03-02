package me.bytebeats.mns.tool;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 市场开市时间判断工具类
 * 用于判断A股、港股、美股是否处于开市时间内
 */
public class MarketTimeUtils {

    // A股开市时间（北京时区）
    private static final LocalTime CN_MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime CN_MARKET_CLOSE = LocalTime.of(15, 0);
    private static final LocalTime CN_PRE_OPEN = LocalTime.of(9, 15);
    private static final LocalTime CN_PRE_CLOSE = LocalTime.of(9, 25);

    // 港股开市时间（北京时间，与A股相同）
    private static final LocalTime HK_MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime HK_MARKET_CLOSE = LocalTime.of(16, 0);
    private static final LocalTime HK_PRE_OPEN = LocalTime.of(9, 0);
    private static final LocalTime HK_PRE_CLOSE = LocalTime.of(9, 30);

    // 美股开市时间（美国东部时间）
    private static final LocalTime US_MARKET_OPEN_ET = LocalTime.of(9, 30);
    private static final LocalTime US_MARKET_CLOSE_ET = LocalTime.of(16, 0);

    // 北京时区
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    // 美国东部时区
    private static final ZoneId US_EASTERN_ZONE = ZoneId.of("America/New_York");

    /**
     * 判断当前是否为A股开市时间（包括集合竞价时间）
     */
    public static boolean isCNMarketOpen() {
        LocalDateTime now = LocalDateTime.now(CN_ZONE);
        LocalTime currentTime = now.toLocalTime();
        int dayOfWeek = now.getDayOfWeek().getValue();

        // 周末休市
        if (dayOfWeek >= 6) {
            return false;
        }

        // 集合竞价时间: 9:15 - 9:25
        if (!currentTime.isBefore(CN_PRE_OPEN) && !currentTime.isAfter(CN_PRE_CLOSE)) {
            return true;
        }

        // 连续竞价时间: 9:30 - 11:30, 13:00 - 15:00
        boolean morningSession = !currentTime.isBefore(CN_MARKET_OPEN) && !currentTime.isAfter(LocalTime.of(11, 30));
        boolean afternoonSession = !currentTime.isBefore(LocalTime.of(13, 0)) && !currentTime.isAfter(CN_MARKET_CLOSE);

        return morningSession || afternoonSession;
    }

    /**
     * 判断当前是否为港股开市时间（包括集合竞价时间）
     */
    public static boolean isHKMarketOpen() {
        LocalDateTime now = LocalDateTime.now(CN_ZONE);
        LocalTime currentTime = now.toLocalTime();
        int dayOfWeek = now.getDayOfWeek().getValue();

        // 周末休市
        if (dayOfWeek >= 6) {
            return false;
        }

        // 集合竞价时间: 9:00 - 9:30
        if (!currentTime.isBefore(HK_PRE_OPEN) && !currentTime.isAfter(HK_PRE_CLOSE)) {
            return true;
        }

        // 早市: 9:30 - 12:00, 午市: 13:00 - 16:00
        boolean morningSession = !currentTime.isBefore(HK_MARKET_OPEN) && !currentTime.isAfter(LocalTime.of(12, 0));
        boolean afternoonSession = !currentTime.isBefore(LocalTime.of(13, 0)) && !currentTime.isAfter(HK_MARKET_CLOSE);

        return morningSession || afternoonSession;
    }

    /**
     * 判断当前是否为美股开市时间
     * 考虑夏令时和冬令时
     */
    public static boolean isUSMarketOpen() {
        // 获取当前美国东部时间
        ZonedDateTime nowET = ZonedDateTime.now(US_EASTERN_ZONE);
        LocalTime currentTime = nowET.toLocalTime();
        int dayOfWeek = nowET.getDayOfWeek().getValue();

        // 周末休市
        if (dayOfWeek >= 6) {
            return false;
        }

        // 常规交易时间: 9:30 - 16:00（美国东部时间）
        return !currentTime.isBefore(US_MARKET_OPEN_ET) && !currentTime.isAfter(US_MARKET_CLOSE_ET);
    }

    /**
     * 判断当前是否为美股夏令时
     */
    public static boolean isUSDaylightSavingTime() {
        ZonedDateTime nowET = ZonedDateTime.now(US_EASTERN_ZONE);
        return nowET.getZone().getRules().isDaylightSavings(nowET.toInstant());
    }

    /**
     * 获取美股开市时间的北京时间（用于调试显示）
     */
    public static String getUSMarketTimeInCN() {
        boolean isDst = isUSDaylightSavingTime();
        LocalTime openTime = US_MARKET_OPEN_ET;
        LocalTime closeTime = US_MARKET_CLOSE_ET;

        if (isDst) {
            // 夏令时：东部时间 + 12 = 北京时间
            openTime = openTime.plusHours(12);
            closeTime = closeTime.plusHours(12);
        } else {
            // 冬令时：东部时间 + 13 = 北京时间
            openTime = openTime.plusHours(13);
            closeTime = closeTime.plusHours(13);
        }

        return String.format("夏令时: %s - %s, 冬令时: %s - %s",
                US_MARKET_OPEN_ET.plusHours(12), US_MARKET_CLOSE_ET.plusHours(12),
                US_MARKET_OPEN_ET.plusHours(13), US_MARKET_CLOSE_ET.plusHours(13));
    }
}