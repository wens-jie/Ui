package com.wens.ui.layout

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.OverScroller
import androidx.annotation.UiThread
import androidx.customview.widget.ViewDragHelper.INVALID_POINTER
import com.wens.ui.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A layout that horizontally lay out children until the row is filled
 * and then moved to the next line.
 *
 * The following snippet shows how to include a flow layout in your layout XML file:
 *
 * <com.wens.ui.layout.FlowLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *   android:layout_width="match_parent"
 *   android:layout_height="match_parent"
 *   android:paddingLeft="16dp"
 *   android:paddingRight="16dp"
 *   android:gravity="center">
 *
 *   <!-- Include other widget or layout tags here. These are considered
 *           "child views" or "children" of the flow layout -->
 *
 * </com.wens.ui.layout.FlowLayout>
 *
 *
 * To control how flow layout aligns all the views it contains, set a value for
 * {@link com.wens.ui.R.styleable#FlowLayout_android_gravity android:gravity}.  For example, the
 * snippet above sets android:gravity to "center".  The value you set affects
 * both horizontal and vertical alignment of all child views within the single row.
 *
 * When the value of layout_height is match_parent,
 * the height of the current row will be adaptive.
 *
 * When each child view is match_parent,
 * the height of the current row is the height of the highest view when layout_height is warp_content
 *
 * See
 * {@link com.wens.ui.layout.FlowLayout.LayoutParams FlowLayout.LayoutParams}
 * to learn about other attributes you can set on a child view to affect its
 * position and size in the containing linear layout.
 *
 * @attr ref com.wens.ui.R.styleable#FlowLayout_android_gravity
 * @attr ref com.wens.ui.R.styleable#FlowLayout_android_minHeight
 * @attr ref com.wens.ui.R.styleable#FlowLayout_android_maxHeight
 * @attr ref com.wens.ui.R.styleable#FlowLayout_android_minWidth
 * @attr ref com.wens.ui.R.styleable#FlowLayout_android_maxWidth
 */
@UiThread
open class FlowLayout(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "Jie"
    }

    private var mActivePointerId: Int = -1
    private var mIsBeingDragged: Boolean = false
    private val mMinimumVelocity: Int
    private val mMaximumVelocity: Int

    //对齐方式
    var gravity: Int = Gravity.START

    //最小宽度,只有widthMode != MeasureSpec.EXACTLY 时生效
    var minWidth: Int = -1

    //最大宽度,只有widthMode != MeasureSpec.EXACTLY,并且maxChildWidth <= maxWidth 时生效
    var maxWidth: Int = -1

    //最小高度,只有heightMode != MeasureSpec.EXACTLY 时生效
    var minHeight: Int = -1

    //最大高度
    var maxHeight: Int = -1

    var scrollable = true

    var contentHeight: Int = 0
        private set
    var contentWidth: Int = 0
        private set

    //中间量，当前行的view集合
    private var lines = ArrayList<View>()

    //子view集合
    private val views = ArrayList<List<View>>()

    //每一行的高度
    private val heights = ArrayList<Int>()

    //每一行的宽度
    private val widths = ArrayList<Int>()

    //最大的子view宽度
    private var maxChildWidth: Int = 0

    //最小滑动距离
    private val mTouchSlop: Int

    //最后一次拦截事件的x值
    private var lastInterceptX: Float = 0f

    //最后一次拦截事件的y值
    private var lastInterceptY: Float = 0f

    //最后一次触摸事件的y值
    private var lastTouchY: Int = 0

    //最后一次按下事件的y值
    private var lastDownY: Int = 0

    //弹性滑动
    private val scroller = OverScroller(context)

    //惯性控制
    private var velocityTracker: VelocityTracker? = null

    constructor(context: Context?) : this(context, null, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        val a = context?.resources?.obtainAttributes(attrs, R.styleable.FlowLayout)
        if (a != null) {
            val n: Int = a.indexCount
            try {
                for (i in 0 until n) {
                    when (val attr: Int = a.getIndex(i)) {
                        R.styleable.FlowLayout_android_maxHeight -> maxHeight =
                            a.getDimensionPixelSize(
                                attr,
                                -1
                            )


                        R.styleable.FlowLayout_android_minHeight -> minHeight =
                            a.getDimensionPixelSize(
                                attr,
                                -1
                            )

                        R.styleable.FlowLayout_android_maxWidth -> maxWidth =
                            a.getDimensionPixelSize(
                                attr,
                                -1
                            )

                        R.styleable.FlowLayout_android_minWidth -> minWidth =
                            a.getDimensionPixelSize(
                                attr,
                                -1
                            )

                        R.styleable.FlowLayout_android_gravity -> {
                            gravity = a.getInt(attr, Gravity.NO_GRAVITY)
                            if (gravity and Gravity.HORIZONTAL_GRAVITY_MASK == 0)
                                gravity = gravity or Gravity.START
                        }
                    }
                }
            } finally {
                a.recycle()
            }
        }
        val viewConfiguration = ViewConfiguration.get(context)
        mMaximumVelocity = viewConfiguration.scaledMaximumFlingVelocity
        mMinimumVelocity = viewConfiguration.scaledMinimumFlingVelocity
        mTouchSlop = viewConfiguration.scaledPagingTouchSlop
    }


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
            return true
        }
        if (super.onInterceptTouchEvent(ev)) {
            return true
        }
        if (scrollY == 0 && !canScrollVertically(1)) {
            return false
        }
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                val activePointerId: Int = mActivePointerId
                if (activePointerId == INVALID_POINTER) {
                    return mIsBeingDragged
                }
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) {
                    return mIsBeingDragged
                }
                val y = ev.getY(pointerIndex).toInt()
                val yDiff: Int = abs(y - lastTouchY)
                if (yDiff > mTouchSlop && View.SCROLL_AXIS_VERTICAL == 0) {
                    mIsBeingDragged = true
                    lastTouchY = y
                    initVelocityTrackerIfNotExists()
                    velocityTracker?.addMovement(ev)
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = ev.y.toInt()
                mActivePointerId = ev.getPointerId(0)
                initOrResetVelocityTracker()
                velocityTracker?.addMovement(ev)
                scroller.computeScrollOffset()
                mIsBeingDragged = !scroller.isFinished
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
                recycleVelocityTracker()
                if (scroller.springBack(scrollX, scrollY, 0, 0, 0, getScrollRange())) {
                    postInvalidateOnAnimation()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }
        return mIsBeingDragged
    }


    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr
                MotionEvent.ACTION_POINTER_INDEX_SHIFT
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            lastTouchY = ev.getY(newPointerIndex).toInt()
            mActivePointerId = ev.getPointerId(newPointerIndex)
            if (velocityTracker != null) {
                velocityTracker?.clear()
            }
        }
    }

    private fun getScrollRange(): Int {
        return contentHeight - measuredHeight + paddingTop + paddingBottom
    }

    private fun initOrResetVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker?.clear()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker?.recycle()
            velocityTracker = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val safeArea = getScrollRange()
        if (!scrollable || event == null || safeArea < 0) {
            return super.onTouchEvent(event)
        }
        if (velocityTracker == null)
            velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                    scroller.startScroll(0, scrollY, 0, 0)
                }
                lastTouchY = event.y.toInt()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                var dy = lastTouchY - event.y
                lastTouchY = event.y.toInt()
                if (scrollY < -height || scrollY > safeArea + height) {
                    dy = 0f
                }
                scroller.startScroll(0, scroller.finalY, 0, dy.toInt())
                postInvalidateOnAnimation()
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                if (abs(velocityTracker!!.yVelocity) > mMinimumVelocity && scroller.finalY < safeArea && scroller.finalY > 0) {
                    scroller.fling(
                        scrollX,
                        scrollY,
                        0,
                        -velocityTracker!!.yVelocity.roundToInt(),
                        0,
                        0,
                        0,
                        safeArea,
                        0,
                        safeArea / 4
                    )
                } else if (scroller.springBack(scrollX, scrollY, 0, 0, 0, safeArea)) {
                    postInvalidateOnAnimation()
                }
                velocityTracker?.recycle()
                velocityTracker = null
                return if (isClickable && abs(lastDownY - event.y) < mTouchSlop && performClick())
                    true
                else
                    return super.onTouchEvent(event)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                lastTouchY = event.getY(index).toInt()
                mActivePointerId = event.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
                return true
            }
        }
        velocityTracker?.recycle()
        velocityTracker = null
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.computeScrollOffset()) {
            scrollTo(0, scroller.currY)
            postInvalidate()
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.e(TAG, "onLayout: ")
        var lineTop = paddingTop
        var lineWidth: Int
        var left: Int
        var right: Int
        var bottom: Int
        var top: Int
        //布局子view
        for ((index, line) in views.withIndex()) {
            lineWidth = widths[index]
            //处理横向对齐方式
            //默认左对齐
            left = paddingLeft
            when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                //右对齐
                Gravity.RIGHT -> {
                    left = measuredWidth - lineWidth + paddingLeft
                }
                //水平居中
                Gravity.CENTER_HORIZONTAL -> {
                    left = (measuredWidth - lineWidth) / 2 + paddingLeft
                }
            }

            for (childView in line) {
                val lp = childView.layoutParams as LayoutParams
                left += lp.leftMargin
                //处理纵向对齐方式
                //默认顶对齐
                top = lineTop + lp.topMargin
                var gravity = lp.gravity
                //当子view的对齐方式没有设置时，采用当前的对齐方式
                if (gravity == Gravity.NO_GRAVITY)
                    gravity = this.gravity
                //当前对齐方式未设置时，默认顶对齐
                if (gravity and Gravity.VERTICAL_GRAVITY_MASK == 0)
                    gravity = gravity or Gravity.TOP
                when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                    //低对齐
                    Gravity.BOTTOM -> {
                        top = lineTop + heights[index] - lp.bottomMargin - childView.measuredHeight
                    }
                    //垂直居中
                    Gravity.CENTER_VERTICAL -> {
                        top =
                            lineTop + lp.topMargin + (heights[index] - lp.topMargin - lp.bottomMargin - childView.measuredHeight) / 2
                    }
                }
                right = left + childView.measuredWidth
                //子view的宽度为MATCH_PARENT时，独占一行
                if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT)
                    right = measuredWidth - paddingRight - lp.rightMargin
                bottom = top + childView.measuredHeight
                childView.layout(left, top, right, bottom)
                left += childView.measuredWidth + lp.rightMargin
            }
            lineTop += heights[index]
        }
        Log.e(TAG, "onLayout: ${lineTop + paddingBottom}")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        //支持垂直方向滑动,heightMode指定为MeasureSpec.EXACTLY,不对子view高度进行约束
        val parentHeightMeasureSpec: Int =
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        val heightMode = MeasureSpec.getMode(parentHeightMeasureSpec)


        var width: Int
        if (widthMode != MeasureSpec.EXACTLY) {
            //处理minWidth和maxWidth,得到期望宽度
            width = max(
                min(
                    widthSize,
                    if (maxWidth == -1) Int.MAX_VALUE else maxWidth
                ), minWidth
            )
            //测量子view，确定maxChildWidth的值
            val array = measureChildren(widthMode, parentHeightMeasureSpec, widthSize)
            val flowLayoutWidth = array[0]
            if (maxChildWidth > width) {
                //当期望宽度不能满足子view的内容布局时，期望宽度取maxChildWidth,以保证所有的子view都能正常显示
                width = maxChildWidth
                Log.e(TAG, "onMeasure: minChildWidth = $maxChildWidth")
            } else if (width > flowLayoutWidth) {
                //当期望宽度足够满足view尽可能的平铺，期望宽度取flowLayoutWidth,以保证每一行尽可能多的容纳更多的view
                width = flowLayoutWidth
            }
        } else {
            width = widthSize
        }
        //测量子view，确定子view的最终宽度和高度
        val array = measureChildren(widthMode, parentHeightMeasureSpec, width)
        contentWidth = array[0]
        contentHeight = array[1]
        //对match_parent的子view进行再次测量，让其高度为当前行的高度
        for ((i, line) in views.withIndex()) {
            val needAppend = heights[i] == 0
            if (needAppend) {
                for (childView in line) {
                    val lp = childView.layoutParams as LayoutParams
                    if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                        val childWidthMeasureSpec = getChildMeasureSpec(
                            MeasureSpec.makeMeasureSpec(width, widthMode),
                            paddingLeft + paddingRight, lp.width
                        )
                        val childHeightMeasureSpec = getChildMeasureSpec(
                            parentHeightMeasureSpec,
                            paddingTop + paddingBottom, ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        childView.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                        heights[i] = max(
                            childView.measuredHeight + lp.topMargin + lp.bottomMargin,
                            heights[i]
                        )
                    }
                }
                contentHeight += heights[i]
            }

            for (childView in line) {
                val lp = childView.layoutParams as LayoutParams
                if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    val childWidthMeasureSpec = getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(width, widthMode),
                        paddingLeft + paddingRight, lp.width
                    )
                    val childHeightMeasureSpec = getChildMeasureSpec(
                        parentHeightMeasureSpec,
                        paddingTop + paddingBottom,
                        heights[i] - lp.topMargin - lp.bottomMargin
                    )
                    childView.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                }
            }
        }
        //计算期望高度
        val height = min(
            if (maxHeight == -1) Int.MAX_VALUE else maxHeight,
            if (heightMode == MeasureSpec.EXACTLY)
                heightSize
            else
                max(
                    contentHeight,
                    minHeight
                )
        )
        Log.e(TAG, "onMeasure: 最终结果: w = $width h = $height ")
        setMeasuredDimension(width, height)
    }


    /**
     * 以期望的宽高测量view,并且确定maxChildWidth的值。会调用多次。
     * @return 返回测量后期望的宽高 Array<Int> index = 0 为宽度 ,index = 1 为高度
     */
    private fun measureChildren(
        widthMode: Int, heightMeasureSpec: Int,
        expectWidth: Int
    ): Array<Int> {

        var currWidth = paddingLeft + paddingRight
        var currHeight = 0
        var flowLayoutWidth = 0
        var flowLayoutHeight = 0
        maxChildWidth = 0

        views.clear()
        lines.clear()
        heights.clear()
        widths.clear()
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val lp = childView.layoutParams as LayoutParams
            //确定期望宽度下的view的宽高
            measureChild(
                childView,
                MeasureSpec.makeMeasureSpec(expectWidth, widthMode),
                heightMeasureSpec
            )
            var childViewMeasuredWidth = childView.measuredWidth
            if (childView.layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT)
            //宽度为MATCH_PARENT的子view独占一行
                childViewMeasuredWidth =
                    expectWidth - paddingLeft - paddingRight - lp.leftMargin - lp.rightMargin
            else
            //确定maxChildWidth的值
                maxChildWidth = max(
                    maxChildWidth,
                    childViewMeasuredWidth + paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin
                )
            if (currWidth + childViewMeasuredWidth + lp.leftMargin + lp.rightMargin > expectWidth) {
                //当前行不能容纳childView,换行
                views.add(lines)
                lines = ArrayList()
                flowLayoutWidth = max(currWidth, flowLayoutWidth)
                flowLayoutHeight += currHeight
                heights.add(currHeight)
                widths.add(currWidth)
                currWidth = paddingLeft + paddingRight
                currHeight = 0
            }
            lines.add(childView)
            currWidth += childViewMeasuredWidth + lp.leftMargin + lp.rightMargin
            //MATCH_PARENT的view需要特殊处理，不计算其高度
            if (lp.height != ViewGroup.LayoutParams.MATCH_PARENT)
                currHeight =
                    max(currHeight, childView.measuredHeight + lp.topMargin + lp.bottomMargin)
        }
        //将最后一个view加入到views中
        views.add(lines)
        flowLayoutWidth = max(currWidth, flowLayoutWidth)
        flowLayoutHeight += currHeight
        heights.add(currHeight)
        widths.add(currWidth)
        return arrayOf(flowLayoutWidth, flowLayoutHeight)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams && super.checkLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    /**
     * Per-child layout information associated with MarginLayoutParams.
     *
     * See
     * {@link com.wens.ui.R.styleable#FlowLayout_LayoutParams Attributes}
     * for a list of all child view attributes that this class supports.
     *
     * @attr ref com.wens.ui.R.styleable#FlowLayout_LayoutParams_android_layout_gravity
     */
    open class LayoutParams : MarginLayoutParams {

        var gravity: Int = Gravity.START

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {
            val typeArray =
                c?.resources?.obtainAttributes(attrs, R.styleable.FlowLayout_LayoutParams)
            try {
                if (typeArray != null)
                    gravity = typeArray.getInt(
                        R.styleable.FlowLayout_LayoutParams_android_layout_gravity,
                        Gravity.NO_GRAVITY
                    )
            } finally {
                typeArray?.recycle()
            }
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source) {
            if (source is LayoutParams)
                gravity = source.gravity
        }

        constructor(width: Int, height: Int) : super(width, height)
    }
}