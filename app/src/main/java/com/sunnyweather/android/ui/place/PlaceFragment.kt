package com.sunnyweather.android.ui.place

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunnyweather.android.MainActivity
import com.sunnyweather.android.R
import com.sunnyweather.android.ui.weather.WeatherActivity
import kotlinx.android.synthetic.main.fragment_place.*

/**
 * 1) : 使用 lazy 函数这种懒加载技术来获取 PlaceViewModel 的实例,这是一种非常棒的写法,允许我们在整个类中随时使用 viewModel 这个变量
 *      而不用关系它何时初始化、是否为空等前提条件
 * 2) : onCreateView() : 加载 fragment_place 布局
 * 3) : onActivityCreated() :
 *          先是给 RecyclerView 设置 LayoutManager 和适配器,使用 PlaceViewModel 中的 placeList 作为数据源
 *          接着调用了 EditText 的 addTextChangedListener() 方法来监听搜索框内容的变化情况,当搜索框的内容发生变化的时候获取新的内容
 *        然后传递给 PlaceViewModel 的 searchPlaces() 方法,这样就可以发起搜索城市数据的网络请求,当输入框内容为空的时候,
 *        将 RecyclerView 隐藏起来,同时将那张用于美观的背景图显示出来
 *          借助 LiveData 获取服务器响应的数据,对 PlaceViewModel 中的 placeLiveData 对象进行观察,就会回调到 Observer 接口实现中
 *        然后对回调数据进行判断 : 如果数据不为空,那么就将这些数据添加到 PlaceViewModel 的 placeList 集合中,并通知 PlaceAdapter 刷新界面
 *        如果数据为空,则说明发生了异常,此时弹出一个 Toast 提示,并且打印具体的异常原因
 *
 */
class PlaceFragment : Fragment() {

    val viewModel by lazy {
        ViewModelProviders.of(this).get(PlaceViewModel::class.java)
    }

    private lateinit var adapter: PlaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_place, container, false)
    }

    /**
     * 如果已经存储了城市数据,那么直接获取已存储的数据并解析成 Place 对象,
     * 然后使用它的经纬度坐标和城市名直接跳转并传递给 WeatherActivity,这样用户就不需要每次都重新搜索并选择城市了
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        /**
         * 只有当 PlaceFragment 被嵌入 MainActivity,并且之前已经存在选择的城市,才会直接跳转到 WeatherActivity
         */
        if (activity is MainActivity && viewModel.isPlaceSaved()) {
            val place = viewModel.getSavedPlace()
            val intent = Intent(context, WeatherActivity::class.java).apply {
                putExtra("location_lng", place.location.lng)
                putExtra("location_lat", place.location.lat)
                putExtra("place_name", place.name)
            }
            startActivity(intent)
            activity?.finish()
            return
        }
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        adapter = PlaceAdapter(this, viewModel.placeList)
        recyclerView.adapter = adapter
        searchPlaceEdit.addTextChangedListener { editable ->
            val content = editable.toString()
            if (content.isNotEmpty()) {
                viewModel.searchPlaces(content)
            } else {
                recyclerView.visibility = View.GONE
                bgImageView.visibility = View.VISIBLE
                viewModel.placeList.clear()
                adapter.notifyDataSetChanged()
            }
        }
        viewModel.placeLiveData.observe(this, Observer { result ->
            val places = result.getOrNull()
            if (places != null) {
                recyclerView.visibility = View.VISIBLE
                bgImageView.visibility = View.GONE
                viewModel.placeList.clear()
                viewModel.placeList.addAll(places)
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(activity, "未能查询到任何地点", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })
    }
}