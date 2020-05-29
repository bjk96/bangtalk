package com.bang.bangtalk.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bang.bangtalk.R
import com.bang.bangtalk.model.ChatModel
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
import java.util.*
import kotlin.collections.ArrayList

class NavigationDrawerListViewAdapter(friendUid: String?, roomUid: String?) : BaseAdapter() {

    var userModels: ArrayList<UserModel> = ArrayList()
    val uid = FirebaseAuth.getInstance().currentUser!!.uid

    init {
        FirebaseDatabase.getInstance().reference.child("users").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userModels.clear()
                userModels.add(dataSnapshot.getValue(UserModel::class.java)!!)

                if(friendUid != null && roomUid == null){
                    FirebaseDatabase.getInstance().reference.child("users").child(friendUid).addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(databaseError2: DatabaseError) {

                        }

                        override fun onDataChange(dataSnapshot2: DataSnapshot) {
                            userModels.add(dataSnapshot2.getValue(UserModel::class.java)!!)
                            notifyDataSetChanged()
                        }
                    })
                } else if (friendUid == null && roomUid != null){
                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomUid).child("users").addValueEventListener(object : ValueEventListener{
                        override fun onCancelled(databaseError: DatabaseError) {

                        }

                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for(item in dataSnapshot.children){
                                if(item.child("status").getValue(Int::class.java) != 2){
                                    FirebaseDatabase.getInstance().reference.child("users").orderByChild("/name").addValueEventListener(object : ValueEventListener{
                                        override fun onCancelled(p0: DatabaseError) {

                                        }

                                        override fun onDataChange(dataSnapshot2: DataSnapshot) {
                                            for(item2 in dataSnapshot2.children){
                                                if(item.key == item2.key && item.key != uid) {
                                                    val model : UserModel = item2.getValue(UserModel::class.java)!!
                                                    userModels.add(model)
                                                    notifyDataSetChanged()
                                                    userModels.drop(1).sorted()
                                                }
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    })
                } else {
                    notifyDataSetChanged()
                }
            }
        })

    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(parent!!.context).inflate(R.layout.item_nav_participants, parent, false)

        val iv_profile : ImageView = view.findViewById(R.id.iv_profile)
        val tv_name : TextView = view.findViewById(R.id.tv_name)

        tv_name.text = userModels[position].name

        if(userModels[position].profileImageUrl.isNullOrEmpty()){
            iv_profile.setImageResource(R.drawable.ic_account_circle)
        } else {
            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(userModels[position].profileImageUrl!!)
            val activity = parent.context as Activity

            storageReference.downloadUrl.addOnSuccessListener { uri ->
                if(!activity.isFinishing)
                    Glide.with(iv_profile.context).load(uri).apply(RequestOptions.circleCropTransform()).into(iv_profile)
            }.addOnFailureListener{
                iv_profile.setImageResource(R.drawable.ic_account_circle)
            }
        }

        if(userModels[position].uid == uid) {
            tv_name.setTypeface(null, Typeface.BOLD)
        }

        return view
    }

    override fun getItem(position: Int): Any {
        return userModels[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return userModels.size
    }
}