package cn.seimo.lessontable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.seimo.lessontable.R
import cn.seimo.lessontable.model.Course

class CourseAdapter(private val courses: List<Course>) : 
    RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseName: TextView = view.findViewById(R.id.tv_course_name)
        val location: TextView = view.findViewById(R.id.tv_course_location)
        val teacher: TextView = view.findViewById(R.id.tv_teacher)
        val weeks: TextView = view.findViewById(R.id.tv_weeks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        
        // 确保视图高度固定
        val layoutParams = view.layoutParams
        layoutParams.height = parent.context.resources.getDimensionPixelSize(R.dimen.course_card_height)
        view.layoutParams = layoutParams
        
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.courseName.text = course.name
        holder.location.text = course.location
        holder.teacher.text = course.teacher
        holder.weeks.text = course.weeksString
    }

    override fun getItemCount() = courses.size
}
