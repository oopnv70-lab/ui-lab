package com.oopnv70.uilab.glass

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

// =====================================================================
// 真·液态玻璃 · 第四步：Compose 桥接
// =====================================================================
// 把 GlassView 包成 Compose 可用的组件。
//
// 用法：
//   Box {
//       content()               // ① 先画背景内容
//       LiquidGlassSurface(...) { /* ② 玻璃层里的内容（图标等） */ }
//   }
//
// ⚠️ 顺序很重要：玻璃必须在内容**之后**绘制，才能抓得到背景。
//    在 Compose 里，"之后声明" = "画在上面"，正好符合 Box 的规则。
// =====================================================================

/**
 * 液态玻璃表面。
 *
 * @param modifier 尺寸/位置（就是玻璃元素的形状范围）
 * @param cornerRadiusDp 圆角半径。给一个大于高/2 的值即得到胶囊/圆形。
 * @param refract 折射强度（像素）
 * @param tint 本体检色量
 * @param tintColor 本体色
 * @param backdropBlur 背景模糊半径（很轻，3~6）
 * @param content 画在玻璃**内部**的东西（图标、文字）。注意：
 *                这些内容不参与折射，是浮在玻璃上的。
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadiusDp: Float = 999f,
    refract: Float = GlassDefaults.REFRACT,
    curve: Float = GlassDefaults.CURVE,
    chroma: Float = GlassDefaults.CHROMA,
    specular: Float = GlassDefaults.SPECULAR,
    specularSharp: Float = GlassDefaults.SPECULAR_SHARP,
    fresnel: Float = GlassDefaults.FRESNEL,
    tint: Float = GlassDefaults.TINT,
    tintColor: Color = Color(0xFFEBF2FF),
    backdropBlur: Float = 4f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier) {
        // 玻璃层：一个透明的 AndroidView，只负责在它自己那块区域内画折射。
        //
        // 它是「绘制层」，不响应触摸 —— 事件交给上面的 content 或外层的
        // clickable 处理，避免玻璃把点击吃掉。
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GlassView(ctx).apply {
                    this.refract = refract
                    this.curve = curve
                    this.chroma = chroma
                    this.specular = specular
                    this.specularSharp = specularSharp
                    this.fresnel = fresnel
                    this.tint = tint
                    this.tintColor = floatArrayOf(
                        tintColor.red, tintColor.green, tintColor.blue
                    )
                    this.backdropBlur = backdropBlur
                    this.setCornerRadiusDp(cornerRadiusDp)
                    // 玻璃自己不消费触摸，让点按穿透到下面的可点击区域
                    isClickable = false
                    isFocusable = false
                }
            }
        )

        // 玻璃之上的内容
        content()
    }
}