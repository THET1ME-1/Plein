package app.plein.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Погода из Open-Meteo.
 *
 * Ключ не нужен, лицензия свободная — ровно как у Overmorrow, только без
 * привязки к чужому сервису. Координаты берём из последней известной точки:
 * лаунчер не имеет права держать геолокацию включённой ради строки на экране.
 */
class Weather(private val context: Context) {

    data class Now(val celsius: Int, val code: Int)

    suspend fun current(provider: String = "open-meteo"): Now? = withContext(Dispatchers.IO) {
        val point = lastKnownPoint() ?: return@withContext null
        val url = when (provider) {
            "met.no" -> URL(
                "https://api.met.no/weatherapi/locationforecast/2.0/compact" +
                    "?lat=${point.first}&lon=${point.second}"
            )
            else -> URL(
                "https://api.open-meteo.com/v1/forecast?latitude=${point.first}" +
                    "&longitude=${point.second}&current=temperature_2m,weather_code"
            )
        }
        val body = runCatching {
            (url.openConnection() as HttpURLConnection).run {
                connectTimeout = 6000
                readTimeout = 8000
                setRequestProperty("User-Agent", "PleinLauncher/0.1")
                inputStream.bufferedReader().use { it.readText() }.also { disconnect() }
            }
        }.getOrNull() ?: return@withContext null

        runCatching {
            if (provider == "met.no") {
                val details = JSONObject(body).getJSONObject("properties")
                    .getJSONArray("timeseries").getJSONObject(0)
                    .getJSONObject("data").getJSONObject("instant").getJSONObject("details")
                Now(celsius = details.getDouble("air_temperature").toInt(), code = 0)
            } else {
                val current = JSONObject(body).getJSONObject("current")
                Now(
                    celsius = current.getDouble("temperature_2m").toInt(),
                    code = current.optInt("weather_code"),
                )
            }
        }.getOrNull()
    }

    private fun lastKnownPoint(): Pair<Double, Double>? {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val location = runCatching {
            manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        }.getOrNull() ?: return null

        return location.latitude to location.longitude
    }

    /** Значок погоды словами WMO: коды сгруппированы по смыслу. */
    fun glyph(code: Int): String = when (code) {
        0 -> "☀"
        1, 2 -> "⛅"
        3 -> "☁"
        in 45..48 -> "🌫"
        in 51..67 -> "🌧"
        in 71..77 -> "❄"
        in 80..82 -> "🌦"
        in 95..99 -> "⛈"
        else -> "·"
    }
}
