package com.lkb.locusassignment

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName

data class PageItem(
    @SerializedName("type") var type: String? = null,
    @SerializedName("id") var id: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("dataMap") var dataMap: DataMap? = DataMap(),
    var bitmap: Bitmap? = null
)


data class DataMap(
    @SerializedName("options") var options: ArrayList<String> = arrayListOf(),
    var selectedItemIndex: Int = -1
)