package com.sunnyweather.android.ui.weather

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sunnyweather.android.R
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.life_index.*
import kotlinx.android.synthetic.main.now.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 1) : onCreate() 方法中,首先从 Intent 中取出经纬度坐标和地区名称,并赋值到 WeatherViewModel 相关变量中
 *      然后对 weatherLiveData 对象进行观察,当获取到服务器返回的天气数据时,就调用 showWeatherInfo() 方法进行解析与展示
 *      最后调用了 WeatherViewModel 的 refreshWeather() 方法来执行一次刷新天气的请求
 * 2) : showWeatherInfo(),首先从 Weather 对象中获取数据
 *      然后显示到对应的控件上
 *      在未来几天天气预报部分,使用了一个 for-in 循环来处理每天的天气信息,在循环中动态加载 forecast_item.xml 并设置相应的数据
 *      最后添加到父布局中
 *      生活指数虽然服务器会返回很多天的数据,但是界面上只需要显示当天的数据就可以了
 *      最后让 ScrollView 变成可见状态
 */
class WeatherActivity : AppCompatActivity() {

    val viewModel by lazy {
        ViewModelProviders.of(this).get(WeatherViewModel::class.java)
    }

    /**
     * 1) : 使用 getWindow().getDecorView() 方法拿到当前 Activity 的 DecorView,
     * 2) : 再调用它的 setSystemUiVisibility() 方法来改变UI的显示,
     *      这里使用 View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 和  View.SYSTEM_UI_FLAG_LAYOUT_STABLE 表示 Activity 的布局会显示在状态栏上面
     * 3) : 最后调用一下 setStatusBarColor() 方法将状态栏设置成透明色
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val decorView = window.decorView
        decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_weather)
        if (viewModel.locationLng.isEmpty()) {
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        if (viewModel.locationLat.isEmpty()) {
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        if (viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }
        viewModel.weatherLiveData.observe(this, Observer { result ->
            val weather = result.getOrNull()
            if (weather != null) {
                showWeather(weather)
            } else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            /**
             * 请求结束之后,将 SwipeRefreshLayout 的 isRefreshing 设置成 false,表示刷新事件结束,并隐藏刷新进度条
             */
            swipeRefresh.isRefreshing = false
        })
        /**
         * 1) : swipeRefresh.setColorSchemeResources(R.color.colorPrimary) 设置下拉刷新进度条的颜色
         * 2) : 使用 swipeRefresh.setOnRefreshListener {} 给 SwipeRefreshLayout 设置一个下拉刷新的监听器
         *      出发了下拉刷新操作的时候,就在监听器的回调中调用 refreshWeather() 方法来刷新天气信息
         */
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        refreshWeather()
        swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }
        /**
         * 1) : 点击切换城市按钮调用 DrawerLayout 的 openDrawer() 方法来打开滑动菜单
         * 2) : 监听 DrawerLayout 的状态,当滑动菜单被隐藏的时候,同时也要隐藏输入法
         *      避免因此滑动菜单后输入法还显示在界面上
         */
        navBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
            }

            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(
                    drawerView.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }

            override fun onDrawerStateChanged(newState: Int) {
            }

        })
    }

    /**
     * 将刷新天气信息的代码提取到 refreshWeather() 中
     * 调用 WeatherViewModel 的 refreshWeather() 方法,
     * 并将 SwipeRefreshLayout 的 isRefreshing 属性设置成 true,从而让下拉刷新进度条显示出来
     */
    fun refreshWeather() {
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        swipeRefresh.isRefreshing = true
    }

    private fun showWeather(weather: Weather) {
        placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily
        // 填充 now.xml 中的布局数据
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        currentTemp.text = currentTempText
        currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气数据 ${realtime.airQuality.aqi.chn.toInt()}"
        currentAQI.text = currentPM25Text
        nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)
        // 填充 forecast.xml 布局中的数据
        forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view =
                LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false)
            val dateInfo = view.findViewById(R.id.dateInfo) as TextView
            val skyIcon = view.findViewById(R.id.skyIcon) as ImageView
            val skyInfo = view.findViewById(R.id.skyInfo) as TextView
            val temperatureInfo = view.findViewById(R.id.temperatureInfo) as TextView
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInfo.text = simpleDateFormat.format(skycon.date)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ℃"
            temperatureInfo.text = tempText
            forecastLayout.addView(view)
        }
        // 填充 life_index.xml 布局中的数据
        val lifeIndex = daily.lifeIndex
        coldRiskText.text = lifeIndex.coldRisk[0].desc
        dressingText.text = lifeIndex.dressing[0].desc
        ultravioletText.text = lifeIndex.ultraviolet[0].desc
        carWashingText.text = lifeIndex.carWashing[0].desc
        weatherLayout.visibility = View.VISIBLE
    }
}