package com.example.possystembw.ui.ViewModel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R

class SectionHeaderDecoration(
    private val context: Context,
    private val title: String
) : RecyclerView.ItemDecoration() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        textSize = context.resources.getDimensionPixelSize(R.dimen.text_size_medium).toFloat()
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        if (parent.childCount > 0) {
            val firstChild = parent.getChildAt(0)
            c.drawText(
                title,
                parent.paddingLeft.toFloat(),
                firstChild.top - 10f,
                paint
            )
        }
    }
}