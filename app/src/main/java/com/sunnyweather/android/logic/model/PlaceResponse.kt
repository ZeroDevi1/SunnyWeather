package com.sunnyweather.android.logic.model

import com.google.gson.annotations.SerializedName

/**
 * 1) : PlaceResponse.kt 中定义的类与属性,完全参照 搜索城市数据接口返回的 JSON 格式来定义的
 *      由于JSON中的一些字段的命名可能与 Kotlin 的命名规范不太一致,所以使用 @SerializedName 让 JSON 字段和 Kotlin 字段建立映射关系
 */
data class PlaceResponse(val status: String, val places: List<Place>)

data class Place(
    val name: String,
    val location: Location,
    @SerializedName("formatted_address") val address: String
)

data class Location(val lng: String, val lat: String)

