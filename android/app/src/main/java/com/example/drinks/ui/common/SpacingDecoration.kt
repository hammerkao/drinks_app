// ui/common/SpacingDecoration.kt
package com.example.drinks.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class SpacingDecoration(
    private val verticalDp: Float = 8f,
    private val horizontalDp: Float = 0f,
    private val includeTop: Boolean = true,
    private val includeBottom: Boolean = true
) : RecyclerView.ItemDecoration() {

    private fun View.dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val pos = parent.getChildAdapterPosition(view)
        val top = if (includeTop && pos == 0) view.dp(verticalDp) else view.dp(verticalDp / 2)
        val bottom = if (includeBottom && pos == state.itemCount - 1) view.dp(verticalDp) else view.dp(verticalDp / 2)
        outRect.set(view.dp(horizontalDp), top, view.dp(horizontalDp), bottom)
    }
}
