package com.bang.bangtalk.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.R
import com.bang.bangtalk.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditProfileActivityRecyclerViewAdapter(mContext : Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    val arrayItem = mContext.resources.getStringArray(R.array.edit_profile)
    var profile : ArrayList<String> = ArrayList()

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tv_item : TextView = view.findViewById(R.id.tv_item)
        var tv_value : TextView = view.findViewById(R.id.tv_value)
        var pos : Int? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_edit_profile, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayItem.size
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder : MyViewHolder = viewHolder as MyViewHolder
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        holder.tv_item.text = arrayItem[position]
        holder.pos = position

        FirebaseDatabase.getInstance().reference.child("users").child(uid).addValueEventListener(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val model : UserModel = dataSnapshot.getValue(UserModel::class.java)!!
                val arrayList : ArrayList<String> = ArrayList()
                arrayList.add(model.name.toString())
                arrayList.add(model.comment.toString())

                if(arrayList[position] != "null")
                    holder.tv_value.text = arrayList[position]
            }
        })

        holder.itemView.setOnClickListener{
            val edit : EditText = EditText(it.context)
            val container : FrameLayout = FrameLayout(it.context)
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = it.resources.getDimensionPixelSize(R.dimen.dialog_margin)
            params.rightMargin = it.resources.getDimensionPixelSize(R.dimen.dialog_margin)
            edit.layoutParams = params
            edit.setText(holder.tv_value.text)
            container.addView(edit)

            val builder = AlertDialog.Builder(it.context)
                .setTitle(holder.tv_item.text.toString())
                .setView(container)
                .setPositiveButton("확인"){ _: DialogInterface, _: Int ->
                    val map : HashMap<String, Any> = hashMapOf()

                    when(holder.pos){
                        0 -> {
                            if(edit.text.isNotEmpty()) {
                                map["name"] = edit.text.toString()
                                FirebaseDatabase.getInstance().reference.child("users").child(uid).updateChildren(map)
                                val profileUpdate = UserProfileChangeRequest.Builder().setDisplayName(edit.text.toString()).build()
                                FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdate)
                            } else {
                                Toast.makeText(it.context, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        1 -> {
                            map["comment"] = edit.text.toString()
                            FirebaseDatabase.getInstance().reference.child("users").child(uid).updateChildren(map)
                        }
                    }
                }
                .setNegativeButton("취소"){dialogInterface: DialogInterface, i: Int ->

                }
            builder.show()
        }
    }
}