package cn.seimo.lessontable

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import cn.seimo.lessontable.databinding.ActivityMainBinding
import cn.seimo.lessontable.db.DBHelper
import com.google.android.material.color.DynamicColors // 新增导入

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        // 申请动态颜色（仅适用于 API 31+ 设备）
        DynamicColors.applyToActivitiesIfAvailable(application)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        // 检查是否存在课程数据或学期开始日期为空，若满足任一条件则启动导入流程和时间设置
        if (!isDataImported() || isSemesterStartDateEmpty()) {
            startActivity(Intent(this, ImportActivity::class.java))
        }

        // 修改右下角悬浮按钮为设置按钮，跳转到 TimeSettingsActivity
        binding.fab.apply {
            setImageResource(R.drawable.ic_settings)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, TimeSettingsActivity::class.java))
            }
        }
    }

    private fun isDataImported(): Boolean {
        try {
            val db = dbHelper.readableDatabase
            // 先检查表是否存在
            val tableCheckCursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND (name='${DBHelper.TABLE_COURSE}' OR name='${DBHelper.TABLE_TIME_SETTINGS}')",
                null
            )
            val existingTables = mutableSetOf<String>()
            tableCheckCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    existingTables.add(cursor.getString(0))
                }
            }

            // 如果有任何表不存在，返回false
            if (!existingTables.containsAll(listOf(DBHelper.TABLE_COURSE, DBHelper.TABLE_TIME_SETTINGS))) {
                return false
            }

            // 检查数据
            var hasData = false
            db.rawQuery("SELECT COUNT(*) FROM ${DBHelper.TABLE_COURSE}", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    hasData = cursor.getInt(0) > 0
                }
            }

            return hasData
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // 新增方法：检测学期开始日期是否为空
    private fun isSemesterStartDateEmpty(): Boolean {
        try {
            val db = dbHelper.readableDatabase
            db.rawQuery("SELECT start_date FROM ${DBHelper.TABLE_TIME_SETTINGS} LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val startDate = cursor.getString(0)
                    return startDate.isNullOrEmpty()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}