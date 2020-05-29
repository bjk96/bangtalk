package com.bang.bangtalk.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.ChatActivity
import com.bang.bangtalk.EditRoomNameActivity
import com.bang.bangtalk.GroupChatActivity
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatroomFragmentRecyclerViewAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var chatModels : ArrayList<ChatModel> = ArrayList()
    var keys : ArrayList<String> = ArrayList()
    var count : Int = 0

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var iv_roomImage : ImageView = view.findViewById(R.id.iv_roomImage)
        var tv_roomTitle : TextView = view.findViewById(R.id.tv_roomTitle)
        var tv_lastMessage : TextView = view.findViewById(R.id.tv_lastMessage)
        var tv_lastMessageTime : TextView = view.findViewById(R.id.tv_lastMessageTime)
        var tv_unreadMessageCount : TextView = view.findViewById(R.id.tv_unreadMessgaeCount)
        val mView : View = view

        var key : String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_chatroom, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return chatModels.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder : MyViewHolder = viewHolder as MyViewHolder
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        var friendUid : String? = null
        val friends : ArrayList<String> = ArrayList()

        //채팅방에 있는 유저 체크
        for(user in chatModels[position].users.keys){
            if(user != uid){
                friendUid = user
                friends.add(friendUid)
            }
        }

        //이미지 설정
        for(i in 0 until friends.size){
            if(chatModels[position].users[friends[i]]?.status != 2){
                FirebaseDatabase.getInstance().reference.child("users").child(friends[i]).addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(databaseError: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userModel : UserModel = dataSnapshot.getValue(UserModel::class.java)!!

                        if(userModel.profileImageUrl.isNullOrEmpty()){
                            holder.iv_roomImage.setImageResource(R.drawable.ic_account_circle)
                        } else{
                            val url = userModel.profileImageUrl.toString()
                            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                            val activity = holder.mView.context as Activity

                            storageReference.downloadUrl.addOnSuccessListener { uri ->
                                if(!activity.isFinishing)
                                    Glide.with(holder.itemView.context).load(uri).apply(RequestOptions.circleCropTransform()).into(holder.iv_roomImage)
                            }.addOnFailureListener{
                                holder.iv_roomImage.setImageResource(R.drawable.ic_account_circle)
                            }
                        }
                    }
                })
            }
        }

        val commentMap : TreeMap<String, ChatModel.Comment> = TreeMap(Collections.reverseOrder())
        commentMap.putAll(chatModels[position].comments)
        val lastMessageKey = commentMap.firstKey()
        val lastComment = chatModels[position].comments[lastMessageKey]
        holder.tv_lastMessage.text = lastComment?.message

        val now = Calendar.getInstance().time

        if(getYear(lastComment!!.send_time!!) == getYear(now) && getMonth(lastComment.send_time!!) == getMonth(now) && getDate(lastComment.send_time!!) == getDate(now)){
            holder.tv_lastMessageTime.text = SimpleDateFormat("a h:mm", Locale.getDefault()).format(lastComment.send_time!!.time)
        } else if(getYear(lastComment.send_time!!) == getYear(now)){
            holder.tv_lastMessageTime.text = SimpleDateFormat("M월 d일", Locale.getDefault()).format(lastComment.send_time!!.time)
        } else
            holder.tv_lastMessageTime.text = SimpleDateFormat("yyyy.M.d", Locale.getDefault()).format(lastComment.send_time!!.time)

        for(key in commentMap.keys){
            if(!commentMap[key]!!.readUsers.containsKey(uid)) {
                count++
            }
        }

        if(count > 0) {
            holder.tv_unreadMessageCount.visibility = View.VISIBLE
            holder.tv_unreadMessageCount.text = count.toString()
        } else{
            holder.tv_unreadMessageCount.visibility = View.INVISIBLE
        }

        count = 0

        holder.itemView.setOnClickListener { v->
            var intent: Intent? = null
            if(friends.size == 1){
                intent = Intent(v.context, ChatActivity::class.java)
                intent.putExtra("friendUid", friends[0])
                intent.putExtra("roomId", chatModels[position].roomId)
                v.context.startActivity(intent)
            } else if(friends.size > 1){
                FirebaseDatabase.getInstance().reference.child("chatRooms").orderByChild("users/"+uid).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onCancelled(databaseError: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        for(item in dataSnapshot.children){
                            val model = item.getValue(ChatModel::class.java)

                            if(model!!.users.keys == chatModels[position].users.keys && model.roomId == chatModels[position].roomId){

                                holder.key = item.key

                                intent = Intent(v.context, GroupChatActivity::class.java)
                                intent!!.putExtra("roomUid", item.key.toString())
                                v.context.startActivity(intent)
                            }
                        }
                    }

                })
            }
        }

        //방이름
        if(chatModels[position].roomName[uid] != null)
            holder.tv_roomTitle.text = chatModels[position].roomName[uid]!!

        holder.itemView.setOnLongClickListener { v ->
            val builder = AlertDialog.Builder(v!!.context)
                .setTitle(chatModels[position].roomName[uid])
                .setItems(R.array.edit_chatroom, DialogInterface.OnClickListener{dialog, which ->
                    when(which){
                        0 -> {
                            FirebaseDatabase.getInstance().reference.child("chatRooms").orderByChild("users/"+uid).addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onCancelled(databaseError: DatabaseError) {

                                }

                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    for(item in dataSnapshot.children){
                                        val model = item.getValue(ChatModel::class.java)

                                        if(model!!.users.keys == chatModels[position].users.keys && model.roomId == chatModels[position].roomId){
                                            val intent = Intent(v.context, EditRoomNameActivity::class.java)
                                            intent.putExtra("roomName", chatModels[position].roomName[uid])
                                            intent.putExtra("roomId", item.key.toString())
                                            v.context.startActivity(intent)
                                        }
                                    }
                                }
                            })
                        }
                    }
                })
            builder.show()

            true
        }
    }

    fun getYear(date : Date): Int? = Integer.parseInt(SimpleDateFormat("yyyy", Locale.getDefault()).format(date))

    fun getMonth(date : Date): Int? = Integer.parseInt(SimpleDateFormat("M", Locale.getDefault()).format(date))

    fun getDate(date : Date): Int? = Integer.parseInt(SimpleDateFormat("d", Locale.getDefault()).format(date))

}