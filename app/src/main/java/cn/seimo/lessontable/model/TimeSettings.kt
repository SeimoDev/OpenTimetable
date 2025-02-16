package cn.seimo.lessontable.model

data class TimeSettings(
    val id: Long = 0,
    val period: Int,
    val startTime: String,
    val endTime: String
)
