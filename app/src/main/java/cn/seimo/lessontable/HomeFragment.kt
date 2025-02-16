package cn.seimo.lessontable

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import cn.seimo.lessontable.adapter.CourseGridAdapter
import cn.seimo.lessontable.adapter.WeekPagerAdapter
import cn.seimo.lessontable.databinding.FragmentHomeBinding
import cn.seimo.lessontable.db.CourseDao
import cn.seimo.lessontable.db.DBHelper
import cn.seimo.lessontable.model.Course
import cn.seimo.lessontable.utils.ViewPagerUtils  // 添加这一行
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.view.GestureDetector

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var courseAdapter: CourseGridAdapter
    private var currentWeek = 1
    private var maxWeek = 20
    private val calendar = Calendar.getInstance()
    private lateinit var gestureDetector: GestureDetector
    private lateinit var weekPagerAdapter: WeekPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calculateCurrentWeekAndDay()
        setupWeekPager()
        setupWeekControls()
        setupTabLayout()
        loadCourses()
    }

    private fun setupWeekPager() {
        weekPagerAdapter = WeekPagerAdapter(
            currentWeek = currentWeek,
            maxWeek = maxWeek,
            onWeekChanged = { week: Int ->
                currentWeek = week
                updateWeekDisplay()
            }
        )
        
        binding.weekPager.apply {
            adapter = weekPagerAdapter
            setCurrentItem(currentWeek - 1, false)
            
            // 设置动画速度为800毫秒（默认为250毫秒）
            ViewPagerUtils.setViewPagerSpeed(this, 1600)
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentWeek = position + 1
                    updateWeekDisplay()
                }
            })
        }
    }

    private fun setupWeekControls() {
        // 使用 ViewPager2 的 setCurrentItem 方法来实现平滑切换
        binding.btnBack.setOnClickListener {
            if (currentWeek > 1) {
                binding.weekPager.setCurrentItem(currentWeek - 2, true) // -2是因为position从0开始，且要切换到上一周
            }
        }

        binding.btnNext.setOnClickListener {
            if (currentWeek < maxWeek) {
                binding.weekPager.setCurrentItem(currentWeek, true) // 切换到下一周
            }
        }

        updateWeekDisplay()
    }

    private fun setupTabLayout() {
        val weekdays = listOf("周一", "周二", "周三", "周四", "周五")
        binding.weekDayTabs.removeAllTabs()

        val firstWeekDate = getFirstWeekDate()
        if (firstWeekDate != null) {
            val calendar = Calendar.getInstance().apply {
                time = firstWeekDate
                add(Calendar.WEEK_OF_YEAR, currentWeek - 1)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }

            weekdays.forEach { weekday ->
                val customTab = LayoutInflater.from(requireContext())
                    .inflate(R.layout.custom_tab_layout, null)
                customTab.findViewById<TextView>(R.id.tv_weekday).apply { 
                    text = weekday 
                }
                customTab.findViewById<TextView>(R.id.tv_date).apply {
                    text = "${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
                }
                
                binding.weekDayTabs.addTab(binding.weekDayTabs.newTab().setCustomView(customTab))
                calendar.add(Calendar.DAY_OF_WEEK, 1)
            }
        }

        val today = Calendar.getInstance()
        val dayOfWeek = today.get(Calendar.DAY_OF_WEEK) - 2
        if (dayOfWeek in 0..4) {
            binding.weekDayTabs.getTabAt(dayOfWeek)?.select()
        }

        binding.weekDayTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun getFirstWeekDate(): Date? {
        val db = DBHelper(requireContext())
        return try {
            db.readableDatabase.query(
                DBHelper.TABLE_TIME_SETTINGS,
                arrayOf("first_week_date"),
                null,
                null,
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateStr = cursor.getString(0)
                    SimpleDateFormat("yyyy-M-d", Locale.getDefault()).parse(dateStr)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateWeekDisplay() {
        binding.tvWeek.text = "第${currentWeek}周"
        setupTabLayout()
    }

    private fun loadCourses() {
        val courseDao = CourseDao(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            courseDao.getAllCourses().collectLatest { courses ->
                weekPagerAdapter.updateCourses(courses)
            }
        }
    }

    private fun calculateCurrentWeekAndDay() {
        val firstWeekDate = getFirstWeekDate()
        if (firstWeekDate != null) {
            val today = Calendar.getInstance()
            val firstDay = Calendar.getInstance().apply { time = firstWeekDate }
            
            val diffInMillis = today.timeInMillis - firstDay.timeInMillis
            val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
            
            if (diffInDays < 0) {
                currentWeek = 1
                showOutOfTermDialog("当前日期在学期开始前")
            } else {
                currentWeek = (diffInDays / 7) + 1
                if (currentWeek > maxWeek) {
                    currentWeek = 1
                    showOutOfTermDialog("当前日期已超出本学期")
                }
            }
        }
    }

    private fun showOutOfTermDialog(message: String) {
        context?.let {
            androidx.appcompat.app.AlertDialog.Builder(it)
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
