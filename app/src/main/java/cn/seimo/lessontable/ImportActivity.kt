package cn.seimo.lessontable

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.appcompat.app.AppCompatActivity
import cn.seimo.lessontable.db.DBHelper
import cn.seimo.lessontable.model.Course
import com.google.android.material.button.MaterialButton
import org.json.JSONArray   // 添加 JSONArray 导入
import org.json.JSONObject
import java.io.InputStream

class ImportActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ImportActivity"
    }

    private lateinit var dbHelper: DBHelper
    private lateinit var jsonPickerLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import) // 确保布局文件正确加载

        dbHelper = DBHelper(this)

        // 使用 ActivityResult API 注册文件选择器
        jsonPickerLauncher = registerForActivityResult(OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                Log.d(TAG, "选中文件的 URI: $uri")
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        importJson(inputStream)
                        inputStream.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "读取 JSON 文件失败", e)
                }
            } else {
                Log.d(TAG, "未选择文件")
            }
        }

        findViewById<MaterialButton>(R.id.btn_select_json).setOnClickListener {
            Log.d(TAG, "btn_select_json 点击")
            selectJsonFile() // 直接调用系统文件选择器
        }
    }

    private fun selectJsonFile() {
        Log.d(TAG, "调用系统文件选择器选择 JSON 文件")
        jsonPickerLauncher.launch(arrayOf("application/json"))
    }

    // 处理读取 JSON 数据、写入数据库并跳转页面
    private fun importJson(inputStream: InputStream) {
        try {
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            processJsonData(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "处理JSON文件失败", e)
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "处理JSON文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun processJsonData(jsonData: String) {
        try {
            val courses = parseCoursesFromJson(jsonData)
            
            // 修正 flatMap 的使用
            val maxPeriods = courses.flatMap { course ->
                course.timeString?.split(",")?.mapNotNull { timeStr ->
                    timeStr.toIntOrNull()
                } ?: emptyList()
            }.maxOrNull() ?: 12

            // 保存课程数据
            saveCourses(courses)

            // 启动时间设置活动，传递最大课程数
            val intent = Intent(this, TimeSettingsActivity::class.java).apply {
                putExtra("maxPeriods", maxPeriods)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "处理JSON文件失败", e)
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "处理JSON文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun parseCoursesFromJson(jsonStr: String): List<Course> {
        val jsonObject = JSONObject(jsonStr)
        val weekdays = listOf("monday", "tuesday", "wednesday", "thursday", "friday")
        val courses = mutableListOf<Course>()

        weekdays.forEachIndexed { index, day ->
            val dayArray = jsonObject.optJSONArray(day)
            Log.d(TAG, "处理 $day 的课程，找到 ${dayArray?.length() ?: 0} 条记录")

            dayArray?.let { array ->
                for (i in 0 until array.length()) {
                    try {
                        val courseObj = array.getJSONObject(i)
                        val courseName = courseObj.getString("name")
                        
                        // 优化周数解析逻辑
                        val weeksList = parseWeeks(courseObj.getJSONArray("weeks"))
                        val timeList = parseTimeSlots(courseObj.getJSONArray("time"))
                        
                        Log.d(TAG, """处理课程: 
                            |名称: $courseName
                            |周数: ${weeksList.joinToString(",")}
                            |时间: ${timeList.joinToString(",")}
                        """.trimMargin())

                        if (weeksList.isNotEmpty() && timeList.isNotEmpty()) {
                            val course = Course(
                                name = courseName,
                                type = courseObj.optString("type", ""),
                                teacher = courseObj.optString("teacher", ""),
                                location = courseObj.optString("classroom", ""),
                                dayOfWeek = index + 1,
                                timeString = timeList.joinToString(","),
                                weeksString = formatWeekString(weeksList)
                            )
                            courses.add(course)
                        } else {
                            Log.e(TAG, "课程 $courseName 的周数或时间段为空")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "处理单个课程时出错", e)
                    }
                }
            }
        }
        return courses
    }

    private fun parseWeeks(weeksArray: JSONArray): List<Int> {
        val weeks = mutableListOf<Int>()
        for (i in 0 until weeksArray.length()) {
            try {
                val weekItem = weeksArray.getString(i)  // 使用 getString 而不是 get
                when {
                    weekItem.contains("-") -> {
                        val (start, end) = weekItem.split("-").map { it.toInt() }
                        weeks.addAll(start..end)
                    }
                    else -> {
                        weeks.add(weekItem.toInt())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析周数出错: ${e.message}")
            }
        }
        return weeks.distinct().sorted()
    }

    private fun parseTimeSlots(timeArray: JSONArray): List<Int> {
        val times = mutableListOf<Int>()
        for (i in 0 until timeArray.length()) {
            try {
                times.add(timeArray.getInt(i))
            } catch (e: Exception) {
                Log.e(TAG, "解析时间段出错: ${e.message}")
            }
        }
        return times.distinct().sorted()
    }

    private fun formatWeekString(weeks: List<Int>): String {
        val ranges = mutableListOf<String>()
        var start = weeks.firstOrNull() ?: return ""
        var prev = start
        
        for (i in 1 until weeks.size) {
            val current = weeks[i]
            if (current != prev + 1) {
                // 结束当前范围
                ranges.add(if (start == prev) "$start" else "$start-$prev")
                start = current
            }
            prev = current
        }
        // 添加最后一个范围
        ranges.add(if (start == prev) "$start" else "$start-$prev")
        
        return ranges.joinToString(",")
    }

    private fun saveCourses(courses: List<Course>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // 删除表内容而不是重新创建表
            db.delete(DBHelper.TABLE_COURSE, null, null)
            db.delete(DBHelper.TABLE_TIME_SETTINGS, null, null)
            db.delete(DBHelper.TABLE_SETTINGS, null, null)

            var totalInserted = 0
            var totalSkipped = 0

            courses.forEach { course ->
                try {
                    val values = ContentValues().apply {
                        put("name", course.name)
                        put("type", course.type)
                        put("teacher", course.teacher)
                        put("classroom", course.location)  // 修改为 location
                        put("day_of_week", course.dayOfWeek)
                        put("time", course.timeString)
                        put("weeks", course.weeksString)
                    }

                    val id = db.insert(DBHelper.TABLE_COURSE, null, values)

                    if (id != -1L) {
                        totalInserted++
                        Log.d(TAG, "成功插入课程: ${course.name}, ID: $id")
                    } else {
                        Log.e(TAG, "插入课程失败: ${course.name}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "处理单个课程时出错", e)
                    totalSkipped++
                }
            }

            db.setTransactionSuccessful()
            Log.d(TAG, "成功导入 $totalInserted 条课程数据，跳过 $totalSkipped 条无效数据")

            runOnUiThread {
                Toast.makeText(this, "成功导入 $totalInserted 条课程数据", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "导入数据失败", e)
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "导入数据失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            db.endTransaction()
        }
    }
}
