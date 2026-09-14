package com.oopnv70.uilab.glass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi

// =====================================================================
// 真·液态玻璃 · GlassView（跑 shader 的绘制层）
// =====================================================================
// 为什么必须是自定义 View，而不是纯 Compose：
//
//   Compose 的 Modifier.blur() 只能模糊**自己**，拿不到身后的内容。
//   而液态玻璃的全部意义在于「折射身后的东西」，所以必须有一个能
//   直接操纵 Canvas / RenderNode / RuntimeShader 的绘制层。
//
// ── 每帧的执行流程 ──────────────────────────────────────────────
//
//   ① recordBackdrop()：把**背景来源视图**（玻璃身后那一层）录进
//      本 View 的 RenderNode。这一步用的是硬件画布，并且通过
//      BackdropCapture 把玻璃自己从画面里剔除。
//   ② shader 参数写进 RuntimeShader 的 uniform。
//   ③ 用 RenderEffect 链把「模糊 → 折射 shader」挂到 RenderNode 上。
//   ④ canvas.drawRenderNode() 输出。
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
//      ✅ 正确做法（本文件现在用的）：录制走 RenderNode.beginRecording()，
//         它给的是**硬件**画布；排除自己走 BackdropCapture 的
//         「把通往自己的那条分支置为 INVISIBLE」。见 BackdropCapture.kt。
//
//   3) 【模糊必须挂在 RenderNode 上，不是 Paint 上】
//      Paint.setRenderEffect 在本项目编译环境不可解析（已删）；
//      正确写法是 renderNode.setRenderEffect(RenderEffect...)。
//
// ⚠️ 性能前提：SDF 是静态的，**只在尺寸变化时算一次**，此后每帧
//    只是 uniform + 录制。这是本方案能跑到可用帧率的关键。
// =====================================================================

/** 诊断开关：排查玻璃渲染问题时临时打开，交付前改回 false。 */
private const val DEBUG_GLASS = true

private const val TAG = "GlassView"

@RequiresApi(Build.VERSION_CODES.S)
class GlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ---- shader ----
    private val shader: RuntimeShader = createLiquidGlassShader()

    // ---- 背景录制节点 ----
    /** 承载「背景 + 效果链」的硬件 RenderNode。 */
    private val backdropNode = RenderNode("GlassBackdrop")

    private val backdropCapture = BackdropCapture()

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
     * ⚠️ 上一轮这里写着「不使用」，因为 setRenderEffect 编译不过。
     *    现在改用 RenderNode.setRenderEffect —— 这个 API 是存在的，
     *    模糊终于真的挂上了。默认给一个轻微的值（液态玻璃的模糊是
     *    「柔化」而不是「糊成一团」）。
     */
    var backdropBlur: Float = 6f

    // ---- 背景来源 ----
    /**
     * 背景来源视图：玻璃要折射的是**它**的内容。
     *
     * 默认是直接父容器（Compose 下即承载本 AndroidView 的那一层），
     * 这样可以折射到玻璃身后的整块 UI。上层可以用
     * [setBackdropSourceView] 指定成更精确的视图（例如天气背景层）。
     */
    private var backdropSourceView: View? = null

    /** 诊断用：本次尺寸下是否已经打过日志。 */
    private var loggedFirstDraw = false

    // =================================================================
    // 尺寸变化
    // =================================================================
    init {
        // ⚠️ 保留：强制本 View 拥有自己的硬件层。
        //   虽然背景录制不再依赖它（录制用的是 RenderNode 的硬件画布），
        //   但它仍然保证 onDraw 拿到的 canvas 是硬件加速的，
        //   让 RuntimeShader 的绘制路径保持可用。
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        loggedFirstDraw = false
        backdropRecorded = false
    }

    // =================================================================
    // 背景录制：必须在 onPreDraw，不能放在 onDraw
    // =================================================================
    //
    // ═══════════════════════════════════════════════════════════════
    // ⚠️⚠️ 这是本轮"玻璃变黑"的根因，务必记住 ⚠️⚠️
    //
    //   上一版把背景录制放在 onDraw 里。表面上没问题，实际上：
    //   onDraw 执行的时刻，整棵树**正在绘制中**，背景层的内容还在
    //   往各自的 RenderNode 里录。此时用 View.draw() 去"重放"背景，
    //   拿到的是一帧**没画完**的内容 —— Compose 的 AndroidComposeView
    //   尤其如此，它的内容由 Compose 自己的绘制管线控制，从 View.draw()
    //   重入常常得到空白。
    //
    //   空白背景 → shader 采样全 0 → 输出纯黑。
    //   所以现象是"黑了"，而不是"闪退"或"白板"。
    //
    //   ✅ 正确时机：ViewTreeObserver.OnPreDrawListener。
    //      它在"这一帧的绘制开始之前"回调 —— 此时上一帧的背景层
    //      已经完整画好，重放它就能拿到真实内容。
    //
    //   这也是官方参考实现的共识做法（react-native-liquid-glassmorphism
    //   的 Android 文档明确写：backdrop capture 发生在 onPreDraw）。
    // ═══════════════════════════════════════════════════════════════

    /** 本次尺寸下背景是否已经录好。 */
    private var backdropRecorded = false

    private val preDrawListener = android.view.ViewTreeObserver.OnPreDrawListener {
        // 在绘制前录制背景。录制本身不动 UI 状态（只短暂改可见性），
        // 所以这里返回 true 让正常的绘制流程继续。
        recordBackdropIfNeeded()
        true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // ⚠️ 必须移除，否则监听器会一直持有 this，且 detach 后仍在跑
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        }
        backdropNode.discardDisplayList()
        backdropRecorded = false
    }

    /**
     * 把背景来源录进 [backdropNode]。
     *
     * 只在 onPreDraw 里调用 —— 见上面那段警示。
     */
    private fun recordBackdropIfNeeded() {
        if (width <= 0 || height <= 0) return

        val source = effectiveBackdropSource() ?: return

        // 记录玻璃相对背景来源的偏移：录制画布的原点要对齐到背景来源的
        // 左上角，这样 shader 里的 uv 才能和背景内容一一对应。
        getLocationOnScreen(locationOnScreen)
        source.getLocationOnScreen(sourceLocationOnScreen)
        val offsetX = (locationOnScreen[0] - sourceLocationOnScreen[0]).toFloat()
        val offsetY = (locationOnScreen[1] - sourceLocationOnScreen[1]).toFloat()

        // 录制区向外扩一圈 margin：让模糊和折射在边缘处也能采到真实内容，
        // 而不是读到节点外的透明黑（那会在边缘拖出一圈发灰的脏边）。
        val margin = computeMargin()
        val recW = width + 2 * margin
        val recH = height + 2 * margin

        // 节点定位：让节点坐标系里的 (margin, margin) 正好落在玻璃左上角。
        backdropNode.setPosition(-margin, -margin, width + margin, height + margin)

        try {
            val recordingCanvas = backdropNode.beginRecording(recW, recH)
            try {
                // 对齐到背景来源原点，再把玻璃自己的位置偏出来
                recordingCanvas.translate(margin - offsetX, margin - offsetY)
                backdropCapture.draw(recordingCanvas, source, this)
            } finally {
                backdropNode.endRecording()
            }
            backdropRecorded = true
        } catch (t: Throwable) {
            // 录制失败不该杀进程：标记未录制，onDraw 里会走兜底。
            // 真机上出过 IllegalStateException（录制重入），必须兜住。
            backdropRecorded = false
            if (DEBUG_GLASS) Log.w(TAG, "backdrop record failed", t)
        }
    }

    /** 录制区外扩边距：覆盖模糊的采样范围，最少 32px（16 对齐更省）。 */
    private fun computeMargin(): Int {
        val need = (backdropBlur * 3f + 16f).toInt()
        return ((need + 15) / 16 * 16).coerceAtLeast(32)
    }

    /**
     * 圆角半径（dp）。
     *
     * 由 shader 解析式使用：传给 uShapeR（像素）。
     * 给一个大于 短边/2 的值即得到胶囊/圆形（默认 999 就是全圆角），
     * 给较小的值则得到圆角矩形。
     */
    private var cornerRadiusDp: Float = 999f

    /** 外部设置圆角（dp 值）。只影响 shader 的 uniform，无需重建任何纹理。 */
    fun setCornerRadiusDp(dp: Float) {
        if (cornerRadiusDp == dp) return
        cornerRadiusDp = dp
        invalidate()
    }

    /**
     * 指定背景来源视图。
     *
     * 不调这个方法时，玻璃默认折射它的**直接父容器**（见 [effectiveBackdropSource]）。
     * 想要折射某个特定区域（例如整页天气背景）时由上层显式指定。
     */
    fun setBackdropSourceView(view: View?) {
        if (backdropSourceView === view) return
        backdropSourceView = view
        invalidate()
    }

    /**
     * 实际使用的背景来源。
     *
     * ⚠️ 不能直接用 `parent`。
     *
     * Compose 的 AndroidView 会被包进它自己的内部容器，直接父级往往
     * 只装着这块玻璃本身 —— 拿它当背景来源，录到的就是"玻璃自己身后的
     * 空白"，折射出来还是一片纯色（这正是"看起来像白板"的一个成因）。
     *
     * 所以要沿父链往上找，跳过那些**只包裹本 View 的中间层**，直到找到
     * 一个真正承载了其它内容的容器。判据：该容器有 ≥2 个可见子级，
     * 或者它就是 Compose 的 AndroidComposeView。
     */
    private fun effectiveBackdropSource(): View? {
        backdropSourceView?.let { return it }

        var candidate: View? = parent as? View
        var depth = 0
        while (candidate != null && depth < 8) {
            if (hasSiblingContent(candidate)) return candidate
            candidate = candidate.parent as? View
            depth++
        }
        // 找不到更合适的就退回直接父级（至少不会崩）
        return parent as? View
    }

    /** 诊断：把整条父链的类名打出来，用来确认背景源解析找对没有。 */
    private fun dumpParentChain(): String {
        val sb = StringBuilder()
        var v: View? = this
        var d = 0
        while (v != null && d < 12) {
            sb.append("\n  [").append(d).append("] ")
                .append(v.javaClass.name)
                .append(" ").append(v.width).append("x").append(v.height)
            if (v is ViewGroup) sb.append(" children=").append(v.childCount)
            v = v.parent as? View
            d++
        }
        return sb.toString()
    }

    /**
     * 判断 [container] 是否"承载了除本玻璃之外的可见内容"。
     *
     * 这是我们能拿它当背景来源的前提：只有它身后有东西，折射才有意义。
     */
    private fun hasSiblingContent(container: View): Boolean {
        if (container is android.view.ViewGroup) {
            if (container.childCount >= 2) return true
        }
        // Compose 的宿主容器：它的内容由 Compose 绘制，childCount 不可靠，
        // 但只要它有实际面积，就是最靠谱的背景来源。
        return container.javaClass.name.contains("AndroidComposeView")
    }

    // =================================================================
    // 核心：录制背景 + 画玻璃
    // =================================================================
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

        val source = effectiveBackdropSource()

        if (DEBUG_GLASS && !loggedFirstDraw) {
            loggedFirstDraw = true
            Log.i(
                TAG,
                "onDraw: w=$width h=$height hw=true " +
                    "source=${source?.javaClass?.simpleName} recorded=$backdropRecorded" +
                    " chain=" + dumpParentChain()
            )
        }

        // 背景来源拿不到、或背景还没录好 → 磨砂兜底。
        if (source == null || !backdropRecorded) {
            drawFallback(canvas)
            return
        }

        try {
            drawGlass(canvas, source)
        } catch (t: Throwable) {
            // 任何平台差异导致的绘制异常都不该杀进程：
            // 退回磨砂色，界面照常可用。
            Log.w(TAG, "glass draw failed, falling back", t)
            drawFallback(canvas)
        }
    }

    /**
     * 输出已经录好的玻璃。
     *
     * ⚠️ 这里**不再录制背景** —— 背景由 onPreDraw 里的
     * [recordBackdropIfNeeded] 提前录好（见那段警示）。
     * 本方法只负责：写 uniform → 挂效果链 → 画节点。
     *
     * ① 把 uniform 全部写进 shader
     * ② 在 [backdropNode] 上挂「模糊 → 折射」的 RenderEffect 链
     * ③ 输出节点
     */
    private fun drawGlass(canvas: Canvas, source: View) {
        val margin = computeMargin()

        // ---- ① 写 uniform ----
        // 注意：这里不碰 backdropNode 的录制内容 —— 它已经在
        // onPreDraw 里录好了。setRenderEffect 只挂效果，不影响录制。
        shader.setFloatUniform("uSize", width.toFloat(), height.toFloat())
        shader.setFloatUniform("uMargin", margin.toFloat())
        shader.setFloatUniform(
            "uShapeR",
            (cornerRadiusDp * resources.displayMetrics.density)
                .coerceAtMost(minOf(width, height) / 2f)
        )
        shader.setFloatUniform("uRefract", refract)
        shader.setFloatUniform("uCurve", curve)
        shader.setFloatUniform("uChroma", chroma)
        shader.setFloatUniform("uSpecular", specular)
        shader.setFloatUniform("uSpecularSharp", specularSharp)
        shader.setFloatUniform("uFresnel", fresnel)
        shader.setFloatUniform("uTint", tint)
        shader.setFloatUniform("uTintColor", tintColor[0], tintColor[1], tintColor[2])
        shader.setFloatUniform("uBrightness", brightness)

        // ---- ③ 效果链：模糊 → 折射 ----
        // ⚠️ 关键是 backdropNode.setRenderEffect（不是 Paint.setRenderEffect）。
        //    createChainEffect(outer, inner) 里 inner 是内层：背景先被模糊，
        //    再交给 RuntimeShader 作 uBackdrop 输入。
        val lensEffect = RenderEffect.createRuntimeShaderEffect(shader, "uBackdrop")
        val effect = if (backdropBlur > 0.01f) {
            RenderEffect.createChainEffect(
                lensEffect,
                RenderEffect.createBlurEffect(backdropBlur, backdropBlur, Shader.TileMode.CLAMP)
            )
        } else {
            lensEffect
        }
        backdropNode.setRenderEffect(effect)

        // ---- ④ 输出 ----
        canvas.drawRenderNode(backdropNode)
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
     * 兜底色：**不透明**的浅乳白。
     *
     * ⚠️ 这里原本是 0x66E8EEF6（40% 透明的白），结果是"连白都看不见"：
     *    GlassView 通过 AndroidView 挂在 Compose 里，自身没有背景，
     *    而且 init 里要了 LAYER_TYPE_HARDWARE —— 这个层的底是**透明的**。
     *    40% 的白叠在透明上，就是一块几乎不可见的幽灵。
     *
     *    兜底的职责是"再怎么退化，用户也得看见一块玻璃"，
     *    所以必须给足不透明度，让它自己就是一块实心的雾面玻璃。
     */
    private var fallbackColor: Int = 0xF2EEF2F8.toInt()

    // 复用的坐标数组，避免每帧分配
    private val locationOnScreen = IntArray(2)
    private val sourceLocationOnScreen = IntArray(2)

    /** 通知玻璃：背景变了，需要重录 + 重绘。 */
    fun markBackdropDirty() {
        backdropRecorded = false
        invalidate()
    }
}