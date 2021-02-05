package com.sunnyweather.android.logic.dao

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.model.Place

/**
 * 1) : savePlace() : 用于将 Place 对象存储到 SharedPreferences 文件中,
 *      这里使用 GSON 将 place对象转存一个 JSON 字符串,然后就可以用字符串存储的方式来保存数据了
 * 2) : getSavedPlace() : 将 JSON 字符串从 SharedPreferences 文件中读取出来,然后再通过 GSON 将 JSON 字符串解析成 Place 对象返回
 * 3) : isPlaceSaved() : 判断是否有数据已被存储
 */
object PlaceDao {
    fun savePlace(place: Place) {
        sharedPreferences().edit {
            putString("place", Gson().toJson(place))
        }
    }

    fun getSavedPlace(): Place {
        val placeJson = sharedPreferences().getString("place", "")
        return Gson().fromJson(placeJson, Place::class.java)
    }

    fun isPlaceSaved() = sharedPreferences().contains("place")

    private fun sharedPreferences() =
        SunnyWeatherApplication.context.getSharedPreferences("sunny_weather", Context.MODE_PRIVATE)
}