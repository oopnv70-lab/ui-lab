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
import com.example.liquidglass.LiquidGlassView

/** Compose adapter for the MIT-licensed QWEA0 Liquid-Glass-Android library. */
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
                }
            },
            update = { view ->
                view.configureThirdPartyGlass(
                    cornerRadiusDp, refract, chroma, specular,
                    tint, tintColor, backdropBlur
                )
            }
        )
        content()
    }
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