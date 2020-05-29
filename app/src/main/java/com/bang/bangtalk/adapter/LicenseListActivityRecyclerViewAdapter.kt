package com.bang.bangtalk.adapter

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.R
import com.bang.bangtalk.model.LicenseModel

class LicenseListActivityRecyclerViewAdapter(mContext : Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var licenseModels : ArrayList<LicenseModel> = ArrayList()
    val arrayLicense = mContext.resources.getStringArray(R.array.copyrignt)
    val arrayCopyright = mContext.resources.getStringArray(R.array.copyright_detail)

    init {
        for(i in arrayLicense.indices){
            licenseModels.add(LicenseModel(arrayLicense[i], arrayCopyright[i]))
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tv_license : TextView = view.findViewById(R.id.tv_license)
        var tv_copyright : TextView = view.findViewById(R.id.tv_copyright)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_license, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return licenseModels.size
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder : MyViewHolder = viewHolder as MyViewHolder

        holder.tv_license.text = licenseModels[position].licenseName
        holder.tv_copyright.text = licenseModels[position].detail
    }
}