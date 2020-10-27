package com.sunnyweather.android.logic

import androidx.lifecycle.liveData
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers

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
    fun searchPlaces(query: String) = liveData(Dispatchers.IO) {
        val result = try {
            val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
            if (placeResponse.status == "ok") {
                val places = placeResponse.places
                Result.success(places)
            } else {
                Result.failure(RuntimeException("response status is ${placeResponse.status}"))
            }
        } catch (e: Exception) {
            Result.failure<List<Place>>(e)
        }
        emit(result)
    }
}