package com.bang.bangtalk.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.bang.bangtalk.R
import com.bang.bangtalk.model.SettingModel

class SettingsFragmentListViewAdapter(mContext: Context?) : BaseAdapter() {

    var settings : ArrayList<SettingModel> = ArrayList()
    val menuArray = mContext!!.resources.getStringArray(R.array.settings_menu)
    val detailArray = mContext!!.resources.getStringArray(R.array.settings_detail)

    init{
        for(i in menuArray.indices){
            settings.add(SettingModel(menuArray[i], detailArray[i]))
        }
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(parent!!.context).inflate(R.layout.item_settings, parent, false)

        val tv_menu : TextView = view.findViewById(R.id.tv_menu)
        val tv_detail : TextView  = view.findViewById(R.id.tv_menu_detail)

        tv_menu.text = settings[position].menu
        tv_detail.text = settings[position].detail

        when(settings[position].detail.isNullOrEmpty()){
            true -> tv_detail.visibility = View.GONE
            false -> tv_detail.visibility = View.VISIBLE
        }

        return view
    }

    override fun getItem(position: Int): Any {
        return settings[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return settings.size
    }

}