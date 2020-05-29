package com.bang.bangtalk.Interface

import android.view.View
import android.widget.CheckBox

interface ItemClickListener {
    fun onItemClick(v : View, position : Int, isChecked: Boolean)
}