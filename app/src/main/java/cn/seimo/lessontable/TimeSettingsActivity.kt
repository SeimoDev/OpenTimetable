package cn.seimo.lessontable

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import cn.seimo.lessontable.db.DBHelper
import java.util.Calendar

class TimeSettingsActivity : AppCompatActivity() {
    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_settings)
        dbHelper = DBHelper(this)

        // 获取最大课程数和现有数据
        val maxPeriods = getMaxPeriodsFromDb()
        val existingSettings = dbHelper.getTimeSettings()

        // 设置学期开始时间
        existingSettings.firstOrNull()?.firstWeekDate?.let { date ->
            findViewById<EditText>(R.id.et_semester_start_date).setText(date)
        }

        // 动态创建课程时间设置
        val container = findViewById<LinearLayout>(R.id.layout_class_times)
        for (i in 1..maxPeriods) {
            val timeRow = layoutInflater.inflate(R.layout.item_time_setting, container, false)
            timeRow.findViewById<TextView>(R.id.tv_period_number).text = "第${i}节课"
            
            // 填充现有数据
            val setting = existingSettings.find { it.periodNumber == i }
            if (setting != null) {
                timeRow.findViewById<EditText>(R.id.et_start_time).setText(setting.startTime)
                timeRow.findViewById<EditText>(R.id.et_end_time).setText(setting.endTime)
            }
            
            setupTimePickers(timeRow)
            container.addView(timeRow)
        }

        // 为上课和下课时间 EditText 设置点击事件显示 TimePickerDialog
        val etStartTime = findViewById<EditText>(R.id.et_start_time)
        val etEndTime = findViewById<EditText>(R.id.et_end_time)
        etStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                etStartTime.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
        etEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                etEndTime.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // 为学期起始日期 EditText 设置点击事件显示 DatePickerDialog
        val etSemesterStartDate = findViewById<EditText>(R.id.et_semester_start_date)
        etSemesterStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                // 注意：month从0开始，需要加1
                etSemesterStartDate.setText("$year-${month + 1}-$dayOfMonth")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            if (validateInputs()) {
                saveAllTimeSettings(maxPeriods)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun setupTimePickers(timeRow: View) {
        timeRow.findViewById<EditText>(R.id.et_start_time).setOnClickListener {
            showTimePicker(it as EditText)
        }
        timeRow.findViewById<EditText>(R.id.et_end_time).setOnClickListener {
            showTimePicker(it as EditText)
        }
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                editText.setText(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun validateInputs(): Boolean {
        // 检查所有输入是否完整
        val container = findViewById<LinearLayout>(R.id.layout_class_times)
        for (i in 0 until container.childCount) {
            val timeRow = container.getChildAt(i)
            val startTime = timeRow.findViewById<EditText>(R.id.et_start_time).text.toString()
            val endTime = timeRow.findViewById<EditText>(R.id.et_end_time).text.toString()
            if (startTime.isEmpty() || endTime.isEmpty()) {
                return false
            }
        }
        return findViewById<EditText>(R.id.et_semester_start_date).text.isNotEmpty()
    }

    private fun saveAllTimeSettings(maxPeriods: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // 清除旧数据
            db.delete(DBHelper.TABLE_TIME_SETTINGS, null, null)
            db.delete(DBHelper.TABLE_SETTINGS, null, null)

            val container = findViewById<LinearLayout>(R.id.layout_class_times)
            
            // 保存实际课程数量
            val periodCountValues = ContentValues().apply {
                put("max_periods", maxPeriods)
            }
            db.insert(DBHelper.TABLE_SETTINGS, null, periodCountValues)

            // 保存每节课的时间设置
            for (i in 0 until container.childCount) {
                val timeRow = container.getChildAt(i)
                val startTimeText = timeRow.findViewById<EditText>(R.id.et_start_time).text.toString()
                val endTimeText = timeRow.findViewById<EditText>(R.id.et_end_time).text.toString()
                
                val values = ContentValues().apply {
                    put("period_number", i + 1)
                    put("start_time", startTimeText)
                    put("end_time", endTimeText)
                    put("first_week_date", findViewById<EditText>(R.id.et_semester_start_date).text.toString())
                }
                
                val result = db.insert(DBHelper.TABLE_TIME_SETTINGS, null, values)
                if (result == -1L) {
                    throw Exception("保存第${i + 1}节课时间设置失败")
                }
            }
            
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
    }

    private fun getMaxPeriodsFromDb(): Int {
        val db = dbHelper.readableDatabase
        return try {
            // 先尝试从课程表获取最大节数
            var maxPeriods = 0
            db.rawQuery("SELECT time FROM ${DBHelper.TABLE_COURSE}", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val timeStr = cursor.getString(0)
                    timeStr?.split(",")?.mapNotNull { it.toIntOrNull() }?.maxOrNull()?.let {
                        if (it > maxPeriods) maxPeriods = it
                    }
                }
            }
            if (maxPeriods == 0) maxPeriods = 12 // 默认值
            maxPeriods
        } catch (e: Exception) {
            Log.e("TimeSettings", "获取最大课程数失败", e)
            12 // 出错时使用默认值
        }
    }
}
