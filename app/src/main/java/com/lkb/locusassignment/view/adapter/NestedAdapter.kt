package com.lkb.locusassignment.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.lkb.locusassignment.data.PageItem
import com.lkb.locusassignment.R
import com.lkb.locusassignment.databinding.CommentsLayoutBinding
import com.lkb.locusassignment.databinding.PhotoPickerBinding
import com.lkb.locusassignment.databinding.SingleChoiceLayoutBinding


class NestedAdapter(items: List<PageItem>, private val itemClickListener: (PageItem, Int) -> Unit) :
    BaseAdapter<RecyclerView.ViewHolder, PageItem>() {
    lateinit var commentsBinding: CommentsLayoutBinding
    lateinit var singleChoiceBinding: SingleChoiceLayoutBinding
    lateinit var photoBinding: PhotoPickerBinding
    private var mutableList: MutableList<PageItem> = items.toMutableList()
    override fun addItems(items: List<PageItem>, clear: Boolean) {
        if (clear) {
            mutableList.clear()
        }
        mutableList.addAll(items.toMutableList())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        commentsBinding = CommentsLayoutBinding.inflate(inflater, parent, false)
        photoBinding = PhotoPickerBinding.inflate(inflater, parent, false)
        singleChoiceBinding = SingleChoiceLayoutBinding.inflate(inflater, parent, false)
        return when (viewType) {
            1 -> {
                PhotoViewHolder(photoBinding, itemClickListener)
            }
            2 -> {
                SingleChoiceViewHolder(singleChoiceBinding, itemClickListener)
            }
            else -> {
                CommentsViewHolder(commentsBinding, itemClickListener)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (mutableList[position].type) {
            "PHOTO" -> {
                1
            }
            "SINGLE_CHOICE" -> {
                2
            }
            else -> {
                3
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ItemViewHolder<PageItem>).run {
            bind(mutableList[position], position)
        }
    }

    override fun getItemCount(): Int = mutableList.size

    inner class CommentsViewHolder(
        private val binding: CommentsLayoutBinding,
        private val itemClick: (PageItem, position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root), ItemViewHolder<PageItem> {
        override fun bind(item: PageItem, position: Int) {
            with(item) {
                binding.etComments.addTextChangedListener {
                    item.comment = it.toString()
                }
                binding.tvTitleLabel.text = title
                binding.toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        binding.etComments.visibility = View.VISIBLE
                    } else {
                        binding.etComments.visibility = View.GONE
                    }
                }

            }
        }
    }

    inner class SingleChoiceViewHolder(
        private val binding: SingleChoiceLayoutBinding,
        private val itemClick: (PageItem, position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root), ItemViewHolder<PageItem> {
        override fun bind(item: PageItem, position: Int) {
            binding.tvTitle.text = item.title
            binding.radioGroup.orientation = LinearLayout.VERTICAL
            binding.radioGroup.removeAllViews()
            val selectedItem = item?.dataMap?.selectedItemIndex
            item?.dataMap?.options?.let { item ->
                for (i in 0 until item.size) {
                    val radioBtn = RadioButton(binding.root.context)
                    radioBtn.id = i
                    radioBtn.text = item[i]
                    if (selectedItem == i) {
                        radioBtn.isChecked = true
                    }
                    binding.radioGroup.addView(radioBtn)
                }
            }
            binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
                item?.dataMap?.selectedItemIndex = checkedId
            }
        }
    }

    inner class PhotoViewHolder(
        private val binding: PhotoPickerBinding,
        private val itemClick: (PageItem, position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root), ItemViewHolder<PageItem> {
        override fun bind(item: PageItem, position: Int) {
            binding.tvTitleImage.text = item.title
            binding.imageView.setImageResource(R.drawable.ic_image)
            binding.ivClear.visibility = View.GONE
            item.bitmap?.let {
                binding.imageView.setImageBitmap(it)
                binding.ivClear.visibility = View.VISIBLE
            }
            binding.imageView.setOnClickListener {
                if (item.bitmap == null) {
                    itemClick.invoke(item, position)
                }
            }
            binding.ivClear.setOnClickListener {
                item.bitmap = null
                notifyDataSetChanged()
            }
        }

    }

    interface ItemViewHolder<D> {
        fun bind(item: D, position: Int)
    }
}