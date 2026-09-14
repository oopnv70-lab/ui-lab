package com.oopnv70.uilab.glass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi

// =====================================================================
// 真·液态玻璃 · 第三步：GlassView（抓背景 + 跑 shader）
// =====================================================================
// 为什么必须是自定义 View，而不是纯 Compose：
//
//   Compose 的 Modifier.blur() 只能模糊**自己**，拿不到身后的内容。
//   而液态玻璃的全部意义在于「折射身后的东西」。
//
//   Android 里唯一能拿到"身后内容"的路径是：
//   在 View 的 dispatchDraw 里，先把父容器画到一块离屏 Bitmap 上，
//   再把这块 Bitmap 当作贴图喂给 shader。
//
// 执行流程（每帧）：
//   ① 让父容器把「不含本 View 的内容」渲染到 offscreen Canvas
//   ② 从 offscreen 里裁出「本 View 所在位置」的那块 = backdrop
//   ③ 把 backdrop + SDF 传给 RuntimeShader
//   ④ 用 Paint(shader) 把自己画到真实 Canvas 上
//
// ⚠️ 关键性能设计（必须保留）：
//   SDF 是静态的 → **只在尺寸变化时算一次**，此后每帧只是 shader 合成。
//   这是本方案能跑到可用帧率的前提。
// =====================================================================

@RequiresApi(Build.VERSION_CODES.S)
class GlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ---- shader ----
    private val shader: RuntimeShader = createLiquidGlassShader()

    // ---- SDF（缓存，形状不变就不重算）----
    private var sdfBitmap: Bitmap? = null

    // ---- 离屏画布：用来抓「身后的内容」----
    private var offscreen: Bitmap? = null
    private var offscreenCanvas: Canvas? = null

    // ---- backdrop：从离屏内容里裁出来的、本 View 位置的那一块 ----
    private var backdrop: Bitmap? = null

    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    // =================================================================
    // 可调参数（从 GlassDefaults 初始化，外部可覆盖）
    // =================================================================
    var refract: Float = GlassDefaults.REFRACT
    var curve: Float = GlassDefaults.CURVE
    var chroma: Float = GlassDefaults.CHROMA
    var specular: Float = GlassDefaults.SPECULAR
    var specularSharp: Float = GlassDefaults.SPECULAR_SHARP
    var fresnel: Float = GlassDefaults.FRESNEL
    var tint: Float = GlassDefaults.TINT
    var tintColor: FloatArray = floatArrayOf(0.92f, 0.95f, 1.0f)
    var brightness: Float = 1.04f

    /**
     * 背景模糊半径（像素）。
     *
     * ⚠️ 目前**不使用** Paint/RenderEffect 级模糊：
     *    setRenderEffect 在本项目编译环境下无法解析，已移除。
     *    液态玻璃的重点是「折射」而非「模糊」，模糊可后续在 shader 内实现。
     *    这个字段保留作参数占位，避免 API 变动影响调用方。
     */
    var backdropBlur: Float = 0f

    /** SDF 最大影响距离，即"玻璃厚度"。 */
    var sdfMaxDistance: Int = 60

    // =================================================================
    // 尺寸变化 → 重建 SDF 与离屏缓冲
    // =================================================================
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        // 离屏画布：装父容器的内容
        offscreen?.recycle()
        offscreen = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        offscreenCanvas = Canvas(offscreen!!)

        // backdrop：最终喂给 shader 的那块
        backdrop?.recycle()
        backdrop = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        // 尺寸变了，原来抓的背景已经对不上位置了，必须重抓
        backdropDirty = true

        rebuildSdf(w, h)
    }

    /**
     * 生成 SDF。
     *
     * ⚠️ 用「圆角矩形」而不是「本 View 的真实绘制结果」当形状源。
     *
     * 为什么要这样：真实绘制结果需要在 onSizeChanged 时先画一次自己，
     * 而那时背景还没准备好，容易拿到空图 → SDF 全 0 → 没有折射。
     * 我们所有的玻璃元素（按钮/导航栏/标题栏）本质都是圆角矩形，
     * 直接按 View 尺寸 + 圆角半径构造一个 mask，稳定且可控。
     */
    private var cornerRadiusDp: Float = 999f   // 默认全圆角（胶囊/圆形）

    private fun rebuildSdf(w: Int, h: Int) {
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(mask)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()   // 不透明 = 玻璃内部
        }
        val r = cornerRadiusDp * resources.displayMetrics.density
            .coerceAtMost(minOf(w, h) / 2f)
        val rect = android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat())
        c.drawRoundRect(rect, r, r, p)

        sdfBitmap?.recycle()
        sdfBitmap = generateSdf(mask, sdfMaxDistance)
        mask.recycle()
    }

    /** 外部设置圆角（dp 值）。会触发 SDF 重建。 */
    fun setCornerRadiusDp(dp: Float) {
        if (cornerRadiusDp == dp) return
        cornerRadiusDp = dp
        if (width > 0 && height > 0) rebuildSdf(width, height)
        invalidate()
    }

    // =================================================================
    // 核心：抓背景 + 画玻璃
    // =================================================================
    //
    // ⚠️ 这里用 onDraw 而不是 dispatchDraw。
    //
    // 为什么：dispatchDraw 是"画子 View"的钩子，在它里面调
    //   parent.draw(offscreenCanvas) 会让整棵树（含 Compose 的
    //   AndroidComposeView）重新绘制一遍，拿到的是未完成的缓冲 →
    //   之前实机上看到的就是"一片黑"。
    //
    // onDraw 的时机是"本 View 正在被绘制"，此时共享缓冲里已经有
    //   它身后画完的内容。我们从**根 View** 抓一次整屏位图，
    //   再按自己在屏幕上的位置裁出那一块当作 backdrop。
    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return

        val bd = backdrop ?: return
        val sdf = sdfBitmap ?: return

        // ---- ① 抓背景 ----
        captureBackdropIfNeeded(bd)

        // ---- ② 传 uniform ----
        shader.setFloatUniform("uSize", width.toFloat(), height.toFloat())
        shader.setFloatUniform("uRefract", refract)
        shader.setFloatUniform("uCurve", curve)
        shader.setFloatUniform("uChroma", chroma)
        shader.setFloatUniform("uSpecular", specular)
        shader.setFloatUniform("uSpecularSharp", specularSharp)
        shader.setFloatUniform("uFresnel", fresnel)
        shader.setFloatUniform("uTint", tint)
        shader.setFloatUniform("uTintColor", tintColor[0], tintColor[1], tintColor[2])
        shader.setFloatUniform("uBrightness", brightness)

        // 两张纹理：背景 + SDF
        val tile = Shader.TileMode.CLAMP
        shader.setInputShader("uBackdrop", BitmapShader(bd, tile, tile))
        shader.setInputShader("uSdf", BitmapShader(sdf, tile, tile))

        // ---- ③ 画玻璃 ----
        // onDraw 的 canvas 已经以本 View 左上角为原点，
        // 所以直接画 0..w / 0..h 即可，不需要 translate。
        glassPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glassPaint)
    }

    /**
     * 抓取「本 View 身后」的内容到 [backdrop]。
     *
     * 策略（按可靠性从高到低）：
     *   1) 如果上层通过 [setBackdropSource] 提供了背景 Bitmap，直接用
     *   2) 否则尝试从 root view 渲染一次（仅在需要重抓时）
     *   3) 都不行 → 用当前主题的中性色填充，**绝不留下黑色**
     *
     * ⚠️ 第 3 条是关键：即使抓不到背景，玻璃也必须是"半透明的乳白"，
     *    而不是"黑块"。宁可退化得平淡，也不能看起来像坏了。
     */
    private fun captureBackdropIfNeeded(target: Bitmap) {
        if (!backdropDirty) return
        backdropDirty = false

        val bc = Canvas(target)
        bc.drawColor(0, PorterDuff.Mode.CLEAR)

        // ---- 路径 1：上层显式提供 ----
        val src = backdropSource
        if (src != null && !src.isRecycled) {
            val p = Paint(Paint.FILTER_BITMAP_FLAG)
            bc.drawBitmap(src, null, android.graphics.Rect(0, 0, width, height), p)
            return
        }

        // ---- 路径 2：从根 View 渲染 ----
        // 用 getLocationOnScreen 拿到自己在屏幕里的位置，
        // 然后把 rootView 画到一张整屏 Bitmap 上，再裁自己那一块。
        val root = rootView
        if (root != null && root.width > 0 && root.height > 0) {
            val full = try {
                Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            } catch (t: Throwable) {
                null
            }
            if (full != null) {
                try {
                    val fc = Canvas(full)
                    // 抓背景时把自己藏起来，否则会把上一帧的玻璃叠进去
                    val wasVisible = visibility
                    visibility = INVISIBLE
                    try {
                        root.draw(fc)
                    } finally {
                        visibility = wasVisible
                    }
                    val loc = IntArray(2)
                    getLocationOnScreen(loc)
                    val left = loc[0].coerceIn(0, full.width)
                    val top = loc[1].coerceIn(0, full.height)
                    val right = (loc[0] + width).coerceIn(0, full.width)
                    val bottom = (loc[1] + height).coerceIn(0, full.height)
                    if (right > left && bottom > top) {
                        val crop = Bitmap.createBitmap(full, left, top, right - left, bottom - top)
                        val p = Paint(Paint.FILTER_BITMAP_FLAG)
                        bc.drawBitmap(crop, null, android.graphics.Rect(0, 0, width, height), p)
                        crop.recycle()
                    }
                } catch (t: Throwable) {
                    // 渲染失败就退到路径 3，不抛
                } finally {
                    full.recycle()
                }
            }
        }

        // ---- 路径 3：兜底中性色（半透明乳白，绝不是黑） ----
        // 判断"上面两条是否真的画进了东西"：采样中心像素的 alpha。
        // 如果中心像素完全透明，说明什么都没抓到，用兜底色覆盖。
        //
        // ⚠️ getPixel 的坐标必须落在 bitmap 内：极窄/极扁的 View
        //    （比如 1px 高的分隔线）会越界崩溃，这里做一次夹取。
        val px = (width / 2).coerceIn(0, target.width - 1)
        val py = (height / 2).coerceIn(0, target.height - 1)
        if (px >= 0 && py >= 0) {
            val probe = target.getPixel(px, py)
            if (android.graphics.Color.alpha(probe) == 0) {
                bc.drawColor(fallbackColor)
            }
        } else {
            bc.drawColor(fallbackColor)
        }
    }

    /** 兜底色：半透明乳白。抓不到背景时用它，视觉上是"雾面玻璃"。 */
    private var fallbackColor: Int = 0x66E8EEF6.toInt()

    private var backdropSource: Bitmap? = null
    private var backdropDirty = true

    /**
     * 由上层提供一个「背景快照」。
     *
     * 这是最可靠的路径 —— 调用方（Compose 侧）知道背景长什么样，
     * 直接交给玻璃即可，玻璃不用自己去"回看"。
     *
     * ⚠️ 生命周期：这块 Bitmap **归调用方所有**，GlassView 只读不回收。
     *    调用方想换背景就直接再调一次；不再需要时把它设为 null 即可。
     */
    fun setBackdropSource(bitmap: Bitmap?) {
        if (backdropSource === bitmap) return
        backdropSource = bitmap
        backdropDirty = true
        invalidate()
    }

    /** 通知玻璃：背景变了，需要重抓。 */
    fun markBackdropDirty() {
        backdropDirty = true
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        offscreen?.recycle(); offscreen = null
        backdrop?.recycle(); backdrop = null
        sdfBitmap?.recycle(); sdfBitmap = null
    }
}