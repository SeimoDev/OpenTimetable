package cn.seimo.lessontable

import android.content.Context
import androidx.core.content.ContextCompat
import cn.seimo.lessontable.R

object ThemeUtils {
    fun getDynamicNeutral1(context: Context) = ContextCompat.getColor(context, R.color.dynamic_neutral1)
    fun getDynamicNeutral2(context: Context) = ContextCompat.getColor(context, R.color.dynamic_neutral2)
    fun getDynamicAccent1(context: Context) = ContextCompat.getColor(context, R.color.dynamic_accent1)
    fun getDynamicAccent2(context: Context) = ContextCompat.getColor(context, R.color.dynamic_accent2)
}
