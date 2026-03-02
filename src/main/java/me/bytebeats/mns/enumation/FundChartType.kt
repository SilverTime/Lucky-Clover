package me.bytebeats.mns.enumation

enum class FundChartType(val description: String) {
    EstimatedNetWorth("估算净值"),
    DailyGrowth("日增长率"),
    WeeklyGrowth("周增长率"),
    MonthlyGrowth("月增长率"),
    QuarterlyGrowth("季度增长率"),
    YearlyGrowth("年度增长率"),
    AllTimeGrowth("成立以来增长率")
}