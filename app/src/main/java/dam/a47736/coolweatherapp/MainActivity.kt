package dam.a47736.coolweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources.Theme
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {

    var day: Boolean = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var city : String


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT->
                if(day) setTheme(R.style.Theme_Day)
                else setTheme(R.style.Theme_Night)
            Configuration.ORIENTATION_LANDSCAPE->
                if(day) setTheme(R.style.Theme_Day_Land)
                else setTheme(R.style.Theme_Night_Land)
            Configuration.ORIENTATION_UNDEFINED ->{}
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val refresh = findViewById<Button>(R.id.refresh_button)
        refresh.setOnClickListener{
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnClickListener
            }
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        city = nomeLocalizacao(latitude, longitude)
                        fetchWeatherData(latitude.toFloat(), longitude.toFloat()).start()
                    }
                }

        }

    }
    private fun nomeLocalizacao(lat: Double, long: Double) : String{
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(lat, long, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val cityName = address.locality
                    Log.d("Location", "Ciudad: $cityName")
                    // Aquí puedes usar los datos de la dirección como desees
                    return cityName
                } else {
                    Log.e("Location", "No se encontraron direcciones para la ubicación proporcionada.")
                }
            }
        } catch (e: Exception) {
            Log.e("Location", "Error al obtener la dirección: ${e.message}")
        }
        return "nada"
    }
    private fun weatherAPICall(lat: Float, long: Float) : WeatherData {
        val reqString = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=${lat}&longitude=${long}&")
            //append("current_weather=true&")
            append("current=temperature_2m,relative_humidity_2m," +
                    "precipitation,weather_code,wind_speed_10m,is_day")
        }
        val url = URL(reqString)
        url.openStream().use {
            return Gson().fromJson(InputStreamReader(it,"UTF-8"),WeatherData::class.java)
        }
    }
    private fun fetchWeatherData(lat: Float, long: Float) : Thread {
        return Thread {
            val weather = weatherAPICall(lat,long)
            updateUI(weather)
        }
    }
    private fun updateUI(request: WeatherData)
    {
        runOnUiThread {

            val weatherImage : ImageView = findViewById(R.id.weatherImage)
            val wind : TextView = findViewById(R.id.ventoValue)
            val temperatura : TextView = findViewById(R.id.temperatureValue)
            val precipitacao : TextView = findViewById(R.id.precipitationVal)
            val humidade : TextView = findViewById(R.id.humidadeVal)
            val cidade : TextView = findViewById(R.id.cidade)

            temperatura.text = request.current.temperature_2m.toString()
            wind.text = request.current.wind_speed_10m.toString() + " Km/h"
            temperatura.text = request.current.temperature_2m.toString() + "ºC"
            precipitacao.text = request.current.precipitation.toString() + "mm"
            humidade.text = request.current.relative_humidity_2m.toString() + "%"
            cidade.text = city
            day = request.current.is_day == 1
            day = false
            setTheme(R.style.Theme_Night)
            val mapt = getWeatherCodeMap()
            val wCode = mapt.get(1)//request.current.weather_code)
            val wImage = when(wCode) {
                WMO_WeatherCode.CLEAR_SKY,
                WMO_WeatherCode.MAINLY_CLEAR,
                WMO_WeatherCode.PARTLY_CLOUDY->if(day) wCode?.image+"day" else wCode?.image+"night"
                else-> wCode?.image
            }
            try {
                if(day) setTheme(R.style.Theme_Day) else setTheme(R.style.Theme_Night)
            }catch (e:Exception){
                Log.e("TAG", "Error: ${e.message}", e)
            }

            //cidade.text = wCode?.image + " "+ cidade.text
            val res = getResources()
            val resID = res.getIdentifier(wImage, "drawable",getPackageName())
            val drawable = this.getDrawable(resID)
            weatherImage.setImageDrawable(drawable)

        }

    }

}