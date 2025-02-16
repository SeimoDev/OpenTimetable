package cn.seimo.lessontable

import android.app.Application
import com.google.android.material.color.DynamicColors

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 最先应用动态颜色
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
