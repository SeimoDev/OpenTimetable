package cn.seimo.lessontable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.seimo.lessontable.R
import cn.seimo.lessontable.model.Course

class WeekPagerAdapter(
    private var currentWeek: Int,
    private val maxWeek: Int,
    private val onWeekChanged: (Int) -> Unit
) : RecyclerView.Adapter<WeekPagerAdapter.WeekViewHolder>() {

    private var courseList: List<Course> = emptyList()

    inner class WeekViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseGrid: RecyclerView = view.findViewById(R.id.coursesGrid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_page, parent, false)
        return WeekViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        val weekNumber = position + 1
        CourseGridAdapter.setupGrid(holder.courseGrid)
        holder.courseGrid.adapter = CourseGridAdapter(getCoursesForWeek(weekNumber))
    }

    override fun getItemCount(): Int = maxWeek

    private fun getCoursesForWeek(week: Int): List<Course> {
        return courseList.filter { course ->
            course.weeksString?.split(",")?.any { weekStr ->
                when {
                    weekStr.contains("-") -> {
                        val (start, end) = weekStr.split("-").map { it.toInt() }
                        week in start..end
                    }
                    else -> weekStr.toInt() == week
                }
            } ?: false
        }
    }

    fun updateCourses(courses: List<Course>) {
        courseList = courses
        notifyDataSetChanged()
    }
}
