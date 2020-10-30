package com.sunnyweather.android.ui.place

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.sunnyweather.android.logic.Repository
import com.sunnyweather.android.logic.model.Place

/**
 * 1) : 首先 PlaceViewModel 中定义一个 searchPlaces() 方法,但是这里并没有直接调用仓库层中的 searchPlaces() 方法
 *      而是将传入的搜索参数赋值给了一个 searchLiveData 对象,并使用 Transformations的 switchMap() 方法来观察这个对象
 *      否则仓库返回的 LiveData 对象将无法进行观察
 * 2) : 现在每当 searchPlaces() 被调用的时候,switchMap() 方法所对应的转换函数就会执行
 *      然后在转换函数汇总,只需要调用仓库层中定义的 searchPlaces() 方法就可以发起网络请求
 *      同时将仓库返回的 LiveData 对象转换成一个可供 Activity 观察的 LiveData 对象
 * 3) : 同时,在 PlaceViewModel 中定义一个 placeList 集合,用于对界面上显示的城市数据进行缓存
 *      因为原则上与界面相关的数据都应该放到 ViewModel 中,这样可以保证它们在手机屏幕发生旋转的时候不会丢失
 */
class PlaceViewModel : ViewModel() {

    private val searchLiveData = MutableLiveData<String>()

    val placeList = ArrayList<Place>()

    val placeLiveData = Transformations.switchMap(searchLiveData) { query ->
        Repository.searchPlaces(query)
    }

    fun searchPlaces(query: String) {
        searchLiveData.value = query
    }

    fun savePlace(place: Place) = Repository.savePlace(place)

    fun getSavedPlace() = Repository.getSavedPlace()

    fun isPlaceSaved() = Repository.isPlaceSaved()
}