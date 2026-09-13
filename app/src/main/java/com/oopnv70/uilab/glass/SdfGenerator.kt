package com.oopnv70.uilab.glass

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

// =====================================================================
// 真·液态玻璃 · 第一步：SDF（Signed Distance Field）生成
// =====================================================================
// 这是「折射」能从原理上成立的关键。
//
// 为什么需要 SDF：
//   毛玻璃只是「把背后的东西模糊一下」，本质上不改变光路。
//   液态玻璃的边缘会把背景「弯折」——就像真玻璃透镜那样。
//   要做到这一点，shader 必须知道：**每个像素离形状边缘有多远、
//   最近的那条边朝哪个方向**。这张「距离 + 方向」的贴图就是 SDF。
//
// 编码方式（与 iOS / 社区实现保持一致，方便 shader 复用）：
//   R 通道 = 归一化距离（1 = 在边界上，0 = 超过最大距离）
//   G 通道 = 法线 X 方向 [-1,1] 映射到 [0,1]（0.5 = 0）
//   B 通道 = 法线 Y 方向 [-1,1] 映射到 [0,1]
//   A 通道 = 255（SDF 图整体有效）
//
// 算法：8SSEDT（8-point Signed Sequential Euclidean Distance Transform）
//   两遍扫描：第一遍左上→右下，第二遍右下→左上。
//   每遍只比较「已处理过的邻居」，两遍合起来就能得到准确的欧氏距离。
//   这是 CPU 上性价比最高的 SDF 算法（JFA 更快但必须上 GPU，
//   而移动端多次离屏渲染的成本并不划算）。
//
// ⚠️ 性能铁律（决定了本方案能不能用）：
//   SDF 生成是纯 CPU 密集计算（O(像素数) 的两遍扫描）。
//   社区实测：若每帧都重新生成，高端机也只剩 15–20 fps。
//   因此**绝不能每帧算** —— 只在「形状变化时」算一次并缓存。
//   我们的玻璃元素（按钮、导航栏、标题栏）形状是静态的，
//   天然满足这个前提，于是每帧只剩 shader 合成这一件事。
// =====================================================================

/** 无穷远的「坐标」哨兵值。留足加法余量（见 compare()）。 */
private const val INF_COORD = 0x3FFFFFFF

/**
 * 生成 SDF 位图。
 *
 * @param source 形状图：**只需要 alpha 通道有意义**（不透明 = 形状内部）。
 *               通常由 View 的离屏渲染得到。
 * @param maxDistance 最大影响距离（像素）。超过这个距离的像素
 *        被视作「远离边缘」，折射/高光一律为 0。
 *        取值决定「玻璃厚度」的视觉尺度，经验值 40~80。
 * @return 编码好的 SDF 位图（尺寸与 source 相同）。
 */
fun generateSdf(source: Bitmap, maxDistance: Int = 60): Bitmap {
    val w = source.width
    val h = source.height
    if (w <= 0 || h <= 0) return source

    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)

    // ---- 网格：dx / dy 记录「最近边界点」的相对坐标，dist 记录其平方距离 ----
    // 用一维数组而不是二维，避免 JVM 上 [][] 的间接寻址开销。
    val dx = IntArray(w * h)
    val dy = IntArray(w * h)
    // ⚠️ dist 必须是 Long：
    //   dx/dy 在最坏情况下会累积到接近 Int.MAX_VALUE（当某个像素完全
    //   找不到任何种子点时），此时 ndx*ndx + ndy*ndy 会「整数溢出成负数」，
    //   于是 nd < dist[i] 恒成立，距离场被污染成垃圾。
    //   64 位累加后距离上限 = w*h 量级，永不溢出。
    val dist = LongArray(w * h)

    val inf = Long.MAX_VALUE

    // ---- 初始化：形状【内部】为种子点（距离 0），外部为无穷远 ----
    //
    // ⚠️ 方向不能搞反：
    //   我们要的是「外部像素离玻璃本体边缘有多远」——
    //   因为折射和高光发生在玻璃【外侧】（光从玻璃边缘弯折出去）。
    //   所以种子是「玻璃内部」（alpha 不透明），外部等距离场扩散过去。
    //   如果把外部当种子（更直觉但错误），整张距离场会整体反掉，
    //   玻璃中心反而变成「最远」，边缘高光会画到里面去。
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            val alpha = (pixels[i] ushr 24) and 0xFF
            if (alpha >= 128) {
                // 不透明 → 玻璃本体：距离源点
                dx[i] = 0
                dy[i] = 0
                dist[i] = 0
            } else {
                // 用 0x3FFFFFFF（约 10.7 亿）而不是 Long.MAX_VALUE：
                // compare() 里会做 dx+偏移，Long.MAX_VALUE 加正数会回绕成负数。
                // 这个值够大（远超任何真实距离）且留足加法余量。
                dx[i] = INF_COORD
                dy[i] = INF_COORD
                dist[i] = inf
            }
        }
    }

    // ---- 8SSEDT 第一遍：左上 → 右下 ----
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            if (x > 0) compare(dx, dy, dist, i, i - 1, -1, 0)
            if (y > 0) {
                compare(dx, dy, dist, i, i - w, 0, -1)
                if (x > 0) compare(dx, dy, dist, i, i - w - 1, -1, -1)
                if (x < w - 1) compare(dx, dy, dist, i, i - w + 1, 1, -1)
            }
        }
    }

    // ---- 8SSEDT 第二遍：右下 → 左上 ----
    for (y in h - 1 downTo 0) {
        for (x in w - 1 downTo 0) {
            val i = y * w + x
            if (x < w - 1) compare(dx, dy, dist, i, i + 1, 1, 0)
            if (y < h - 1) {
                compare(dx, dy, dist, i, i + w, 0, 1)
                if (x < w - 1) compare(dx, dy, dist, i, i + w + 1, 1, 1)
                if (x > 0) compare(dx, dy, dist, i, i + w - 1, -1, 1)
            }
        }
    }

    // ---- 编码成 RGB 位图 ----
    val out = IntArray(w * h)
    val maxDistanceF = maxDistance.toFloat()
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            val d = dist[i]

            // 归一化距离 normDist 的语义（与 shader 严格对齐，不能反）：
            //   1 = 玻璃【内部】（完全在玻璃里，向外折射区已结束）
            //   0 = 距离玻璃 >= maxDistance（完全不受影响）
            //   中间线性过渡 = 玻璃边缘外的那圈"坡"，折射/高光就发生在这
            //
            // dist[i] 是"到最近玻璃内部点的距离平方"：
            //   内部点 dist=0 → normDist=1
            //   外部点 dist 越大 → normDist 越小
            val normDist: Float = when {
                d == Long.MAX_VALUE -> 0f          // 完全找不到玻璃（图像里没有形状）
                else -> {
                    val px = sqrt(d.toDouble()).toFloat()   // 真实像素距离，内部点=0
                    (1.0 - px / maxDistanceF).coerceIn(0f, 1f)
                }
            }

            // 法线方向：由「最近的玻璃内部点」指向当前点，即从玻璃指向外的方向。
            // 映射到 [0,1]（0.5 = 0），便于塞进 8bit 通道。
            // 内部点 dx=dy=0 → len=0 → 保持 0.5/0.5（法线中性），
            // 这对玻璃正中间是正确的：那里没有方向可言。
            var nx = 0.5f
            var ny = 0.5f
            if (d > 0L && d != Long.MAX_VALUE) {
                val vx = dx[i]
                val vy = dy[i]
                if (vx != 0 || vy != 0) {
                    val len = sqrt((vx.toDouble() * vx + vy.toDouble() * vy))
                    if (len > 0.0) {
                        nx = ((vx / len) + 1.0).toFloat() * 0.5f
                        ny = ((vy / len) + 1.0).toFloat() * 0.5f
                    }
                }
            }

            val r = (normDist * 255f).toInt().coerceIn(0, 255)
            val g = (nx * 255f).toInt().coerceIn(0, 255)
            val b = (ny * 255f).toInt().coerceIn(0, 255)
            out[i] = Color.argb(255, r, g, b)
        }
    }

    return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
}

/**
 * 比较邻居：如果「从邻居再加上偏移」比当前记录的距离更近，就更新。
 *
 * @param nx 邻居到当前点的 x 偏移（单位：格，-1/0/1）
 * @param ny 同上 y
 */
private inline fun compare(
    dx: IntArray,
    dy: IntArray,
    dist: LongArray,
    i: Int,
    ni: Int,
    nx: Int,
    ny: Int
) {
    val dNi = dist[ni]
    if (dNi == Long.MAX_VALUE) return
    val ndx = dx[ni] + nx
    val ndy = dy[ni] + ny
    // ⚠️ 先转 Long 再乘：ndx 可能高达 ~2^30，平方后 2^60，仍在 Long 范围内。
    // 若用 Int 在这行就会溢出成负数。
    val ndxL = ndx.toLong()
    val ndyL = ndy.toLong()
    val nd = ndxL * ndxL + ndyL * ndyL
    if (nd < dist[i]) {
        dist[i] = nd
        dx[i] = ndx
        dy[i] = ndy
    }
}