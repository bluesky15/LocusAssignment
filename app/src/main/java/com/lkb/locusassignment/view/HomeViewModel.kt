package com.lkb.locusassignment.view

import android.util.Log
import androidx.lifecycle.ViewModel
import com.lkb.locusassignment.data.PageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    fun logData(pageData: List<PageItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            pageData.forEach { item ->
                Log.d(
                    TAG,
                    "View Id - ${item.id}, user comments - ${item.comment}, Single Selection Item - ${
                        if (item.dataMap?.selectedItemIndex!! > -1) {
                            item.dataMap!!.options[item.dataMap?.selectedItemIndex!!]
                        } else {
                            "not selected"
                        }
                    } "
                )
            }
        }
    }

    companion object {
        const val TAG = "LOCUS"
    }


}