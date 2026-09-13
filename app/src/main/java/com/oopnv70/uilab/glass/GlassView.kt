package com.oopnv70.uilab.glass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RenderEffect
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
     * 背景模糊半径（像素）。0 = 不模糊，保持背景完全清晰。
     *
     * 这里给一个**很小的**模糊（3~6），而不是毛玻璃那种大模糊。
     * 原因：液态玻璃的重点是「折射」，不是「模糊」。
     * 全清晰会让折射后的背景显得太"硬"，稍微糊一点点最像真玻璃。
     */
    var backdropBlur: Float = 4f

    /** SDF 最大影响距离，即"玻璃厚度"。 */
    var sdfMaxDistance: Int = 60

    private var backdropRenderEffect: RenderEffect? = null

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

        rebuildSdf(w, h)

        // 背景模糊（可选，很轻）
        backdropRenderEffect = if (backdropBlur > 0f) {
            RenderEffect.createBlurEffect(
                backdropBlur, backdropBlur, Shader.TileMode.CLAMP
            )
        } else null
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
    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return

        val off = offscreen ?: return
        val offCanvas = offscreenCanvas ?: return
        val bd = backdrop ?: return
        val sdf = sdfBitmap ?: return

        // ---- ① 抓背景 ----
        // 关键技巧：让**父容器**重画一遍，但把「本 View」临时设为不可见，
        // 这样 offscreen 里就是「没有玻璃的纯背景」。
        //
        // ⚠️ 不能直接在 canvas 上 draw，因为 canvas 已经包含了本 View 之前
        //    绘制的内容，直接画会把自己叠进去。
        //    用 parent.draw(offscreenCanvas) 才能拿到完整的背景层。
        val parent = parent as? View
        if (parent != null) {
            offCanvas.drawColor(0, PorterDuff.Mode.CLEAR)

            val wasVisible = visibility
            // 把自己藏起来，避免抓背景时把玻璃自身抓进去（会自我引用/拖影）
            visibility = INVISIBLE
            try {
                // 把父容器的绘制原点挪到「本 View 相对父容器」的位置，
                // 这样 offscreen 里的内容正好对齐本 View 的坐标系。
                offCanvas.save()
                offCanvas.translate(-left.toFloat(), -top.toFloat())
                parent.draw(offCanvas)
                offCanvas.restore()
            } finally {
                visibility = wasVisible
            }

            // offscreen → backdrop（如果需要轻微模糊）
            val bc = Canvas(bd)
            bc.drawColor(0, PorterDuff.Mode.CLEAR)
            val bmp = off.copy(Bitmap.Config.ARGB_8888, false)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            backdropRenderEffect?.let { paint.setRenderEffect(it) }
            bc.drawBitmap(bmp, 0f, 0f, paint)
            bmp.recycle()
        }

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
        // Shader 以「view 本地坐标」为基准：fragCoord 从 (0,0) 到 (w,h)。
        // 而 dispatchDraw 拿到的 canvas 原点在父容器坐标系里，
        // 所以要先平移到本 View 的位置，画完再还原。
        glassPaint.shader = shader
        val save = canvas.save()
        canvas.translate(left.toFloat(), top.toFloat())
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glassPaint)
        canvas.restoreToCount(save)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        offscreen?.recycle(); offscreen = null
        backdrop?.recycle(); backdrop = null
        sdfBitmap?.recycle(); sdfBitmap = null
    }
}