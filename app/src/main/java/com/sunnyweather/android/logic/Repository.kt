package com.sunnyweather.android.logic

import androidx.lifecycle.liveData
import com.sunnyweather.android.logic.dao.PlaceDao
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext

object Repository {
    /**
     * 1) : 为了一部获取的数据以响应式编程的方式通知给上一层,通常会返回一个 LiveData 对象
     *      这里使用 lifecycle-livedata-ktx 库提供的一个强大的功能,可以自动构建并返回 LiveData 对象
     *      然后在它的代码块中提供一个挂起函数的上下文,这样就可以在 liveData() 函数的代码块中调用任意的挂起函数
     * 2) : 这里调用 SunnyWeatherNetwork.searchPlaces(query) 函数来搜索城市数据
     *      然后判断服务器的响应状态,如果是 ok,name使用 Kotlin 内置的 Result.success() 来包装获取的城市数据列表
     *      否则使用 Result.failure() 方法来包装一个异常信息
     * 3) : 最后使用一个 emit() 方法将包装的结果发射出去,这个 emit() 方法类似于调用 LiveData 的 setValue() 方法通知数据变化
     *      只不过这里我们无法直接获取返回的 LiveData 对象,所以 lifecycle-livedata-ktx 提供了这样一个替代方法
     * 4) : liveData() 函数的线程参数类型指定成 Dispatchers.IO,这样代码块中的所有代码都运行在子线程中了
     *      因为 Android 不允许在主线程中进行网络请求的,读写数据库之类的本地数据操作也是不建议在主线程中进行
     *      所以有必要在仓库层进行一次线程转换
     */
    fun searchPlaces(query: String) = fire(Dispatchers.IO) {
        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if (placeResponse.status == "ok") {
            val places = placeResponse.places
            Result.success(places)
        } else {
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }

    /**
     * 1) : 这里我们使用一个方法来刷新天气信息
     * 2) : 获取实时天气信息和未来天气信息没有先后顺序,并行执行可以提升程序的运行效率
     *      但是如果要在同时得到它们的响应结果只会才能进一步执行程序,可以使用 async {} 函数进行网络调用
     *      然后分别调用它们的 await() 方法,皆可以保证只有在这两个网络请求都成功响应之后,才会进一步执行程序
     *      因为 async 函数必须在 协程作用域内才能调用,所以这里使用 coroutineScope 创建了一个协程作用域
     */
    fun refreshWeather(lng: String, lat: String) = fire(Dispatchers.IO) {
        coroutineScope {
            val deferredRealtime = async {
                SunnyWeatherNetwork.getRealtimeWeather(lng, lat)
            }
            val deferredDaily = async {
                SunnyWeatherNetwork.getDailyWeather(lng, lat)
            }
            val realtimeResponse = deferredRealtime.await()
            val dailyResponse = deferredDaily.await()
            if (realtimeResponse.status == "ok" && dailyResponse.status == "ok") {
                val weather =
                    Weather(realtimeResponse.result.realtime, dailyResponse.result.daily)
                Result.success(weather)
            } else {
                Result.failure(RuntimeException("realtime response status is ${realtimeResponse.status} daily response status is ${dailyResponse.status}"))
            }
        }
    }

    /**
     * 这是一个按照 liveData() 函数的参数接收标准定义的一个高阶函数
     * 在 fire() 函数的内部会先调用一下 liveData() 函数,然后在 liveData() 函数的代码块中统一进行了 try catch 处理
     * 在 try 语句中调用传入的 Lambda 表达式中的代码,最终获取 Lambda 表达式的执行结果并调用 emit() 方法发射出去
     *
     * 因为 liveData() 函数的代码块中,是由挂起函数的上下文的,但是当回调到 Lambda 表达式,代码就没有挂起函数上下文了
     * 虽然实际上 Lambda 表达式中的代码也一定是在挂起函数中运行的,所以需要在函数类型前声明一个 suspend 关键字
     * 以表示所有传入的 Lambda 表达式中的代码也是拥有挂起函数的上下文的
     */
    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }

    /**
     * 虽然 SharedPreferences 执行速度很快,但是通常建议开启一个线程执行,然后通过 LiveData 对象进行数据返回
     */
    fun savePlace(place: Place) = PlaceDao.savePlace(place)

    fun getSavedPlace() = PlaceDao.getSavedPlace()

    fun isPlaceSaved() = PlaceDao.isPlaceSaved()

}