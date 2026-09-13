package com.oopnv70.uilab.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

// =====================================================================
// 真·液态玻璃 · 第二步：折射 + 高光 RuntimeShader
// =====================================================================
// 这是整个方案里「真」与「假」的分水岭。
//
// 毛玻璃（假货）：把背景采样出来，做一次高斯模糊，贴上去。光路没变。
// 液态玻璃（真货）：把背景采样出来，**根据 SDF 把人眼本该看到的光线
//                  方向偏移掉**，再叠上边缘的镜面高光。
//
// 具体三个效果，全在这一个 shader 里：
//
//   ① 折射（lensing）
//      思路：把玻璃想成一块厚透镜。像素离边缘越近，玻璃的「坡」越陡，
//      折射越强；到了正中间坡变平，就几乎不偏移（等于透过去）。
//      用 SDF 的归一化距离 d（1=在边缘，0=远离边缘）当「坡高」：
//          strength = (1 - d) ^ power
//      沿 SDF 给的外法线方向，把采样坐标往回推一小段：
//          uv' = uv - normal * strength * maxOffset
//      于是背景在玻璃里「被弯了一下」——这就是折射。
//
//   ② 边缘高光（specular）
//      光从斜上方打过来，玻璃边缘的坡面会产生一道镜面反射。
//      高光强度 = 坡面方向 · 光照方向，再取正。
//      只在边缘一圈亮（d 接近 1），中间不亮，符合真实玻璃。
//
//   ③ 菲涅耳（fresnel）
//      掠射角反射强。玻璃越靠边、法线越平行于屏幕，反射越亮。
//      严格算法要视线向量；这里用一个廉价的近似：越靠边越亮，
//      和 specular 叠在一起，视觉上就"像玻璃"了。
//
// 所有参数都由外部传入，方便调参到好看为止（用户当前诉求：美观优先）。
// =====================================================================

/**
 * RuntimeShader 的 AGSL 源码。
 *
 * AGSL 与 GLSL 基本同源，但有几处硬性差异，容易踩：
 *   - 入口函数固定为 `half4 main(float2 fragCoord)`，不是 `void main()`
 *   - 颜色分量是 0..1 的 float，不是 0..255
 *   - uniform 用 `uniform` 关键字声明，通过 setFloatUniform 传
 *   - 没有隐式类型转换，浮点必须写 1.0 而不是 1
 */
@RequiresApi(Build.VERSION_CODES.S)
const val GLASS_AGSL = """
uniform shader uBackdrop;   // 背后被折射的内容（由 GlassView 抓取后传入）
uniform shader uSdf;        // 第一步生成的 SDF 图（R=距离, G=法线x, B=法线y）

uniform float2 uSize;       // 玻璃尺寸（像素），用于把 fragCoord 归一化
uniform float  uRefract;    // 折射强度（像素），越大越"弯"
uniform float  uCurve;      // 折射曲线指数：越大，弯折越集中在边缘
uniform float  uChroma;     // 色散强度：RGB 三通道折射量分离，产生彩虹边
uniform float  uSpecular;   // 高光强度
uniform float  uSpecularSharp; // 高光锐度
uniform float  uFresnel;    // 菲涅耳边缘增亮强度
uniform float  uTint;       // 玻璃本体染色量（0=纯透，1=不透）
uniform float3 uTintColor;  // 染色颜色
uniform float  uBrightness; // 整体亮度微调

float3 sampleRefracted(float2 uv, float2 dir, float strength) {
    // 把归一化方向转成实际像素偏移。
    // 注意 uv 是 0..1，而 backdrop 是像素坐标，所以要乘 uSize。
    float2 offset = dir * strength;

    // ---- 色散：红折射少、蓝折射多，模仿真实玻璃的色差 ----
    // 这就是 iOS 液态玻璃边缘那圈若有若无的彩色。
    float2 uvR = uv - offset * (1.0 - uChroma * 1.0);
    float2 uvG = uv - offset * 1.0;
    float2 uvB = uv - offset * (1.0 + uChroma * 1.0);

    float r = uBackdrop.eval(uvR * uSize).r;
    float g = uBackdrop.eval(uvG * uSize).g;
    float b = uBackdrop.eval(uvB * uSize).b;
    return float3(r, g, b);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uSize;

    // ---- 读 SDF ----
    // SDF 图与玻璃同尺寸，fragCoord 可直接当采样坐标。
    //
    // ⚠️ 语义（必须与 SdfGenerator.kt 严格一致，写反了效果会整体颠倒）：
    //   d = 1 → 玻璃【内部】（透镜最厚的中心，坡是平的）
    //   d → 0 → 离玻璃越远（完全无影响）
    //   中间过渡带 → 玻璃边缘外侧的那圈"坡"
    float4 sdf = uSdf.eval(fragCoord);
    float d       = sdf.r;                    // 归一化"玻璃厚度"：1=内部，0=远处
    float2 normal = sdf.gb * 2.0 - 1.0;       // 从 [0,1] 还原到 [-1,1]

    // 内部（d≈1）：坡是平的，应该完全透过去、不做折射。
    // 只有"斜边"（d 在中间段）才弯折光线 —— 这才是真玻璃的行为：
    // 平板玻璃不产生折射位移，只有边缘/曲面才会。
    //
    // 用 edgeBand 把折射限制在「靠边的一圈」：
    //   定义 edge = 1 - d，即"离边缘有多近"（0=内部，1=最外）
    //   再在 0..1 上做一条钟形/斜坡曲线
    float edge = 1.0 - d;

    // ---- ① 折射 ----
    // edge: 0 = 玻璃内部（平坦，不折射），1 = 最外圈（坡最陡，折射最强）
    //
    // 用 pow(edge, uCurve) 把能量集中到边缘：
    //   uCurve 越大，弯折越集中在最外一圈，中间越干净 —— 最像真玻璃。
    float shaped = pow(edge, uCurve);
    float refractPx = shaped * uRefract;

    // 方向：沿外法线把采样点往"外"推，于是背景看起来被"吸"向边缘。
    // normal 在 SDF 里已归一化，直接乘。
    float2 dir = normal * refractPx;

    float3 col = sampleRefracted(uv, dir, 1.0);

    // 完全在内部、且远离边缘时（edge≈0），折射为 0，此处 col 就等于原背景。
    // 不需要额外分支 —— pow(0, curve) = 0，天然退化成"清晰透过"。

    // ---- ② 边缘高光（specular）----
    // 光从左上打来（和大多数界面光源一致），斜扫过边缘的坡面。
    float2 lightDir = normalize(float2(-0.6, -0.8));
    // 坡面越陡（edge 越接近 1）+ 法线越朝向光源 → 高光越亮
    float slope = pow(edge, uSpecularSharp);
    float ndl   = max(dot(normal, lightDir), 0.0);
    float spec  = pow(ndl, 8.0) * slope * uSpecular;

    // 高光颜色略偏冷白，比纯白更像玻璃（纯白会显得像塑料）
    col += float3(0.95, 0.97, 1.0) * spec;

    // ---- ③ 菲涅耳 ----
    // 掠射增亮：边缘一圈柔和增亮，让玻璃"有厚度"。
    float fres = pow(edge, 2.0) * uFresnel;
    col += float3(fres);

    // ---- 染色 ----
    // 内部染得多一点（玻璃的"体色"），边缘染得少（高光/折射主导）。
    // 这样玻璃中心有质感、边缘有光感，不糊成一块塑料板。
    col = mix(col, uTintColor, uTint * d * 0.30 + uTint * 0.10);

    // ---- 整体亮度 ----
    col *= uBrightness;

    // alpha 恒为 1：折射区域不透明地盖住背景，
    // 否则会看到"折射后的背景"和"原背景"两层重影。
    return half4(clamp(col, float3(0.0), float3(1.0)), 1.0);
}
"""

/**
 * 创建配置好的液态玻璃 shader。
 *
 * @param ctx 用于在 API 33+ 之后启用不可变编译（性能更稳）
 */
@RequiresApi(Build.VERSION_CODES.S)
fun createLiquidGlassShader(): RuntimeShader = RuntimeShader(GLASS_AGSL)

// =====================================================================
// 参数默认值
// =====================================================================
// 这组值是按「一眼看出是液态玻璃」调的，不是按物理精确调的。
// 用户当前要求：效果/美观优先。所以折射和色散都比真玻璃夸张一些，
// 否则在手机小尺寸控件上根本看不出来。
//
// 调参口诀（真机效果不对时按这个顺序改）：
//   看不出弯折   → 加大 uRefract（8 → 16）
//   边缘糊成一团 → 减小 uRefract，或加大 uCurve
//   边缘没高光   → 加大 uSpecular / 减小 uSpecularSharp
//   像塑料不像玻璃 → 减小 uTint，加大 uFresnel
// =====================================================================

object GlassDefaults {
    /** 折射像素量。太小看不出折射，太大背景会被撕碎。 */
    const val REFRACT = 14f

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
