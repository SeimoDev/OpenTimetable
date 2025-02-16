package cn.seimo.lessontable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.seimo.lessontable.R
import cn.seimo.lessontable.databinding.ItemCourseBinding
import cn.seimo.lessontable.databinding.DialogCourseDetailBinding
import cn.seimo.lessontable.model.Course
import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect

class CourseGridAdapter(private val courses: List<Course>) : 
    RecyclerView.Adapter<CourseGridAdapter.CourseViewHolder>() {
    
    // 计算最大节数
    private val maxPeriods = courses
        .flatMap { course -> course.timeString?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList() }
        .maxOrNull() ?: 10  // 如果没有课程数据，默认10节
    
    private val grid = Array(maxPeriods) { Array<Course?>(5) { null } }
    private val coursePeriods = mutableMapOf<Course, List<Int>>()
    
    init {
        // 计算每天最大课程数
        val sortedCourses = courses.sortedWith(
            compareBy({ it.dayOfWeek }, { it.timeString?.split(",")?.firstOrNull()?.toIntOrNull() ?: 0 })
        )
        
        // 按天分组处理课程
        sortedCourses.groupBy { it.dayOfWeek }.forEach { (day, dayCourses) -> 
            val col = day - 1
            processDayCourses(dayCourses, col)
        }
    }

    private fun processDayCourses(dayCourses: List<Course>, col: Int) {
        if (dayCourses.isEmpty()) return
        
        val mergedCourses = mutableListOf<Pair<Course, List<Int>>>()
        var currentGroup = mutableListOf<Course>()
        
        // 分组相同的课程
        dayCourses.forEach { course -> 
            if (currentGroup.isEmpty() || isSameCourse(currentGroup.first(), course)) {
                currentGroup.add(course)
            } else {
                // 处理当前组
                processGroup(currentGroup, col, mergedCourses)
                currentGroup = mutableListOf(course)
            }
        }
        
        // 处理最后一组
        if (currentGroup.isNotEmpty()) {
            processGroup(currentGroup, col, mergedCourses)
        }
        
        // 将合并后的课程放入网格
        mergedCourses.forEach { (course, periods) -> 
            addCourseToGrid(course, periods, col)
        }
    }

    private fun processGroup(group: List<Course>, col: Int, result: MutableList<Pair<Course, List<Int>>>) {
        if (group.isEmpty()) return
        
        // 合并同组课程的时间段
        val allPeriods = group.flatMap { course -> 
            course.timeString?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        }.distinct().sorted()
        
        // 使用组内第一个课程作为代表
        if (allPeriods.isNotEmpty()) {
            result.add(group.first() to allPeriods)
        }
    }

    private fun addCourseToGrid(course: Course, periods: List<Int>, col: Int) {
        coursePeriods[course] = periods
        periods.forEach { period -> 
            val row = period - 1
            // 使用动态最大节数检查边界
            if (row in 0 until maxPeriods && col in 0..4) {
                grid[row][col] = course
            }
        }
    }

    private fun isSameCourse(course1: Course?, course2: Course): Boolean {
        if (course1 == null) return false
        return course1.name == course2.name && 
               course1.teacher == course2.teacher &&
               course1.location == course2.location &&
               course1.dayOfWeek == course2.dayOfWeek
    }

    // 更新网格大小计算
    override fun getItemCount() = maxPeriods * 5

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val row = position / SPANS
        val col = position % SPANS

        if (row >= maxPeriods) {
            val params = holder.binding.root.layoutParams as GridLayoutManager.LayoutParams
            params.height = BASE_HEIGHT
            holder.binding.root.layoutParams = params
            holder.binding.root.visibility = View.INVISIBLE
            return
        }

        val course = grid[row][col]
        
        holder.binding.apply {
            if (course != null) {
                val periods = coursePeriods[course] ?: listOf()

                if (isStartOfSegment(periods, row + 1)) {
                    root.visibility = View.VISIBLE
                    val params = root.layoutParams as GridLayoutManager.LayoutParams
                    params.height = BASE_HEIGHT
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    root.layoutParams = params
                    
                    tvCourseName.text = course.name
                    tvCourseLocation.text = course.location
                    tvTeacher.text = course.teacher
                    tvWeeks.text = "第${course.weeksString}周"

                    root.setOnClickListener {
                        showCourseDetailDialog(root.context, course, periods)
                    }
                } else {
                    val params = root.layoutParams as GridLayoutManager.LayoutParams
                    params.height = BASE_HEIGHT
                    root.layoutParams = params
                    root.visibility = View.INVISIBLE
                }
            } else {
                val params = root.layoutParams as GridLayoutManager.LayoutParams
                params.height = BASE_HEIGHT
                root.layoutParams = params
                root.visibility = View.INVISIBLE
            }
        }
    }

    private fun isStartOfSegment(periods: List<Int>, period: Int): Boolean {
        return periods.contains(period) && !periods.contains(period - 1)
    }

    private fun findAllSegments(periods: List<Int>): List<List<Int>> {
        val segments = mutableListOf<List<Int>>()
        var currentSegment = mutableListOf<Int>()
        
        periods.sorted().forEach { period -> 
            if (currentSegment.isEmpty() || period == currentSegment.last() + 1) {
                currentSegment.add(period)
            } else {
                segments.add(currentSegment)
                currentSegment = mutableListOf(period)
            }
        }
        
        if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment)
        }
        
        return segments
    }

    // 查找包含指定时间的连续段
    private fun findContinuousSegment(periods: List<Int>, currentPeriod: Int): List<Int> {
        if (!periods.contains(currentPeriod)) return emptyList()
        
        val segment = mutableListOf<Int>()
        var current = currentPeriod
        
        // 向前查找连续时间
        while (current - 1 >= 1 && periods.contains(current - 1)) {
            current--
        }
        
        // 从找到的最早时间开始添加连续时间
        while (periods.contains(current)) {
            segment.add(current)
            current++
        }
        
        return segment
    }

    private fun findContinuousSegmentFromPosition(periods: List<Int>, startPeriod: Int): List<Int> {
        val sortedPeriods = periods.sorted()
        val segment = mutableListOf<Int>()
        var current = startPeriod
        
        // 从当前位置开始，找出连续的时间段
        while (current <= maxPeriods && periods.contains(current)) {
            segment.add(current)
            current++
            // 如果下一个时间段不连续，就停止
            if (!periods.contains(current)) break
        }
        
        return segment
    }

    private fun showCourseDetailDialog(context: Context, course: Course, periods: List<Int>) {
        val dialogBinding = DialogCourseDetailBinding.inflate(LayoutInflater.from(context))
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()

        with(dialogBinding) {
            tvDetailCourseName.text = course.name
            tvDetailTeacher.text = course.teacher
            tvDetailLocation.text = course.location
            tvTime.text = "星期${course.dayOfWeek}\n${periods.joinToString(",") { "第${it}节" }}"
            tvWeeks.text = "第${course.weeksString}周"
            tvType.text = course.type ?: "未设置"
            
            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    companion object {
        private const val SPANS = 5
        private const val BASE_HEIGHT = 550  // 增大卡片高度从150调整为200
        private const val GRID_SPACING = 1

        fun setupGrid(recyclerView: RecyclerView) {
            val layoutManager = GridLayoutManager(recyclerView.context, SPANS)
            recyclerView.layoutManager = layoutManager
            
            // 添加网格间距装饰器
            recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(GRID_SPACING, GRID_SPACING, GRID_SPACING, GRID_SPACING)
                }
            })
        }
    }

    class CourseViewHolder(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root)
}
