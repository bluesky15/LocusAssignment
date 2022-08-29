package com.lkb.locusassignment.data

import android.graphics.Bitmap

data class PageItem(
    var type: String? = null,
    var id: String? = null,
    var title: String? = null,
    var dataMap: DataMap? = DataMap(),
    var bitmap: Bitmap? = null,
    var comment: String? = null
)


data class DataMap(
    var options: ArrayList<String> = arrayListOf(),
    var selectedItemIndex: Int = -1
)