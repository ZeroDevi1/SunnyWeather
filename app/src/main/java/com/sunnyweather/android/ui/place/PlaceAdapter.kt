package com.sunnyweather.android.ui.place

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunnyweather.android.R
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.ui.weather.WeatherActivity
import kotlinx.android.synthetic.main.activity_weather.*

/**
 * 1) : 将传入的 Fragment 修改成 PlaceFragment,就可以调用 PlaceFragment 对应的 PlaceViewModel
 * 2) : 在 onCreateViewHolder() 方法中,点击子项布局调整到 WeatherActivity 之前,存储选中的城市
 */
class PlaceAdapter(private val fragment: PlaceFragment, private val placeList: List<Place>) :
    RecyclerView.Adapter<PlaceAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.placeName)
        val placeAddress: TextView = itemView.findViewById(R.id.placeAddress)
    }

    /**
     * 在 place_item.xml 最外层注册了一个点击事件监听器
     * 在点击事件监听器获取当前点击项的经纬度和地区名称,并把它们传入 Intent 中
     * 最后调用 Fragment 的 startActivity() 方法启动 WeatherActivity
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.place_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            val place = placeList[position]

            /**
             * 如果当前 PlaceFragment 处在 WeatherActivity,直接关闭滑动菜单,给 WeatherViewModel 赋值新的经纬度坐标和地区名称
             * 然后刷新城市的天气信息
             * 如果实在 MainActivity,保持之前的处理逻辑即可
             */
            val activity = fragment.activity
            if (activity is WeatherActivity) {
                activity.drawerLayout.closeDrawers()
                activity.viewModel.locationLng = place.location.lng
                activity.viewModel.locationLat = place.location.lat
                activity.viewModel.placeName = place.name
                activity.refreshWeather()
            } else {
                val intent = Intent(parent.context, WeatherActivity::class.java).apply {
                    putExtra("location_lng", place.location.lng)
                    putExtra("location_lat", place.location.lat)
                    putExtra("place_name", place.name)
                }
                fragment.startActivity(intent)
                activity?.finish()
            }
            fragment.viewModel.savePlace(place)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = placeList[position]
        holder.placeName.text = place.name
        holder.placeAddress.text = place.address
    }

    override fun getItemCount() = placeList.size
}