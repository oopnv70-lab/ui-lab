package com.oopnv70.uilab.glass

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.liquidglass.LiquidGlassView

/**
 * QWEA0 Liquid-Glass-Android 的 Compose 适配层。
 *
 * ---------------------------------------------------------------------------
 * 为什么这里**禁止**自动寻找背景源（真机取证 2026-09-15 01:26）
 * ---------------------------------------------------------------------------
 * 崩溃栈（run-as 读 files/last_crash.txt，每次启动必现）：
 *
 *   AndroidComposeView.dispatchDraw
 *     └ AndroidViewsHandler.drawView            ← 画我们的 AndroidView 里的玻璃
 *         └ LiquidGlassView.onDraw
 *             └ GlassLensRenderer.draw
 *                 └ BackdropCapture.draw → drawContent → host.draw(canvas)
 *                     └ AndroidComposeView.dispatchDraw   ← ★ 同一个 view，重入
 *                         └ GraphicsLayerOwnerLayer.updateDisplayList
 *                             └ RenderNode.beginRecording
 *                                 → IllegalStateException: Recording currently in progress
 *
 * 即：把 `backdropSource` 指到 Compose 宿主 `AndroidComposeView` 时，
 * 玻璃在绘制中又通过 `BackdropCapture.drawContent` 调
 * `AndroidComposeView.draw()`，而 Compose 的 dispatchDraw 会直接去
 * beginRecording 它自己的 RenderNode —— 该节点此刻正在录制外层那一帧，
 * 于是必然重入崩溃。
 *
 * 第三方库确实**声称**支持「跨层级祖先」作为来源（BackdropCapture.childOnPathTo
 * 会跳过玻璃所在分支、改用公开的 View.draw 规避重入），这套规避对普通
 * ViewGroup 有效，但对 `AndroidComposeView` 无效 —— 因为 Compose 的
 * dispatchDraw 本身就会重入自己的图形层，公开 draw() 救不了它。
 *
 * 同时，不指定来源时库会回退到**直接父容器**，在 Compose 里那是空壳
 * `AndroidViewsHandler`，捕获结果恒为空白 → 玻璃发黑（上一轮已实测）。
 *
 * 结论：
 *   - 指祖先（AndroidComposeView） → 必崩
 *   - 不指（回退空壳父级）        → 必黑
 * 所以两条自动路径都不能用。适配层只接受调用方**显式**给出的、且
 * **不是玻璃自身/后代/祖先**的真实 View 作为来源；给不出就老老实实不画玻璃，
 * 绝不闪退、也绝不假装有折射。
 *
 * 为什么当前调用方都传 null：本 App 的内容全部由 Compose 绘制在同一棵
 * `AndroidComposeView` 里，玻璃（底部导航栏 / 设置按钮）与内容
 * （PullToRefreshBox）在 View 层是**同一棵树**，不存在「与玻璃同级、
 * 又不包含玻璃」的普通 View 可作为来源。要拿到真正的同级来源，
 * 需要把待折射内容也放进一个由我们持有的普通 View 子树中（见 README/迁移计划），
 * 那是下一步的结构改造，不在本轮。
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    /**
     * 背景来源。必须是**真实承载内容**、且与玻璃**无父子关系**的 View。
     * 传 null（默认）= 不做背景捕获，玻璃退化为不绘制 —— 这是当前唯一安全的行为。
     */
    backdropSourceView: View? = null,
    cornerRadiusDp: Float = 999f,
    refract: Float = GlassDefaults.REFRACT,
    curve: Float = GlassDefaults.CURVE,
    chroma: Float = GlassDefaults.CHROMA,
    specular: Float = GlassDefaults.SPECULAR,
    specularSharp: Float = GlassDefaults.SPECULAR_SHARP,
    fresnel: Float = GlassDefaults.FRESNEL,
    tint: Float = GlassDefaults.TINT,
    tintColor: Color = Color(0xFFEBF2FF),
    backdropBlur: Float = 5f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                LiquidGlassView(context).apply {
                    configureThirdPartyGlass(
                        cornerRadiusDp, refract, chroma, specular,
                        tint, tintColor, backdropBlur
                    )
                    isClickable = false
                    isFocusable = false
                    applyBackdrop(this, backdropSourceView)
                }
            },
            update = { view ->
                view.configureThirdPartyGlass(
                    cornerRadiusDp, refract, chroma, specular,
                    tint, tintColor, backdropBlur
                )
                applyBackdrop(view, backdropSourceView)
            }
        )
        content()
    }
}

/**
 * 只在来源**安全**时挂上去。
 *
 * 安全 = 来源既不是玻璃自己，也不是玻璃的后代（库已校验），
 *        **也不是玻璃的祖先**（库不校验，而它恰恰是崩溃来源）。
 *
 * 任一条不满足 → 保持 `backdropSource = null`。此时库会回退到直接父容器
 * （Compose 里是空壳 AndroidViewsHandler，捕获为空白），视觉上等于没有玻璃，
 * 但**绝不会崩**。宁可不显示，也不闪退。
 */
private fun applyBackdrop(glass: LiquidGlassView, source: View?) {
    if (source == null || source === glass) {
        glass.backdropSource = null
        return
    }
    // 来源是玻璃的祖先 → 库允许，但 Compose 下必崩（见文件头注释），拒绝。
    if (isAncestorOf(source, glass)) {
        glass.backdropSource = null
        return
    }
    // 来源是玻璃的后代 → 库自己会拒绝并打日志；这里同步置空，避免不一致。
    if (isAncestorOf(glass, source)) {
        glass.backdropSource = null
        return
    }
    glass.backdropSource = source
}

/** [ancestor] 是否在 [view] 的父链上。 */
private fun isAncestorOf(ancestor: View, view: View): Boolean {
    var p: View? = view.parent as? View
    while (p != null) {
        if (p === ancestor) return true
        p = p.parent as? View
    }
    return false
}

private fun LiquidGlassView.configureThirdPartyGlass(
    cornerRadiusDp: Float,
    refract: Float,
    chroma: Float,
    specular: Float,
    tint: Float,
    tintColor: Color,
    backdropBlur: Float
) {
    cornerRadius = cornerRadiusDp
    refractionHeight = refract
    blurAmount = (backdropBlur / 64f).coerceIn(0.01f, 1f)
    aberrationIntensity = (chroma * 20f).coerceIn(0f, 8f)
    enableBackdropBlur = true
    enableChromaticAberration = chroma > 0.001f
    enableEdgeHighlight = specular > 0.001f
    // 必须开：否则背景不每帧重录，玻璃看起来是「冻住的」。
    enableDynamicBackground = true
    setGlassTint(
        android.graphics.Color.rgb(
            (tintColor.red * 255f).toInt(),
            (tintColor.green * 255f).toInt(),
            (tintColor.blue * 255f).toInt()
        ),
        tint.coerceIn(0f, 1f)
    )
}
