package com.lkb.locusassignment

import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter <T : RecyclerView.ViewHolder, D> : RecyclerView.Adapter<T>(){
    abstract fun addItems(items: List<D>, clear: Boolean = false)
}