package com.example.gallery

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import android.widget.ImageView
import kotlinx.android.synthetic.main.item.view.*
import java.io.File
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

class Adapter(var data: ArrayList<File>, var context: Context):RecyclerView.Adapter<Adapter.ViewHolder>(){

    private var selectedItemsIds = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var options = BitmapFactory.Options()
        options.inSampleSize = 5
        holder.img.setImageBitmap(BitmapFactory.decodeFile(data[position].absolutePath, options))
        holder.selectedImg.setBackgroundColor(if (selectedItemsIds.get(position)) -0x66cb4a1c else Color.TRANSPARENT)
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view) {
        var img: ImageView = view.imageView
        var selectedImg: ImageView = view.selectedImg
    }

    fun toggleSelection(position: Int) {
        selectView(position, !selectedItemsIds.get(position))
    }

    fun removeSelection() {
        selectedItemsIds = SparseBooleanArray()
        notifyDataSetChanged()
    }

    fun selectView(position: Int, value: Boolean) {
        if (value)
            selectedItemsIds.put(position, value)
        else
            selectedItemsIds.delete(position)

        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int {
        return selectedItemsIds.size()
    }

    fun getSelectedIds(): SparseBooleanArray {
        return selectedItemsIds
    }

}