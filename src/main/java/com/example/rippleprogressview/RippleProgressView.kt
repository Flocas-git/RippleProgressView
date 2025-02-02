package com.example.rippleprogressview

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class RippleProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var animatorSet: AnimatorSet? = null

    private var radius = 0f // 圆形半径
    private var waterLevel = 100f // 水位高度
    private var progress = 100f // 当前进度（0-100）
    private var change = 0f // 波浪相位变化

    private var period = 0f // 波浪周期
    private var swing = 0f // 波浪振幅
    private var originalPeriod: Float = 0f

    private var pointOneY: FloatArray = floatArrayOf() // 波浪1的Y坐标
    private var pointTwoY: FloatArray = floatArrayOf() // 波浪2的Y坐标
    private var circleY: FloatArray = floatArrayOf() // 圆形Y坐标

    private val rippleColor = Color.BLUE // 波浪颜色
    private val backgroundColor = Color.LTGRAY // 背景颜色

    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = minOf(w, h) / 2f

        waterLevel = if (progress >= 100f) radius else radius * (progress / 100f)
        period = (2 * PI / radius).toFloat()
        swing = radius / 10f

        val arraySize = ceil(2 * radius).toInt()
        pointOneY = FloatArray(arraySize)
        pointTwoY = FloatArray(arraySize)
        circleY = FloatArray(arraySize)
        originalPeriod = period // 保存原始周期
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制背景圆
        paint.color = backgroundColor
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 更新波浪和圆形坐标
        updateWaveAndCircle()

        // 绘制水波纹
        drawWater(canvas)

    }

    private fun updateWaveAndCircle() {
        val x = radius.toInt()
        val y = x

        for (i in pointOneY.indices) {
            val oneY = (swing * 0.8 * sin(period * i * 0.5 + change) + waterLevel).toFloat()
            val twoY = ((swing - 5) * 0.8 * sin(period * i * 0.7 + change + 5) + waterLevel).toFloat()

            if (waterLevel < radius) {
                circleY[i] = (-sqrt(radius * radius - (i - x) * (i - x)) + y).toFloat()
                pointOneY[i] = max(circleY[i], oneY)
                pointTwoY[i] = max(circleY[i], twoY)
            } else {
                circleY[i] = (sqrt(radius * radius - (i - x) * (i - x)) + y).toFloat()
                pointOneY[i] = min(circleY[i], oneY)
                pointTwoY[i] = min(circleY[i], twoY)
            }
        }
    }

    private fun drawWater(canvas: Canvas) {
        paint.color = rippleColor
        paint.alpha = 80

        for (i in pointOneY.indices) {
            val temp = if (waterLevel < radius) 2 * radius - circleY[i] else circleY[i]

            // 画波浪1
            canvas.drawLine(i.toFloat(), pointOneY[i], i.toFloat(), temp, paint)

            // 画波浪2
            paint.alpha = 100
            canvas.drawLine(i.toFloat(), pointTwoY[i], i.toFloat(), temp, paint)
        }

        paint.alpha = 255 // 重置透明度
    }

    /**
     * * 设置进度百分比
     * @param progress 进度值，范围为 0 到 100
     */
    fun setProgress(progress: Float) {
        // 限制进度范围
        this.progress = progress.coerceIn(0f, 100f)
        // 更新水位高度,并限制输入参数（0f,100f）
        waterLevel = radius * (this.progress / 100f)
        // 触发重绘
        invalidate()
    }

    fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 2 * PI.toFloat()).apply {
            duration = 1300L //波浪速度
            repeatCount = ValueAnimator.INFINITE  //动画循环次数=无限
            interpolator = LinearInterpolator()
            addUpdateListener {
                change = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        animator?.end()
    }

    private var initialProgress = 100f // 记录动画开始前的初始进度

    init {
        // 添加点击监听
        setOnClickListener {
            startDropAndRiseAnimation()
        }
    }

    /**
     * 触发水面下降再回升的动画
     */
    fun startDropAndRiseAnimation() {

        animatorSet?.cancel() // 取消之前的动画

        initialProgress = progress // 保存初始进度

        var previousFraction = 0f

        // 第一阶段：降到 100%
        val dropAnimator = ValueAnimator.ofFloat(progress, 100f).apply {
            duration = 700L // 下降耗时 1.5 秒
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                setProgress(it.animatedValue as Float)
            }
        }


        //第二阶段：停顿
        val pauseAnimator = ValueAnimator.ofFloat(100f, 100f).apply {
            duration = 100L // 停顿 0.1 秒
            interpolator = LinearInterpolator()
        }

        // 第三阶段：加速
        val accelerateAnimator = ValueAnimator.ofFloat(100f, 100f).apply {
            duration = 3000L // 加速 3 秒
            interpolator = LinearInterpolator()

            addUpdateListener {
                // 计算加速因子（可以根据需要调整）
                val speedFactor = 5f // 加速 5 倍
                // 根据动画进度计算加速后的 change
                change += speedFactor * (it.animatedFraction - previousFraction) * 2 * PI.toFloat()
                invalidate()
            }
        }

        // 第四阶段：回升到初始进度
        val riseAnimator = ValueAnimator.ofFloat(100f, initialProgress).apply {
            duration = 900L // 回升耗时 0.9 秒
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                setProgress(it.animatedValue as Float)
            }
        }

        // 按顺序执行动画
        AnimatorSet().apply {
            playSequentially(dropAnimator, pauseAnimator,accelerateAnimator,riseAnimator)
            start()
        }

        // 用于记录上一次的动画进度

    }
}