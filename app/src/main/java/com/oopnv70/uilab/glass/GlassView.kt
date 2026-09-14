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
// 真·液态玻璃 · 第三步：GlassView（跑 shader 的绘制层）
// =====================================================================
// 为什么必须是自定义 View，而不是纯 Compose：
//
//   Compose 的 Modifier.blur() 只能模糊**自己**，拿不到身后的内容。
//   而液态玻璃的全部意义在于「折射身后的东西」，所以必须有一个能
//   直接操纵 Canvas / Paint / RuntimeShader 的绘制层。
//
// 执行流程（每帧）：
//   ① 取背景贴图（backdrop）
//   ② backdrop + SDF 一起喂给 AGSL RuntimeShader
//   ③ 用 Paint(shader) 把自己那一块画出来
//
// ⚠️⚠️ 三条用血换来的铁律，改这个文件前务必读一遍 ⚠️⚠️
//
//   1) 【绝不在 Bitmap 的 Canvas 上跑 shader】
//      Bitmap → Canvas 一定是软件 canvas，而 RuntimeShader 只支持硬件。
//      在软件 canvas 上画它必抛：
//        IllegalArgumentException: Software rendering doesn't support RuntimeShader
//      真机上就是因此闪退的（HONOR AAK-AN00 / Android 17）。
//
//   2) 【绝不 rootView.draw(软件Canvas) 去抓背景】
//      Compose 的 rootView 是 AndroidComposeView，重绘它会连带重绘
//      子树里的其他 GlassView（胶囊栏、齿轮），那些 View 的 onDraw
//      拿到软件 canvas 后踩中第 1 条 → 崩；而且它会递归回自己。
//
//   3) 【背景只能由上层 provide】
//      唯一没有副作用的路径是调用方用 setBackdropSource() 把背景位图
//      交进来。抓不到就退化成磨砂色 —— 平淡，但永远不崩、不黑。
//
// ⚠️ 性能前提：SDF 是静态的，**只在尺寸变化时算一次**，此后每帧
//    只是 shader 合成。这是本方案能跑到可用帧率的关键。
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

    // ---- backdrop：喂给 shader 的背景贴图（本 View 尺寸）----
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
    init {
        // ⚠️ 关键：强制本 View 拥有**自己的硬件层**。
        //
        // 不加这句的话，Compose 通过 AndroidViewsHandler.drawView 录制
        // 这个 AndroidView 时，onDraw 可能拿到**软件** canvas，而
        // RuntimeShader 在软件 canvas 上会直接抛：
        //   IllegalArgumentException: Software rendering doesn't support RuntimeShader
        // 真机上就是这么闪退的。
        //
        // 有了硬件层之后，系统会先把本 View 画进一张硬件缓冲，
        // 再交给合成器，onDraw 拿到的一定是硬件加速的 canvas。
        //
        // 只影响这一个 View，不改变它的大小/布局与触摸行为。
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        // backdrop：喂给 shader 的背景贴图
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

        // ⚠️⚠️ 这里曾经是崩溃源，务必保留这个守卫 ⚠️⚠️
        //
        // 实测崩溃（真机 HONOR AAK-AN00 / Android 17）：
        //   java.lang.IllegalArgumentException:
        //     Software rendering doesn't support RuntimeShader
        //       at android.graphics.Canvas.drawRect(Canvas.java:2162)
        //       at GlassView.onDraw(GlassView.kt:191)
        //
        // Compose 会通过 AndroidViewsHandler.drawView 录制 AndroidView，
        // 在这个过程中给过来的 canvas 可能是**软件** canvas。
        // 而 AGSL 的 RuntimeShader 只有硬件管线能跑 —— 软件 canvas 上
        // 画它就必然抛 IllegalArgumentException，直接把进程干掉。
        //
        // 所以：不是硬件加速就**不碰 shader**，退化成一片磨砂色。
        // 少一点折射远好过闪退。
        if (!canvas.isHardwareAccelerated) {
            drawFallback(canvas)
            return
        }

        val bd = backdrop ?: run { drawFallback(canvas); return }
        val sdf = sdfBitmap ?: run { drawFallback(canvas); return }

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
        try {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glassPaint)
        } catch (t: Throwable) {
            // 任何平台差异导致的绘制异常都不该杀进程：
            // 退回磨砂色，界面照常可用。
            glassPaint.shader = null
            drawFallback(canvas)
        }
    }

    /**
     * 兜底绘制：一片半透明磨砂色，完全走普通 Paint，不涉及 shader。
     *
     * ⚠️ 这个方法必须「绝对安全」：它出现在所有异常路径上，
     *    如果它自己也抛，就失去了兜底的意义。
     */
    private fun drawFallback(canvas: Canvas) {
        try {
            glassPaint.shader = null
            glassPaint.color = fallbackColor
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glassPaint)
        } catch (_: Throwable) {
            // 连兜底都失败就什么都不画。绝不再往外抛。
        }
    }

    /**
     * 抓取「本 View 身后」的内容到 [backdrop]。
     *
     * 策略（按可靠性从高到低）：
     *   1) 上层通过 [setBackdropSource] 主动提供的 Bitmap
     *   2) 都拿不到 → 磨砂色兜底，**绝不留黑，也绝不留黑屏**
     *
     * ═══════════════════════════════════════════════════════════════
     * ⚠️ 血泪教训：这里曾经有一条「把 rootView 画进 Bitmap」的路径，
     *    它是真机闪退的元凶，**永远不要再加回来**：
     *
     *      Bitmap → Canvas 必然是**软件** canvas，
     *      而 rootView（Compose 的 AndroidComposeView）重绘时，
     *      会连带重绘它子树里的**其他 GlassView**（比如胶囊导航栏），
     *      那些 GlassView 的 onDraw 拿到软件 canvas 之后跑 AGSL
     *      RuntimeShader，直接抛：
     *
     *        IllegalArgumentException:
     *          Software rendering doesn't support RuntimeShader
     *
     *      而且 rootView.draw() 会递归回到自己 → 无限重绘。
     *
     * ⚠️ 也**不要**改用 PixelCopy：
     *    PixelCopy 只能对着 Window / SurfaceView 发起，对着 View 没有重载；
     *    而且它读的是「已合成完毕的缓冲」，在 onDraw 里发起会形成
     *    「我要画 → 我要先读我刚画的东西」的循环依赖，读到的永远是上一帧
     *    甚至空帧。放在这条链路上只会引入新的不确定性。
     *
     * 结论：背景**只能由上层提供**。Compose 侧知道背景长什么样，
     *      把那张位图交给玻璃，是唯一没有副作用的路径。
     * ═══════════════════════════════════════════════════════════════
     */
    private fun captureBackdropIfNeeded(target: Bitmap) {
        if (!backdropDirty) return

        // ---- 路径 1：上层显式提供（同步，最可靠） ----
        val src = backdropSource
        if (src != null && !src.isRecycled) {
            backdropDirty = false
            val bc = Canvas(target)
            bc.drawColor(0, PorterDuff.Mode.CLEAR)
            val p = Paint(Paint.FILTER_BITMAP_FLAG)
            bc.drawBitmap(src, null, android.graphics.Rect(0, 0, width, height), p)
            return
        }

        // ---- 路径 2：没有背景可用 → 磨砂兜底 ----
        // 不抛、不崩、不黑。用户看到的是一块得体的半透明磨砂玻璃。
        backdropDirty = false
        val bc = Canvas(target)
        bc.drawColor(0, PorterDuff.Mode.CLEAR)
        bc.drawColor(fallbackColor)
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
        backdrop?.recycle(); backdrop = null
        sdfBitmap?.recycle(); sdfBitmap = null
    }
}