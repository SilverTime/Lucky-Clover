package me.bytebeats.mns.enumation

enum class StockChartType(val type: String, val description: String) {
    Minute("min", "分时图"),
    Daily("daily", "日K线"),
    Weekly("weekly", "周K线"),
    Monthly("monthly", "月K线")
}