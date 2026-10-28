package com.leandro.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class MainActivity : Activity() {

    private class BgTheme(val name: String, val colors: IntArray)

    // Fundos disponíveis (botão "Trocar fundo"). Para criar o seu, adicione uma linha aqui.
    private val themes = listOf(
        BgTheme("OLED preto", intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt())),
        BgTheme("Noite", intArrayOf(0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF2C5364.toInt())),
        BgTheme("Aurora", intArrayOf(0xFF0F0C29.toInt(), 0xFF302B63.toInt(), 0xFF24243E.toInt())),
        BgTheme("Pôr do sol", intArrayOf(0xFF2B1B3D.toInt(), 0xFF5B3A6B.toInt(), 0xFF8E4F62.toInt())),
        BgTheme("Floresta", intArrayOf(0xFF0B2B26.toInt(), 0xFF134E5E.toInt(), 0xFF3F8F6B.toInt())),
        BgTheme("Grafite", intArrayOf(0xFF1A1A1A.toInt(), 0xFF2E3033.toInt(), 0xFF414345.toInt()))
    )

    private lateinit var prefs: SharedPreferences
    private lateinit var root: View
    private lateinit var scroll: ScrollView
    private lateinit var greeting: TextView
    private lateinit var rvShortcuts: RecyclerView

    private lateinit var wEmoji: TextView
    private lateinit var wTemp: TextView
    private lateinit var wSummary: TextView
    private lateinit var wDetails: TextView
    private lateinit var wToday: TextView
    private lateinit var forecastRow: LinearLayout
    private var lastWeatherOk = 0L
    private var weatherLoaded = false

    private val handler = Handler(Looper.getMainLooper())

    private val weatherTicker = object : Runnable {
        override fun run() {
            fetchWeather()
            handler.postDelayed(this, WEATHER_REFRESH_MS)
        }
    }

    private val shortcutAdapter = ShortcutAdapter()
    private val favAdapter = AppAdapter(::launchApp, ::toggleFavorite)
    private val tvAdapter = AppAdapter(::launchApp, ::toggleFavorite)
    private val otherAdapter = AppAdapter(::launchApp, ::toggleFavorite)

    private var tvApps: List<AppEntry> = emptyList()
    private var otherApps: List<AppEntry> = emptyList()

    private val ticker = object : Runnable {
        override fun run() {
            updateGreeting()
            handler.postDelayed(this, 60_000)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        root = findViewById(R.id.root)
        scroll = findViewById(R.id.scroll)
        greeting = findViewById(R.id.greeting)
        rvShortcuts = findViewById(R.id.rv_shortcuts)
        wEmoji = findViewById(R.id.w_emoji)
        wTemp = findViewById(R.id.w_temp)
        wSummary = findViewById(R.id.w_summary)
        wDetails = findViewById(R.id.w_details)
        wToday = findViewById(R.id.w_today)
        forecastRow = findViewById(R.id.forecast_row)

        setupRow(R.id.rv_shortcuts, shortcutAdapter)
        setupRow(R.id.rv_fav, favAdapter)
        setupRow(R.id.rv_tv, tvAdapter)
        setupRow(R.id.rv_other, otherAdapter)

        shortcutAdapter.submit(buildShortcuts())

        applyBackground()
        updateGreeting()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)

        // Atualiza o clima ao voltar para a tela inicial, sem exagerar nas consultas
        handler.removeCallbacks(weatherTicker)
        val elapsed = System.currentTimeMillis() - lastWeatherOk
        val delay = if (lastWeatherOk == 0L) 0L else (WEATHER_REFRESH_MS - elapsed).coerceAtLeast(0L)
        handler.postDelayed(weatherTicker, delay)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(weatherTicker)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
    }

    // Apertar HOME com o launcher já aberto: volta ao topo
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        scroll.smoothScrollTo(0, 0)
        rvShortcuts.requestFocus()
    }

    // Um launcher não deve fechar com o botão Voltar
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        scroll.smoothScrollTo(0, 0)
    }

    // ---------------------------------------------------------------- UI

    private fun setupRow(id: Int, rowAdapter: RecyclerView.Adapter<*>) {
        findViewById<RecyclerView>(id).apply {
            layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
            adapter = rowAdapter
            itemAnimator = null
        }
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val salute = when {
            hour < 12 -> "Bom dia"
            hour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
        greeting.text = "$salute, ${getString(R.string.owner_name)}"
    }

    private fun applyBackground() {
        val index = prefs.getInt(KEY_BG, 0).coerceIn(0, themes.size - 1)
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, themes[index].colors)
    }

    private fun nextBackground() {
        val next = (prefs.getInt(KEY_BG, 0) + 1) % themes.size
        prefs.edit().putInt(KEY_BG, next).apply()
        applyBackground()
        toast("Fundo: ${themes[next].name}")
    }

    // ------------------------------------------------------------- Clima

    private fun fetchWeather() {
        val lat = getString(R.string.city_latitude)
        val lon = getString(R.string.city_longitude)
        Thread {
            val data = WeatherClient.fetch(lat, lon)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (data != null) {
                    lastWeatherOk = System.currentTimeMillis()
                    weatherLoaded = true
                    bindWeather(data)
                } else {
                    if (!weatherLoaded) wSummary.text = getString(R.string.weather_offline)
                    // Sem internet: tenta de novo em 5 minutos
                    handler.removeCallbacks(weatherTicker)
                    handler.postDelayed(weatherTicker, WEATHER_RETRY_MS)
                }
            }
        }.start()
    }

    private fun bindWeather(d: WeatherData) {
        wEmoji.text = d.emoji
        wTemp.text = "${d.temp}°"
        wSummary.text = "${getString(R.string.city_name)}  ·  ${d.description}"
        wDetails.text = "Sensação ${d.feelsLike}°     Umidade ${d.humidity}%     Vento ${d.windKmh} km/h"
        wToday.text = "Hoje: máx ${d.todayMax}°  mín ${d.todayMin}°     Chance de chuva ${d.rainChance}%"

        forecastRow.removeAllViews()
        for (day in d.days) {
            val v = layoutInflater.inflate(R.layout.item_forecast, forecastRow, false)
            v.findViewById<TextView>(R.id.fc_day).text = day.dayLabel
            v.findViewById<TextView>(R.id.fc_emoji).text = day.emoji
            v.findViewById<TextView>(R.id.fc_max).text = "${day.max}°"
            v.findViewById<TextView>(R.id.fc_min).text = "${day.min}°"
            v.findViewById<TextView>(R.id.fc_rain).text = "${day.rainChance}%"
            forecastRow.addView(v)
        }
    }

    // ----------------------------------------------------------- Atalhos

    private fun buildShortcuts(): List<ShortcutEntry> = listOf(
        ShortcutEntry("⚙  Configurações") { openSettings(Settings.ACTION_SETTINGS) },
        ShortcutEntry("📶  Wi-Fi") { openSettings(Settings.ACTION_WIFI_SETTINGS) },
        ShortcutEntry("🔵  Bluetooth") { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
        ShortcutEntry("🖥  Tela") { openSettings(Settings.ACTION_DISPLAY_SETTINGS) },
        ShortcutEntry("🎨  Trocar fundo") { nextBackground() }
    )

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast("Esse atalho não está disponível nesta TV")
        }
    }

    // -------------------------------------------------------------- Apps

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val self = packageName

            val tvQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            val tvInfos = pm.queryIntentActivities(tvQuery, 0)
                .filter { it.activityInfo.packageName != self }
            val tvPackages = tvInfos.map { it.activityInfo.packageName }.toSet()

            val tv = tvInfos.map { ri ->
                val banner = ri.activityInfo.loadBanner(pm)
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                AppEntry(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    launchIntent = intent,
                    image = banner ?: ri.loadIcon(pm),
                    isBanner = banner != null
                )
            }.sortedBy { it.label.lowercase() }

            val otherQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val other = pm.queryIntentActivities(otherQuery, 0)
                .filter {
                    val p = it.activityInfo.packageName
                    p != self && p !in tvPackages
                }
                .map { ri ->
                    val intent = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    AppEntry(
                        label = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        launchIntent = intent,
                        image = ri.loadIcon(pm),
                        isBanner = false
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }

            runOnUiThread {
                tvApps = tv
                otherApps = other
                refreshRows()
                if (currentFocus == null) rvShortcuts.requestFocus()
            }
        }.start()
    }

    private fun refreshRows() {
        val favs = favoriteSet()
        val favList = (tvApps + otherApps)
            .filter { it.packageName in favs }
            .sortedBy { it.label.lowercase() }

        favAdapter.submit(favList, favs)
        tvAdapter.submit(tvApps, favs)
        otherAdapter.submit(otherApps, favs)

        setSectionVisible(R.id.title_fav, R.id.rv_fav, favList.isNotEmpty())
        setSectionVisible(R.id.title_tv, R.id.rv_tv, tvApps.isNotEmpty())
        setSectionVisible(R.id.title_other, R.id.rv_other, otherApps.isNotEmpty())
    }

    private fun setSectionVisible(titleId: Int, rowId: Int, visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        findViewById<View>(titleId).visibility = v
        findViewById<View>(rowId).visibility = v
    }

    private fun launchApp(entry: AppEntry) {
        try {
            startActivity(entry.launchIntent)
        } catch (e: Exception) {
            toast("Não foi possível abrir ${entry.label}")
        }
    }

    // --------------------------------------------------------- Favoritos

    private fun favoriteSet(): Set<String> =
        prefs.getStringSet(KEY_FAVS, emptySet()) ?: emptySet()

    private fun toggleFavorite(entry: AppEntry) {
        val favs = HashSet<String>(favoriteSet())
        val added = favs.add(entry.packageName)
        if (!added) favs.remove(entry.packageName)
        prefs.edit().putStringSet(KEY_FAVS, favs).apply()
        toast(if (added) "★ ${entry.label} adicionado aos favoritos" else "${entry.label} removido dos favoritos")
        refreshRows()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val KEY_FAVS = "favorites"
        private const val KEY_BG = "background"
        private const val WEATHER_REFRESH_MS = 30 * 60 * 1000L
        private const val WEATHER_RETRY_MS = 5 * 60 * 1000L
    }
}
