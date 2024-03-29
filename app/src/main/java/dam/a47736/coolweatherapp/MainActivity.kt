package dam.a47736.coolweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var day: Boolean = true

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var city : String? = null

    private var weatherImage : ImageView? = null
    private var wind : TextView? = null
    private var temperatura : TextView? = null
    private var precipitacao : TextView? = null
    private var humidade : TextView? = null
    private var cidade : TextView? = null




    private val REQUEST_LOCATION_PERMISSION = 100


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
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                /*
                TODO: Consider calling
                   ActivityCompat#requestPermissions
                 here to request the missing permissions, and then overriding
                   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                          int[] grantResults)
                 to handle the case where the user grants the permission. See the documentation
                 for ActivityCompat#requestPermissions for more details.*/
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
        println("onCreate")

    }

    override fun onStart() {
        super.onStart()
        println("onStart")
    }

    override fun onResume() {
        super.onResume()
        println( "onResume")
    }

    override fun onPause() {
        super.onPause()
        println("onPause")
    }

    override fun onStop() {
        super.onStop()
        println("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        println("onRestart")
    }

    @Override
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso de ubicación concedido, puedes realizar las operaciones relacionadas con la ubicación aquí
            } else {
                // Permiso de ubicación denegado, muestra un mensaje al usuario o toma alguna otra acción
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
            val weather : WeatherData = weatherAPICall(lat,long)
            updateUI(weather)
        }
    }

    private fun initViews(){
        weatherImage = findViewById(R.id.weatherImage)
        wind = findViewById(R.id.ventoValue)
        temperatura = findViewById(R.id.temperatureValue)
        precipitacao = findViewById(R.id.precipitationVal)
        humidade = findViewById(R.id.humidadeVal)
        cidade = findViewById(R.id.cidade)
    }

    private fun updateUI(request: WeatherData)
    {
        runOnUiThread {

            initViews()

            wind?.text = getString(R.string.vento_vel_val, request.current.wind_speed_10m.toInt())
            temperatura?.text = getString(R.string.temperatura_valor, request.current.temperature_2m.toInt())
            precipitacao?.text = getString(R.string.precipitacao_val, request.current.precipitation.toInt())
            humidade?.text = getString(R.string.humidade_val, request.current.relative_humidity_2m)
            cidade?.text = city

            day = request.current.is_day == 1

            setTheme(R.style.Theme_Night)
            val mapt = getWeatherCodeMap()
            val wCode = mapt.get(1)//request.current.weather_code)
            val wImage = when(wCode) {
                WMO_WeatherCode.CLEAR_SKY,
                WMO_WeatherCode.MAINLY_CLEAR,
                WMO_WeatherCode.PARTLY_CLOUDY->if(day) wCode.image+"day" else wCode.image+"night"
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
            weatherImage?.setImageDrawable(drawable)

        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("temperatura", temperatura?.text.toString())
        outState.putString("wind", wind?.text.toString())
        outState.putString("precipitacao", precipitacao?.text.toString())
        outState.putString("humidade", humidade?.text.toString())
        outState.putString("cidade", cidade?.text.toString())
        outState.putBoolean("day", day)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        initViews()
        temperatura?.text = savedInstanceState.getString("temperatura")
        wind?.text = savedInstanceState.getString("wind")
        precipitacao?.text = savedInstanceState.getString("precipitacao")
        humidade?.text = savedInstanceState.getString("humidade")
        cidade?.text = savedInstanceState.getString("cidade")
        day = savedInstanceState.getBoolean("day")

    }

}