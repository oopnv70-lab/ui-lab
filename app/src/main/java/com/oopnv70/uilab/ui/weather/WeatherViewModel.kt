package com.oopnv70.uilab.ui.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oopnv70.uilab.data.GeoPlace
import com.oopnv70.uilab.data.RealWeather
import com.oopnv70.uilab.data.WeatherKind
import com.oopnv70.uilab.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =====================================================================
// 天气 ViewModel：真实数据的唯一入口
// =====================================================================
// 职责：
//   1. 按关键词【网络搜索】城市（不再有预置城市表）
//   2. 按坐标拉真实天气
//   3. 维护「城市列表 + 当前选中城市 → 当前天气」的一致性
//
// UI 只订阅 StateFlow，不直接碰网络。
// =====================================================================

/** 一个「已加入列表」的城市。UI 用它渲染城市卡片。 */
data class SavedCity(
    /** 显示名（如「深圳」）。 */
    val name: String,
    /** 副标题（如「广东省 · 中国」）。 */
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    /** 是否是定位到的当前位置（列表里最多一个，不可删除）。 */
    val isCurrent: Boolean = false,
    /** 该城市的实况温度（没拿到时为 null → UI 显示「—」）。 */
    val temperature: Int? = null,
    /** 该城市的天气归类（用于图标）。 */
    val kind: WeatherKind? = null,
    /** 该城市是否正在加载天气。 */
    val loading: Boolean = false
)

/** 城市搜索的 UI 状态。 */
sealed interface CitySearchState {
    /** 还没搜（输入框为空）。 */
    data object Idle : CitySearchState
    /** 正在请求网络。 */
    data object Loading : CitySearchState
    /** 有结果（可能为空列表 → UI 显示「没有找到」）。 */
    data class Done(val results: List<GeoPlace>) : CitySearchState
    /** 请求失败。 */
    data class Failed(val message: String) : CitySearchState
}

/** 天气加载状态。 */
sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Ready(val weather: RealWeather, val cityName: String) : WeatherUiState
    data class Failed(val message: String) : WeatherUiState
}

class WeatherViewModel : ViewModel() {

    private companion object {
        const val TAG = "UiLab.WeatherVM"
    }

    // ---- 城市列表 ----
    private val _cities = MutableStateFlow<List<SavedCity>>(emptyList())
    val cities: StateFlow<List<SavedCity>> = _cities.asStateFlow()

    // ---- 当前选中城市名（null = 跟随定位城市） ----
    private val _selectedCityName = MutableStateFlow<String?>(null)
    val selectedCityName: StateFlow<String?> = _selectedCityName.asStateFlow()

    // ---- 当前城市的真实天气 ----
    private val _weather = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    // ---- 城市搜索 ----
    private val _search = MutableStateFlow<CitySearchState>(CitySearchState.Idle)
    val search: StateFlow<CitySearchState> = _search.asStateFlow()

    /** 搜索防抖任务（输入时每敲一个字都会取消上一个）。 */
    private var searchJob: Job? = null

    /** 当前生效城市对应的坐标（用于刷新）。 */
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    // -----------------------------------------------------------------
    // 1) 关键词搜索城市（走网络，无预置表）
    // -----------------------------------------------------------------

    /** 输入变化时调用；内部做 350ms 防抖，避免每敲一个字都发请求。 */
    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            _search.value = CitySearchState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _search.value = CitySearchState.Loading
            val hits = WeatherRepository.searchCities(q, count = 10)
            Log.d(TAG, "搜索「$q」→ ${hits.size} 条")
            _search.value = CitySearchState.Done(hits)
        }
    }

    /** 清空搜索。 */
    fun clearSearch() {
        searchJob?.cancel()
        _search.value = CitySearchState.Idle
    }

    // -----------------------------------------------------------------
    // 2) 添加 / 删除 / 选择城市
    // -----------------------------------------------------------------

    /**
     * 把搜索结果里的一个地点加入城市列表，并拉它的真实天气。
     * 已存在同名城市时不重复添加，只选中它。
     */
    fun addCity(place: GeoPlace) {
        val existing = _cities.value.firstOrNull { it.name == place.name }
        if (existing != null) {
            selectCity(existing)
            return
        }
        val item = SavedCity(
            name = place.name,
            subtitle = place.subtitle,
            latitude = place.latitude,
            longitude = place.longitude,
            loading = true
        )
        _cities.update { it + item }
        _selectedCityName.value = place.name
        loadWeatherFor(place.name, place.latitude, place.longitude)
        // 顺便把该城市的温度补进卡片
        viewModelScope.launch {
            WeatherRepository.fetchWeather(place.latitude, place.longitude)
                .onSuccess { w ->
                    _cities.update { list ->
                        list.map {
                            if (it.name == place.name) {
                                it.copy(temperature = w.temperature, kind = w.kind, loading = false)
                            } else it
                        }
                    }
                }
                .onFailure {
                    _cities.update { list ->
                        list.map {
                            if (it.name == place.name) it.copy(loading = false) else it
                        }
                    }
                }
        }
    }

    /** 从列表移除（定位城市不可删）。 */
    fun removeCity(name: String) {
        if (_cities.value.firstOrNull { it.name == name }?.isCurrent == true) return
        _cities.update { list -> list.filterNot { it.name == name } }
        if (_selectedCityName.value == name) {
            _selectedCityName.value = null
            selectFirstAvailable()
        }
    }

    /** 选中某城市 → 拉它的真实天气。 */
    fun selectCity(city: SavedCity) {
        // 已经是这个城市的天气就不重复请求
        val ready = _weather.value as? WeatherUiState.Ready
        if (ready != null && ready.cityName == city.name) {
            _selectedCityName.value = city.name
            return
        }
        _selectedCityName.value = city.name
        loadWeatherFor(city.name, city.latitude, city.longitude)
        // 顺带刷新卡片上的小温度
        if (city.temperature == null) {
            viewModelScope.launch {
                WeatherRepository.fetchWeather(city.latitude, city.longitude).onSuccess { r ->
                    _cities.update { list ->
                        list.map {
                            if (it.name == city.name) {
                                it.copy(temperature = r.temperature, kind = r.kind, loading = false)
                            } else it
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // 3) 定位成功 → 把真实城市变成列表里的「当前位置」
    // -----------------------------------------------------------------

    /**
     * 定位到真实坐标后调用。
     *
     * 注意：定位只给了「坐标 + 城市名」，这里立刻去拉**该坐标的真实天气**，
     * 所以温度一定是定位当地的真实温度，不是写死的北京 26°。
     */
    fun setLocatedCity(name: String, latitude: Double, longitude: Double) {
        val display = name.ifBlank { "当前位置" }
        val old = _cities.value.firstOrNull { it.isCurrent }
        val item = SavedCity(
            name = display,
            subtitle = "当前位置",
            latitude = latitude,
            longitude = longitude,
            isCurrent = true,
            loading = true
        )
        _cities.update { list ->
            // 替换旧的定位项；若旧的被手动选中过，保持选中新名字
            val without = list.filterNot { it.isCurrent }
            listOf(item) + without
        }
        if (_selectedCityName.value == null || _selectedCityName.value == old?.name) {
            _selectedCityName.value = display
            loadWeatherFor(display, latitude, longitude)
        }
        // 补上卡片温度
        viewModelScope.launch {
            WeatherRepository.fetchWeather(latitude, longitude)
                .onSuccess { r ->
                    _cities.update { list ->
                        list.map {
                            if (it.isCurrent) {
                                it.copy(temperature = r.temperature, kind = r.kind, loading = false)
                            } else it
                        }
                    }
                }
                .onFailure {
                    _cities.update { list ->
                        list.map { if (it.isCurrent) it.copy(loading = false) else it }
                    }
                }
        }
    }

    /** 重新拉取当前城市的天气（下拉刷新 / 手动重试）。 */
    fun refresh() {
        val (lat, lon) = currentLat to currentLon
        if (lat != null && lon != null) {
            viewModelScope.launch {
                val name = currentCityName() ?: return@launch
                loadWeatherFor(name, lat, lon)
            }
        } else {
            // 没有当前坐标 → 用定位城市
            val cur = _cities.value.firstOrNull { it.isCurrent } ?: _cities.value.firstOrNull()
            if (cur != null) loadWeatherFor(cur.name, cur.latitude, cur.longitude)
        }
    }

    // -----------------------------------------------------------------
    // 内部
    // -----------------------------------------------------------------

    private fun currentCityName(): String? =
        _selectedCityName.value
            ?: _cities.value.firstOrNull { it.isCurrent }?.name
            ?: _cities.value.firstOrNull()?.name

    private fun selectFirstAvailable() {
        val target = _cities.value.firstOrNull { it.isCurrent } ?: _cities.value.firstOrNull()
        if (target != null) {
            _selectedCityName.value = target.name
            loadWeatherFor(target.name, target.latitude, target.longitude)
        } else {
            _weather.value = WeatherUiState.Idle
        }
    }

    private fun loadWeatherFor(name: String, lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon
        _weather.value = WeatherUiState.Loading
        viewModelScope.launch {
            WeatherRepository.fetchWeather(lat, lon)
                .onSuccess { w ->
                    Log.d(TAG, "「$name」→ ${w.temperature}° ${w.conditionText}")
                    _weather.value = WeatherUiState.Ready(w, name)
                }
                .onFailure { e ->
                    Log.w(TAG, "「$name」取天气失败: ${e.message}")
                    _weather.value = WeatherUiState.Failed(e.message ?: "网络请求失败")
                }
        }
    }
}
