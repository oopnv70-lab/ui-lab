package com.oopnv70.uilab.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

// =====================================================================
// 真·液态玻璃 · 折射 + 高光 RuntimeShader
// =====================================================================
// 这是整个方案里「真」与「假」的分水岭。
//
// 毛玻璃（假货）：把背景采样出来，做一次高斯模糊，贴上去。光路没变。
// 液态玻璃（真货）：把背景采样出来，**根据形状把人眼本该看到的光线
//                  方向偏移掉**，再叠上边缘的镜面高光。
//
// ── 本轮的关键改动（前四轮"只是白板"的根因）──────────────
//
//   旧写法有两个致命错误：
//
//   ① SDF 走纹理（uSdf.eval(fragCoord)）
//      纹理和视图像素必须严格一一对齐，只要 margin / 采样坐标有一点
//      偏差，读到的就是错的距离场 → 折射方向乱掉 → 看起来像没效果。
//      ✅ 现在改为**解析式 SDF**：圆角矩形直接用公式算，
//         零纹理、零对齐问题，也是成熟实现（GlassLensRenderer）的做法。
//
//   ② 背景采样坐标没有考虑录制区外扩
//      背景节点现在为了模糊/折射外扩了 margin，fragCoord 是**外扩后**
//      空间的坐标。采样背景前必须先减掉 margin 才能对上内容。
//      ✅ 现在由 uMargin 显式补偿。
//
// ── 三个光学效果 ──────────────────────────────────────
//
//   ① 折射（lensing）
//      把玻璃想成一块厚透镜：像素离边缘越近，玻璃的「坡」越陡，
//      折射越强；到了正中间坡变平，就几乎不偏移（等于透过去）。
//          offset = normal * pow(edge, uCurve) * uRefract
//      于是背景在玻璃里「被弯了一下」——这就是折射。
//
//   ② 边缘高光（specular）
//      光从斜上方打来，玻璃边缘的坡面产生镜面反射。
//      高光强度 = 坡面法线 · 光照方向，只在边缘一圈亮。
//
//   ③ 菲涅耳（fresnel）
//      掠射角反射强：越靠边越亮，让玻璃"有厚度"。
//
// 所有参数都由外部传入，方便调参到好看为止。
// =====================================================================

/**
 * RuntimeShader 的 AGSL 源码。
 *
 * AGSL 与 GLSL 基本同源，但有几处硬性差异，容易踩：
 *   - 入口函数固定为 `half4 main(float2 fragCoord)`，不是 `void main()`
 *   - 颜色分量是 0..1 的 float，不是 0..255
 *   - uniform 用 `uniform` 关键字声明，通过 setFloatUniform 传
 *   - 没有隐式类型转换，浮点必须写 1.0 而不是 1
 *   - `uniform shader xxx;` 声明的子输入，通过
 *     RenderEffect.createRuntimeShaderEffect(shader, "xxx") 绑定
 */
@RequiresApi(Build.VERSION_CODES.S)
const val GLASS_AGSL = """
uniform shader uBackdrop;   // 背后被折射的内容（GlassView 录制的背景）

uniform float2 uSize;       // 玻璃尺寸（像素），用于把 fragCoord 归一化
uniform float  uMargin;     // 录制区外扩边距：采样背景前必须减掉它
uniform float  uShapeR;     // 圆角半径（像素，已钳到短边一半以内）
uniform float  uRefract;    // 折射强度（像素），越大越"弯"
uniform float  uCurve;      // 折射曲线指数：越大，弯折越集中在边缘
uniform float  uChroma;     // 色散强度：RGB 三通道折射量分离，产生彩虹边
uniform float  uSpecular;   // 高光强度
uniform float  uSpecularSharp; // 高光锐度
uniform float  uFresnel;    // 菲涅耳边缘增亮强度
uniform float  uTint;       // 玻璃本体染色量（0=纯透，1=不透）
uniform float3 uTintColor;  // 染色颜色
uniform float  uBrightness; // 整体亮度微调

/**
 * 圆角矩形 SDF（解析式，零纹理）。
 *
 * 返回值语义：
 *   < 0 → 形状内部（越负越深）
 *   = 0 → 正好在边界上
 *   > 0 → 形状外部（离边界多远）
 *
 * 它同时也是"厚度剖面"的来源：内部越深 → 坡越平 → 折射越弱。
 */
float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fragCoord) {
    // ── 坐标换算 ──────────────────────────────────────────────
    // fragCoord 是**录制区**（外扩 margin 之后的节点）里的像素坐标。
    // 减掉 margin 得到视图像素坐标（原点在玻璃左上角）。
    float2 v = fragCoord - float2(uMargin, uMargin);

    // 视图矩形外直接透明：形状外不画任何东西（覆盖率由 SDF 输出，
    // 所以这里不需要 canvas 裁剪）。
    if (v.x < 0.0 || v.y < 0.0 || v.x > uSize.x || v.y > uSize.y) {
        return half4(0.0);
    }

    // ── 解析式 SDF ────────────────────────────────────────────
    // 半尺寸；圆角半径由外部传入（已钳到短边一半以内）。
    // 半径 = 短边一半 → 胶囊/圆形（我们的按钮和底栏都是这种）
    float2 half_ = uSize * 0.5;
    float radius = clamp(uShapeR, 0.0, min(half_.x, half_.y));
    float2 p = v - half_;

    float d = sdRoundBox(p, half_, radius);

    // 覆盖率：1.5px 抗锯齿羽化，形状外完全透明
    float cov = clamp(0.5 - d / 1.5, 0.0, 1.0);
    if (cov <= 0.004) {
        return half4(0.0);
    }

    // 屏幕空间外法线：SDF 数值梯度
    float2 n = float2(
        sdRoundBox(p + float2(1.0, 0.0), half_, radius) - sdRoundBox(p - float2(1.0, 0.0), half_, radius),
        sdRoundBox(p + float2(0.0, 1.0), half_, radius) - sdRoundBox(p - float2(0.0, 1.0), half_, radius)
    );
    float nLen = length(n);
    n = nLen > 0.0001 ? n / nLen : float2(0.0, -1.0);

    // ── 厚度剖面 ──────────────────────────────────────────────
    // 斜面宽度：取短边的 22%，最少 8px。
    // edge: 0 = 玻璃内部（平坦，不折射），1 = 最外圈（坡最陡，折射最强）
    float bevel = max(min(half_.x, half_.y) * 0.22, 8.0);
    float t = clamp(-d / bevel, 0.0, 1.0);   // 1 = 深处，0 = 边缘
    float edge = 1.0 - t;

    // ── ① 折射 ────────────────────────────────────────────────
    // pow(edge, uCurve) 把弯折能量集中到靠边的一圈，中间保持干净。
    // 这就是"透镜感"的来源：只有边缘在弯折光线。
    float shaped = pow(edge, uCurve);
    float refractPx = shaped * uRefract;
    float2 offset = n * refractPx;

    // 采样坐标：背景在**录制区**坐标系里。
    // 录制时录的是"视图矩形 + 四周 margin"，所以视图坐标 v 对应
    // 录制区坐标 (v + margin)。
    float2 baseUV = v + float2(uMargin, uMargin);
    float2 uvG = baseUV + offset;
    float2 uvR = baseUV + offset * (1.0 - uChroma);
    float2 uvB = baseUV + offset * (1.0 + uChroma);

    // 钳制到录制区，杜绝采到透明黑（边缘拖出灰边）
    float2 lo = float2(0.0);
    float2 hi = uSize + float2(uMargin * 2.0, uMargin * 2.0);
    uvR = clamp(uvR, lo, hi);
    uvG = clamp(uvG, lo, hi);
    uvB = clamp(uvB, lo, hi);

    float r = uBackdrop.eval(uvR).r;
    float g = uBackdrop.eval(uvG).g;
    float b = uBackdrop.eval(uvB).b;
    float3 col = float3(r, g, b);

    // ── ② 边缘高光（specular）────────────────────────────────
    // 光从左上打来（和大多数界面光源一致），斜扫过边缘的坡面。
    float2 lightDir = normalize(float2(-0.6, -0.8));
    float slope = pow(edge, uSpecularSharp);
    float ndl = max(dot(n, lightDir), 0.0);
    float spec = pow(ndl, 8.0) * slope * uSpecular;

    // 高光颜色略偏冷白，比纯白更像玻璃（纯白会显得像塑料）
    col += float3(0.95, 0.97, 1.0) * spec;

    // ── ③ 菲涅耳 ─────────────────────────────────────────────
    // 掠射增亮：边缘一圈柔和增亮，让玻璃"有厚度"。
    float fres = pow(edge, 2.0) * uFresnel;
    col += float3(fres);

    // ── 染色 ─────────────────────────────────────────────────
    // 内部染得多一点（玻璃的"体色"），边缘染得少（高光/折射主导）。
    col = mix(col, uTintColor, uTint * t * 0.30 + uTint * 0.10);

    // ── 整体亮度 ──────────────────────────────────────────────
    col *= uBrightness;

    // alpha 由覆盖率给出：形状外（含抗锯齿圈）自然透明，
    // 形状内不透明地盖住背景，否则会看到"折射后的背景"和
    // "原背景"两层重影。
    return half4(clamp(col, float3(0.0), float3(1.0)), cov);
}
"""

/**
 * 创建液态玻璃 shader。
 */
@RequiresApi(Build.VERSION_CODES.S)
fun createLiquidGlassShader(): RuntimeShader = RuntimeShader(GLASS_AGSL)

// =====================================================================
// 参数默认值
// =====================================================================
// 这组值是按「一眼看出是液态玻璃」调的，不是按物理精确调的。
//
// 调参口诀（真机效果不对时按这个顺序改）：
//   看不出弯折     → 加大 REFRACT（14 → 20）
//   边缘糊成一团   → 减小 REFRACT，或加大 CURVE
//   边缘没高光     → 加大 SPECULAR / 减小 SPECULAR_SHARP
//   像塑料不像玻璃 → 减小 TINT，加大 FRESNEL
// =====================================================================

object GlassDefaults {
    /** 折射像素量。太小看不出折射，太大背景会被撕碎。 */
    const val REFRACT = 16f

    /** 折射曲线指数。2~4 之间效果最好：弯折集中在靠边的一圈。 */
    const val CURVE = 2.6f

    /** 色散。0.05~0.15 是"高雅"，超过 0.25 就成迪斯科了。 */
    const val CHROMA = 0.10f

    /** 高光强度。 */
    const val SPECULAR = 1.15f

    /** 高光锐度。越大高光越细、越像一道线。 */
    const val SPECULAR_SHARP = 4.0f

    /** 菲涅耳边缘增亮。 */
    const val FRESNEL = 0.28f

    /** 本体染色。保持很低，让背景能透出来。 */
    const val TINT = 0.18f
}