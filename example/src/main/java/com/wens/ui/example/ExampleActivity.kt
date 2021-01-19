package com.wens.ui.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import com.wens.ui.layout.FlowLayout
import com.wens.ui.util.ScreenInfo
import kotlinx.android.synthetic.main.activity_example.*

class ExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        gravityTop.setOnClickListener {
            flowLayout.gravity = flowLayout.gravity and Gravity.HORIZONTAL_GRAVITY_MASK or Gravity.TOP
            updateUi()
        }
        gravityBottom.setOnClickListener {
            flowLayout.gravity = flowLayout.gravity and Gravity.HORIZONTAL_GRAVITY_MASK or Gravity.BOTTOM
            updateUi()
        }
        gravityStart.setOnClickListener {
            flowLayout.gravity = flowLayout.gravity and Gravity.VERTICAL_GRAVITY_MASK or Gravity.START
            updateUi()
        }
        gravityEnd.setOnClickListener {
            flowLayout.gravity = flowLayout.gravity and Gravity.VERTICAL_GRAVITY_MASK or Gravity.END
            updateUi()
        }

        gravityCenterHorizontal.setOnClickListener {
            flowLayout.gravity = flowLayout.gravity and Gravity.VERTICAL_GRAVITY_MASK or Gravity.CENTER_HORIZONTAL
            updateUi()
        }
        gravityCenterVertical.setOnClickListener {
            flowLayout.gravity = flowLayout.gravity and Gravity.HORIZONTAL_GRAVITY_MASK or Gravity.CENTER_VERTICAL
            updateUi()
        }
        minWidth.setOnClickListener {
            flowLayout.minWidth = ScreenInfo.dp2px(this, 100)
            updateUi()
        }
        maxWidth.setOnClickListener {
            flowLayout.maxWidth = ScreenInfo.dp2px(this, 200)
            updateUi()
        }

        minHeight.setOnClickListener {
            flowLayout.minHeight = ScreenInfo.dp2px(this, 100)
            updateUi()
        }
        maxHeight.setOnClickListener {
            flowLayout.maxHeight = ScreenInfo.dp2px(this, 200)
            updateUi()
        }

        padding0.setOnClickListener {
            flowLayout.setPadding(0)
            updateUi()
        }

        padding10.setOnClickListener {
            flowLayout.setPadding(ScreenInfo.dp2px(this, 10))
            updateUi()
        }

        childPadding0.setOnClickListener {
            for (i in 0 until flowLayout.childCount) {
                flowLayout.getChildAt(i).setPadding(0)
            }
            updateUi()
        }
        childPadding10.setOnClickListener {
            for (i in 0 until flowLayout.childCount) {
                flowLayout.getChildAt(i).setPadding(ScreenInfo.dp2px(this, 10))
            }
            updateUi()
        }

        childMargin0.setOnClickListener {
            for (i in 0 until flowLayout.childCount) {
                (flowLayout.getChildAt(i).layoutParams as FlowLayout.LayoutParams).setMargins(0)
            }
            updateUi()
        }
        childMargin10.setOnClickListener {
            for (i in 0 until flowLayout.childCount) {
                (flowLayout.getChildAt(i).layoutParams as FlowLayout.LayoutParams).setMargins(
                    ScreenInfo.dp2px(this, 10)
                )
            }
            updateUi()
        }
        randomChildWidth.setOnClickListener {
            for (i in 0 until flowLayout.childCount) {
                flowLayout.getChildAt(i).layoutParams.width =
                    listOf(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ScreenInfo.dp2px(this,50),
                        ScreenInfo.dp2px(this,100)
                    ).random()
            }
            updateUi()
        }

        randomChildHeight.setOnClickListener {
            for (i in 0 until flowLayout.childCount) {
                flowLayout.getChildAt(i).layoutParams.height =
                    listOf(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ScreenInfo.dp2px(this,50),
                        ScreenInfo.dp2px(this,100)
                    ).random()
            }
            updateUi()
        }
    }

    private fun updateUi() {
        flowLayout.requestLayout()
        flowLayout.invalidate()
    }
}