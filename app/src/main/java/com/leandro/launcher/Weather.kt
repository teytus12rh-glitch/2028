package com.leandro.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class DayForecast(
    val dayLabel: String,
    val emoji: String,
    val max: Int,
    val min: Int,
    val rainChance: Int
)

class WeatherData(
    val temp: Int,
    val feelsLike: Int,
    val humidity: Int,
    val windKmh: Int,
    val description: String,
    val emoji: String,
    val todayMax: Int,
    val todayMin: Int,
    val rainChance: Int,
    val days: List<DayForecast>
)

/** Busca o clima no Open-Meteo (gratuito, sem chave de API). */
object WeatherClient {

    fun fetch(lat: String, lon: String): WeatherData? {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m," +
            "weather_code,wind_speed_10m,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=6"

        var conn: HttpURLConnection? = null
        return try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) {
                null
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parse(JSONObject(body))
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parse(json: JSONObject): WeatherData {
        val cur = json.getJSONObject("current")
        val isDay = cur.optInt("is_day", 1) == 1
        val (desc, emoji) = describe(cur.getInt("weather_code"), isDay)

        val daily = json.getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxs = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")
        val rains = daily.getJSONArray("precipitation_probability_max")

        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outFmt = SimpleDateFormat("EEE", Locale("pt", "BR"))

        val days = ArrayList<DayForecast>()
        for (i in 1 until times.length()) {
            val date = inFmt.parse(times.getString(i))
            val label = if (date != null) {
                outFmt.format(date).replace(".", "").replaceFirstChar { it.uppercase() }
            } else {
                ""
            }
            days.add(
                DayForecast(
                    dayLabel = label,
                    emoji = describe(codes.getInt(i), true).second,
                    max = maxs.getDouble(i).roundToInt(),
                    min = mins.getDouble(i).roundToInt(),
                    rainChance = rains.optInt(i, 0)
                )
            )
        }

        return WeatherData(
            temp = cur.getDouble("temperature_2m").roundToInt(),
            feelsLike = cur.getDouble("apparent_temperature").roundToInt(),
            humidity = cur.getInt("relative_humidity_2m"),
            windKmh = cur.getDouble("wind_speed_10m").roundToInt(),
            description = desc,
            emoji = emoji,
            todayMax = maxs.getDouble(0).roundToInt(),
            todayMin = mins.getDouble(0).roundToInt(),
            rainChance = rains.optInt(0, 0),
            days = days
        )
    }

    /** Código WMO do Open-Meteo -> (descrição em português, emoji). */
    private fun describe(code: Int, isDay: Boolean): Pair<String, String> = when (code) {
        0 -> Pair("Céu limpo", if (isDay) "☀️" else "🌙")
        1 -> Pair("Quase limpo", if (isDay) "🌤️" else "🌙")
        2 -> Pair("Parcialmente nublado", "⛅")
        3 -> Pair("Nublado", "☁️")
        45, 48 -> Pair("Neblina", "🌫️")
        51, 53, 55 -> Pair("Garoa", "🌦️")
        56, 57 -> Pair("Garoa congelante", "🌧️")
        61 -> Pair("Chuva fraca", "🌧️")
        63 -> Pair("Chuva moderada", "🌧️")
        65 -> Pair("Chuva forte", "🌧️")
        66, 67 -> Pair("Chuva congelante", "🌧️")
        71, 73, 75, 77 -> Pair("Neve", "❄️")
        80 -> Pair("Pancadas de chuva fracas", "🌦️")
        81 -> Pair("Pancadas de chuva", "🌧️")
        82 -> Pair("Pancadas de chuva fortes", "🌧️")
        85, 86 -> Pair("Pancadas de neve", "❄️")
        95 -> Pair("Trovoada", "⛈️")
        96, 99 -> Pair("Trovoada com granizo", "⛈️")
        else -> Pair("Tempo instável", "🌥️")
    }
}
