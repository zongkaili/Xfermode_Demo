# Xfermode_Demo
通过PorterDuffXfermode实现的圆角、圆形和svg图形样式的ImageView

## 效果图
![screen](https://github.com/zongkaili/Xfermode_Demo/blob/master/screen1.jpg?raw=true)
![screen](https://github.com/zongkaili/Xfermode_Demo/blob/master/screen2.jpg?raw=true)
![screen](https://github.com/zongkaili/Xfermode_Demo/blob/master/effect.gif?raw=true)

## PorterDuff的各个模式以及对应的效果图
Tips: 此demo只用到了其中的DST_IN模式
 * 1. PorterDuff.Mode.CLEAR ------ 所有绘制不会提交到画布上。
 * 2. PorterDuff.Mode.SRC ------ 显示上层绘制图片
 * 3. PorterDuff.Mode.DST ------ 显示下层绘制图片
 * 4. PorterDuff.Mode.SRC_OVER ------ 正常绘制显示，上下层绘制叠盖。
 * 5. PorterDuff.Mode.DST_OVER ------ 上下层都显示。下层居上显示。
 * 6. PorterDuff.Mode.SRC_IN ------ 取两层绘制交集。显示上层。
 * 7. PorterDuff.Mode.DST_IN ------ 取两层绘制交集。显示下层。
 * 8. PorterDuff.Mode.SRC_OUT ------ 取上层绘制非交集部分。
 * 9. PorterDuff.Mode.DST_OUT ------ 取下层绘制非交集部分。
 * 10. PorterDuff.Mode.SRC_ATOP ------ 取下层非交集部分与上层交集部分
 * 11. PorterDuff.Mode.DST_ATOP ------ 取上层非交集部分与下层交集部分
 * 12. PorterDuff.Mode.XOR ------ 异或：去除两图层交集部分
 * 13. PorterDuff.Mode.DARKEN ------ 取两图层全部区域，交集部分颜色加深
 * 14. PorterDuff.Mode.LIGHTEN ------ 取两图层全部，点亮交集部分颜色
 * 15. PorterDuff.Mode.MULTIPLY ------ 取两图层交集部分叠加后颜色
 * 16. PorterDuff.Mode.SCREEN ------ 取两图层全部区域，交集部分变为透明色
 
 ![pic](https://github.com/zongkaili/Xfermode_Demo/blob/master/pic.jpg?raw=true)
 
## 实现过程
-----
0.首先定义style
-----
```xml
<resources>
  <attr name="borderRadius" format="dimension"/>
    <attr name="type" format="integer">
        <enum name="circle" value="0"/>
        <enum name="round" value="1"/>
        <enum name="svg" value="2"/>
    </attr>
    <attr name="svg_resource" format="reference" />

    <declare-styleable name="ShapeImageViewByXfermode">
        <attr name="borderRadius"/>
        <attr name="type"/>
        <attr name="svg_resource"/>
    </declare-styleable>
</resources>
```
1.获取自定义属性并写进成员变量
-----
```kotlin
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
```

2.onDraw()绘制过程
-----
```kotlin
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
```
3.绘制外层圆形或者圆角的bitmap
-----
```kotlin
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
```

Tips: 此demo的svg资源来自[CustomShapeImageView](https://github.com/MostafaGazar/CustomShapeImageView)
