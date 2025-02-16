package cn.seimo.lessontable.db

import android.content.Context
import android.database.Cursor
import cn.seimo.lessontable.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CourseDao(context: Context) {
    private val dbHelper = DBHelper(context)

    fun getAllCourses(): Flow<List<Course>> = flow {
        dbHelper.readableDatabase.use { db ->
            val cursor = db.query(
                DBHelper.TABLE_COURSE,
                null,
                null,
                null,
                null,
                null,
                "day_of_week ASC, time ASC"
            )

            val courses = mutableListOf<Course>()
            cursor.use {
                while (it.moveToNext()) {
                    courses.add(it.toCourse())
                }
            }
            emit(courses)
        }
    }.flowOn(Dispatchers.IO)

    private fun Cursor.toCourse() = Course(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        type = getString(getColumnIndexOrThrow("type")),
        teacher = getString(getColumnIndexOrThrow("teacher")),
        location = getString(getColumnIndexOrThrow("classroom")),
        dayOfWeek = getInt(getColumnIndexOrThrow("day_of_week")),
        timeString = getString(getColumnIndexOrThrow("time")),
        weeksString = getString(getColumnIndexOrThrow("weeks"))
    )
}
