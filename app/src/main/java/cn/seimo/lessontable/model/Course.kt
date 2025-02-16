package cn.seimo.lessontable.model

data class Course(
    val id: Long = 0,
    val name: String,
    val location: String,
    val teacher: String,
    val type: String,
    val dayOfWeek: Int,
    val timeString: String,
    val weeksString: String,
    val weeks: List<Int> = parseWeeksString(weeksString)
) {
    companion object {
        fun parseWeeksString(weeksString: String): List<Int> {
            return weeksString.split(",").mapNotNull { week ->
                when {
                    week.contains("-") -> {
                        val (start, end) = week.split("-").map { it.toInt() }
                        (start..end).toList()
                    }
                    else -> listOf(week.toInt())
                }
            }.flatten()
        }
    }
}
