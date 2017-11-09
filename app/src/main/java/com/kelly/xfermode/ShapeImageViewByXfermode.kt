package com.kelly.xfermode

import android.content.Context
import android.graphics.*
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import com.kelly.xfermode.svgandroid.SVG
import com.kelly.xfermode.svgandroid.SVGParser
import java.lang.ref.WeakReference

/**
 * 通过PorterDuffXfermode实现的圆角ImageView
 * @author zongkaili
 * @date 2017/11/8
 *
 * 1. PorterDuff.Mode.CLEAR      所绘制不会提交到画布上。
 * 2. PorterDuff.Mode.SRC        显示上层绘制图片
 * 3. PorterDuff.Mode.DST        显示下层绘制图片
 * 4. PorterDuff.Mode.SRC_OVER   正常绘制显示，上下层绘制叠盖。
 * 5. PorterDuff.Mode.DST_OVER   上下层都显示。下层居上显示。
 * 6. PorterDuff.Mode.SRC_IN     取两层绘制交集。显示上层。
 * 7. PorterDuff.Mode.DST_IN     取两层绘制交集。显示下层。
 * 8. PorterDuff.Mode.SRC_OUT    取上层绘制非交集部分。
 * 9. PorterDuff.Mode.DST_OUT    取下层绘制非交集部分。
 * 10. PorterDuff.Mode.SRC_ATOP  取下层非交集部分与上层交集部分
 * 11. PorterDuff.Mode.DST_ATOP  取上层非交集部分与下层交集部分
 * 12. PorterDuff.Mode.XOR       异或：去除两图层交集部分
 * 13. PorterDuff.Mode.DARKEN    取两图层全部区域，交集部分颜色加深
 * 14. PorterDuff.Mode.LIGHTEN   取两图层全部，点亮交集部分颜色
 * 15. PorterDuff.Mode.MULTIPLY  取两图层交集部分叠加后颜色
 * 16. PorterDuff.Mode.SCREEN    取两图层全部区域，交集部分变为透明色
 */

class ShapeImageViewByXfermode(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    private var mPaint: Paint? = null
    private val mXfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    private var mMaskBitmap: Bitmap? = null
    private var mWeakBitmap: WeakReference<Bitmap>? = null
    private lateinit var mDrawCanvas: Canvas
    /**
     * 图片的类型，圆形or圆角
     */
    private val mType: Int
    private val mSvgRawResourceId: Int
    /**
     * 圆角半径
     */
    private val mBorderRadius: Int

    companion object {
        private val TAG = ShapeImageViewByXfermode::class.java.simpleName
        private val BODER_RADIUS_DEFAULT = 0
        val TYPE_CIRCLE = 0
        val TYPE_ROUND = 1
        val TYPE_SVG = 2
    }

    private val mBitmap: Bitmap
        get() {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.BLACK

            when (mType) {
                TYPE_ROUND -> canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), mBorderRadius.toFloat(), mBorderRadius.toFloat(), paint)
                TYPE_CIRCLE -> canvas.drawCircle((width / 2).toFloat(), (width / 2).toFloat(), (width / 2).toFloat(), paint)
                TYPE_SVG -> {
                    if(mSvgRawResourceId > 0) {
                        val svg: SVG = SVGParser.getSVGFromInputStream(context.resources.openRawResource(mSvgRawResourceId),width,height)
                        canvas.drawPicture(svg.picture)
                    }
                }
            }
            return bitmap
        }

    constructor(context: Context) : this(context, null) {
        mPaint = Paint()
        //抗锯齿
        mPaint!!.isAntiAlias = true
    }

    init {
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeImageViewByXfermode)
        mBorderRadius = typedArray.getDimensionPixelSize(R.styleable.ShapeImageViewByXfermode_borderRadius,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BODER_RADIUS_DEFAULT.toFloat(), resources.displayMetrics).toInt())

        mType = typedArray.getInt(R.styleable.ShapeImageViewByXfermode_type, TYPE_CIRCLE)
        mSvgRawResourceId = typedArray.getResourceId(R.styleable.ShapeImageViewByXfermode_svg_resource, 0)
        Log.d(TAG, " mBorderRadius : $mBorderRadius mType: $mType")
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (mType == TYPE_CIRCLE) {
            val width = Math.min(measuredWidth, measuredHeight)
            setMeasuredDimension(width, width)
        }
    }

    override fun onDraw(canvas: Canvas) {
        //1.取出bitmap
        var bitmap: Bitmap? = if (mWeakBitmap == null) null else mWeakBitmap!!.get()
        if (null == bitmap || bitmap.isRecycled) {
            val drawable = drawable
            val dWidth = drawable!!.intrinsicWidth
            val dHeight = drawable.intrinsicHeight

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            var scale = 1.0f
            //2.创建画布
            mDrawCanvas = Canvas(bitmap!!)
            if (mType == TYPE_ROUND) {
                scale = Math.max(width * 1.0f / dWidth, height * 1.0f / dHeight)
            } else {
                scale = width * 1.0f / Math.min(dWidth, dHeight)
            }
            drawable.setBounds(0, 0, (dWidth * scale).toInt(), (dHeight * scale).toInt())
            drawable.draw(mDrawCanvas)
            if (mMaskBitmap == null || mMaskBitmap!!.isRecycled) {
                mMaskBitmap = mBitmap
            }
            mPaint!!.reset()
            mPaint!!.isFilterBitmap = false
            mPaint!!.xfermode = mXfermode
            //4.绘制形状
            mDrawCanvas.drawBitmap(mMaskBitmap!!, 0f, 0f, mPaint)
            mPaint!!.xfermode = null
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            mWeakBitmap = WeakReference(bitmap)
            return
        }
        mPaint!!.xfermode = null
        canvas.drawBitmap(bitmap, 0f, 0f, mPaint)
    }

    override fun invalidate() {
        mWeakBitmap = null
        if (mMaskBitmap != null) {
            mMaskBitmap!!.isRecycled
            mMaskBitmap = null
        }
        super.invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

}