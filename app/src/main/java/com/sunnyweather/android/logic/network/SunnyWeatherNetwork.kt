package com.sunnyweather.android.logic.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 1) : 使用 ServiceCreator 创建一个 PlaceService 接口的动态代理对象
 * 2) : 定义一个 searchPlaces() 函数,在这里调用刚才在 PlaceService 接口中定义的 searchPlaces() 方法
 * 3) : 为了简化 Retrofit 的回调写法,借助协程实现
 *      定义一个 await() 函数,将 searchPlaces() 也声明成挂起函数
 * 4) : 当外部调用 SunnyWeatherNetwork 的 searchPlaces() 函数时,Retrofit 就会立即发送网路请求,
 *      同时当前的协程也会被阻塞住,知道服务器响应我们的请求之后,await()函数会将解析出来的数据模型对象取出并返回
 *      同时恢复当前协程的执行,searchPlaces()函数在得到 await() 函数的返回值后会将该数据再返回到上一层
 */
object SunnyWeatherNetwork {

    private val placeService = ServiceCreator.create<PlaceService>()

    suspend fun searchPlaces(query: String) = placeService.searchPlaces(query).await()

    private suspend fun <T> Call<T>.await(): T {
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    val body = response.body()
                    if (body != null) continuation.resume(body)
                    else continuation.resumeWithException(RuntimeException("response body is null"))
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }
}