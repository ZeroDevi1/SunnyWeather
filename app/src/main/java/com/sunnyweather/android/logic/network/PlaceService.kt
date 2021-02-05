package com.sunnyweather.android.logic.network

import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.model.PlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaceService {
    /**
     * 1) : @GET 注解表示发送一条 GET 请求
     * 2) : 因为搜索城市数据API只有 query 这个参数是需要动态指定的,使用 @Query 注解的方式来实现
     *      另外两个参数不会变,所以固定写在 @GET 注解中即可
     * 3) : 返回值声明成 Call<PlaceResponse>,这样 Retrofit 就会将服务器返回的 JSON 数据自动解析成 PlaceResponse 对象
     */
    @GET("v2/place?token=${SunnyWeatherApplication.TOKEN}%lang=zh_CN")
    fun searchPlaces(@Query("query") query: String): Call<PlaceResponse>
}