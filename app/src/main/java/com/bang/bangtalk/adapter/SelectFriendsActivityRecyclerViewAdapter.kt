package com.bang.bangtalk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.Interface.ItemClickListener
import com.bang.bangtalk.R
import com.bang.bangtalk.SelectFriendsActivity
import com.bang.bangtalk.model.UserModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.ArrayList

class SelectFriendsActivityRecyclerViewAdapter(friends: ArrayList<String>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var userModels: ArrayList<UserModel> = ArrayList()
    val uid = FirebaseAuth.getInstance().currentUser!!.uid

    var mListener : ItemClickListener? = null

    fun setOnItemClickListener(listener : ItemClickListener){
        mListener = listener
    }

    init {
        FirebaseDatabase.getInstance().reference.child("users").orderByChild("/name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userModels.clear()

                if(friends == null){
                    for(item in dataSnapshot.children){
                        if(uid != item.key.toString()){
                            val model: UserModel = item.getValue<UserModel>(UserModel::class.java)!!
                            userModels.add(model)
                        }
                    }
                    notifyDataSetChanged()

                } else {
                    for(item in dataSnapshot.children){
                        if(!friends.contains(item.key)){
                            val model: UserModel = item.getValue<UserModel>(UserModel::class.java)!!
                            userModels.add(model)
                        }
                    }
                    notifyDataSetChanged()
                }

            }
        })
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tv_name : TextView = view.findViewById(R.id.tv_friendName)
        var iv_image : ImageView = view.findViewById(R.id.iv_friendProfile)
        var checkbox : CheckBox = view.findViewById(R.id.cb_select_friends)
        val mView : View = view
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_select, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder : MyViewHolder = viewHolder as MyViewHolder

        //친구 이름
        holder.tv_name.text = userModels[position].name.toString()

        //친구 사진
        if(!userModels[position].profileImageUrl.isNullOrEmpty()){
            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(userModels[position].profileImageUrl!!)

            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(holder.mView.context).load(uri).apply(RequestOptions.circleCropTransform()).into(holder.iv_image)
            }.addOnFailureListener{
                holder.iv_image.setImageResource(R.drawable.ic_account_circle)
            }
        }else{
            holder.iv_image.setImageResource(R.drawable.ic_account_circle)
        }

        //스크롤해도 체크값이 초기화되지 않게 하기 위함
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.setOnCheckedChangeListener { buttonView, _ ->
            if(mListener != null){
                mListener!!.onItemClick(buttonView, position, holder.checkbox.isChecked)
            }
        }

        holder.mView.setOnClickListener{
            when(holder.checkbox.isChecked){
                true -> {
                    holder.checkbox.isChecked = false
                }
                false -> {
                    holder.checkbox.isChecked = true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return userModels.size
    }
}