package cn.seimo.lessontable.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "lesson_table.db"
        const val DATABASE_VERSION = 2
        const val TABLE_COURSE = "course"
        const val TABLE_TIME_SETTINGS = "time_settings"
        const val TABLE_SETTINGS = "settings"

        // 创建课程表的SQL语句
        private const val SQL_CREATE_COURSE_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_COURSE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT,
                teacher TEXT,
                classroom TEXT,
                day_of_week INTEGER NOT NULL,
                time TEXT,
                weeks TEXT
            )
        """

        // 确保时间设置表的列名与查询一致
        private const val SQL_CREATE_TIME_SETTINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_TIME_SETTINGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                period_number INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                first_week_date TEXT
            )
        """

        // 创建设置表的SQL语句
        private const val SQL_CREATE_SETTINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_SETTINGS (
                max_periods INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 使用事务确保原子性
        db.beginTransaction()
        try {
            // 创建表前先检查是否存在
            db.execSQL(SQL_CREATE_COURSE_TABLE)
            db.execSQL(SQL_CREATE_TIME_SETTINGS_TABLE)
            db.execSQL(SQL_CREATE_SETTINGS_TABLE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 删除旧表并创建新表
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TIME_SETTINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COURSE")
        onCreate(db)
    }

    fun getTimeSettings(): List<TimeSettings> {
        val list = mutableListOf<TimeSettings>()
        val db = readableDatabase
        try {
            val cursor = db.rawQuery("""
                SELECT period_number, start_time, end_time, first_week_date 
                FROM $TABLE_TIME_SETTINGS 
                ORDER BY period_number ASC
            """.trimIndent(), null)

            cursor.use {
                while (it.moveToNext()) {
                    list.add(TimeSettings(
                        periodNumber = it.getInt(0),
                        startTime = it.getString(1),
                        endTime = it.getString(2),
                        firstWeekDate = it.getString(3)
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果表不存在或查询出错，返回空列表
        }
        return list
    }

    fun getFirstWeekDate(): Date? {
        return try {
            readableDatabase.query(
                TABLE_TIME_SETTINGS,
                arrayOf("first_week_date"),
                null, null, null, null, null
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

    data class TimeSettings(
        val periodNumber: Int,
        val startTime: String,
        val endTime: String,
        val firstWeekDate: String? = null  // 添加学期开始时间字段
    )
}
