package com.oopnv70.uilab.settings

import android.content.Context

// =====================================================================
// 应用设置：持久化 + 读取
// =====================================================================
// 为什么不用 DataStore：
//   与 LocationPermissionStore 保持一致 —— 本项目目前所有持久化都用
//   SharedPreferences（系统自带、零新增依赖）。设置项少、读写频率低，
//   SharedPreferences 完全够用，且不引入 coroutine/Flow 的生命周期复杂度。
//   依赖表里的 androidx-datastore-preferences 至今未被使用，
//   本次也**不**借机引入 —— 不要为了「看起来现代」而改。
//
// 设计原则：设置的「唯一真相」在 SharedPreferences；
//   Compose 侧通过 AppSettingsState 快照读取，改设置走 setXxx() 写回。
// =====================================================================

/** SharedPreferences 文件名（与定位那个分开，避免键名混淆）。 */
private const val PREFS_NAME = "ui_lab_settings"

/** 主题模式：跟随系统 / 强制浅色 / 强制深色。 */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色");

    companion object {
        /** 按存储值还原，未知值一律回退「跟随系统」（避免脏数据导致崩溃）。 */
        fun fromId(id: String?): ThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/** 温度单位。 */
enum class TemperatureUnit(val id: String, val label: String, val suffix: String) {
    CELSIUS("celsius", "摄氏度", "°C"),
    FAHRENHEIT("fahrenheit", "华氏度", "°F");

    companion object {
        fun fromId(id: String?): TemperatureUnit =
            entries.firstOrNull { it.id == id } ?: CELSIUS
    }
}

/**
 * 主题**风格**（注意：这是与 [ThemeMode] 完全正交的另一个维度）。
 *
 * - [ThemeMode] 管「亮还是暗」——系统级配色方案。
 * - [ThemeStyle] 管「材质长什么样」——浮动层是用实心卡片，还是真玻璃。
 *
 * 两者相乘才是最终外观（例如「深色 + 液态玻璃」）。
 * 之所以分开，是因为用户完全可能想要「浅色 + 默认材质」这种组合，
 * 如果把它们塞进一个枚举会变成 3×2=6 项的笛卡尔积，无法维护。
 */
enum class ThemeStyle(val id: String, val label: String, val subtitle: String) {
    DEFAULT("default", "默认", "实心卡片，性能最好"),
    LIQUID_GLASS("liquid_glass", "液态玻璃", "真折射 + 高光，实时渲染");

    companion object {
        fun fromId(id: String?): ThemeStyle =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * 设置快照：一次性读出全部设置项，供 Compose 渲染。
 *
 * 用不可变 data class 而不是让 UI 直接读 SharedPreferences，
 * 是为了让「读取」集中在一处、便于以后加项，也便于做状态比对。
 */
data class AppSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val themeStyle: ThemeStyle = ThemeStyle.DEFAULT,
    val sweepAnimation: Boolean = false
)

// ---- SharedPreferences 键名 ----
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_DYNAMIC_COLOR = "dynamic_color"
private const val KEY_TEMPERATURE_UNIT = "temperature_unit"
private const val KEY_THEME_STYLE = "theme_style"
private const val KEY_SWEEP_ANIMATION = "sweep_animation"

/**
 * 读取当前设置。
 *
 * ⚠️ 默认值必须与 [AppSettingsState] 的默认值一致，否则会出现
 * 「首次进来显示 A、重启后突然变 B」的诡异现象。
 */
fun readAppSettings(context: Context): AppSettingsState {
    val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return AppSettingsState(
        themeMode = ThemeMode.fromId(prefs.getString(KEY_THEME_MODE, null)),
        // ⚠️ 动态取色默认 false：见 Theme.kt 注释 —— 开了会覆盖本项目
        // 自己调过的配色。这里保持与 Theme 默认值一致。
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
        temperatureUnit = TemperatureUnit.fromId(prefs.getString(KEY_TEMPERATURE_UNIT, null)),
        // ⚠️ 液态玻璃默认关闭。它是「重度渲染」选项，默认开启会让
        // 首次启动的机器（尤其低端）直接掉帧，用户会觉得 App 卡。
        // 想要的人自己去设置里打开 —— 这是有意的产品取舍。
        themeStyle = ThemeStyle.fromId(prefs.getString(KEY_THEME_STYLE, null)),
        // 切换「经过中间页」动画默认关闭：连续扫过在部分设备上仍有卡顿感，
        // 想要的人自己去「隐藏设置」里打开。
        sweepAnimation = prefs.getBoolean(KEY_SWEEP_ANIMATION, false)
    )
}

/** 写入主题模式。 */
fun setThemeMode(context: Context, mode: ThemeMode) {
    prefs(context).edit().putString(KEY_THEME_MODE, mode.id).apply()
}

/** 写入动态取色开关。 */
fun setDynamicColor(context: Context, enabled: Boolean) {
    prefs(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
}

/** 写入温度单位。 */
fun setTemperatureUnit(context: Context, unit: TemperatureUnit) {
    prefs(context).edit().putString(KEY_TEMPERATURE_UNIT, unit.id).apply()
}

/**
 * 一次性写入完整设置快照。
 *
 * 给 UI 层（设置页）统一调用：改哪一项都整体写回，避免 MainActivity
 * 里散落三个 setter 的调用。用同一个 editor 提交（一次 IO）。
 */
fun persistAppSettings(context: Context, state: AppSettingsState) {
    prefs(context).edit()
        .putString(KEY_THEME_MODE, state.themeMode.id)
        .putBoolean(KEY_DYNAMIC_COLOR, state.dynamicColor)
        .putString(KEY_TEMPERATURE_UNIT, state.temperatureUnit.id)
        .putString(KEY_THEME_STYLE, state.themeStyle.id)
        .putBoolean(KEY_SWEEP_ANIMATION, state.sweepAnimation)
        .apply()
}

private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
