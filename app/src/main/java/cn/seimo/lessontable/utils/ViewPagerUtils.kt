package cn.seimo.lessontable.utils

import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView

object ViewPagerUtils {
    fun setViewPagerSpeed(viewPager: ViewPager2, duration: Int) {
        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(viewPager) as RecyclerView

            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, touchSlop * 4) // 减少滑动敏感度

            val layoutAnimatorField = ViewPager2::class.java.getDeclaredField("mLayoutManager")
            layoutAnimatorField.isAccessible = true
            val layoutManager = layoutAnimatorField.get(viewPager)
            val scroller = layoutManager::class.java.getDeclaredField("mScroller")
            scroller.isAccessible = true
            val interpolator = scroller.get(layoutManager)

            val scrollerField = interpolator::class.java.getDeclaredField("mDuration")
            scrollerField.isAccessible = true
            scrollerField.set(interpolator, duration) // 设置动画时长
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
