package com.bang.bangtalk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.Interface.ItemClickListener
import com.bang.bangtalk.adapter.SelectFriendsActivityRecyclerViewAdapter
import com.bang.bangtalk.model.ChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SelectFriendsActivity : AppCompatActivity() {

    var chatModel = ChatModel()
    var participants : ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_friends)

        val friends = intent.getStringArrayListExtra("friends")
        val adapter = SelectFriendsActivityRecyclerViewAdapter(friends)
        val rv : RecyclerView = findViewById(R.id.rv_select)
        val btn : Button = findViewById(R.id.btn_ok)

        rv.layoutManager = LinearLayoutManager(applicationContext)
        rv.adapter = adapter

        val user = ChatModel.User()
        user.status = 0

        adapter.setOnItemClickListener(object : ItemClickListener {

            override fun onItemClick(v: View, position: Int, isChecked: Boolean) {
                if(isChecked){
                    chatModel.users[adapter.userModels[position].uid.toString()] = user
                    participants.add(adapter.userModels[position].uid.toString())

                } else {
                    chatModel.users.remove(adapter.userModels[position].uid)
                    participants.remove(adapter.userModels[position].uid)
                }

                if(participants.size <= 0){
                    btn.isEnabled = false
                    btn.setBackgroundResource(R.drawable.button_ok_unable)
                }else{
                    btn.isEnabled = true
                    btn.setBackgroundResource(R.drawable.button_ok)
                }
            }
        })
    }

    fun onClick(view: View){
        when(view.id){
            R.id.btn_ok -> {
                val uid = FirebaseAuth.getInstance().currentUser!!.uid

                if(intent.getStringExtra("chatRoomUid").isNullOrEmpty()){
                    if(participants.size > 1){
                        val me = ChatModel.User()
                        me.status = 1
                        me.invite_time = Calendar.getInstance().time.time
                        chatModel.users[uid] = me
                        participants.add(uid)

                        FirebaseDatabase.getInstance().reference.child("chatRooms").push().setValue(chatModel).addOnCompleteListener{

                            FirebaseDatabase.getInstance().reference.child("chatRooms").orderByChild("users/"+uid).addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onCancelled(databaseError: DatabaseError) {

                                }

                                @Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val map = HashMap<String, Any>()

                                    for(item in dataSnapshot.children){
                                        val model = item.getValue(ChatModel::class.java)
                                        val model2 = item.child("users").getValue(ChatModel.User::class.java)!!
                                        if(model!!.users.keys == chatModel.users.keys){

                                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(item.key!!).addListenerForSingleValueEvent(object : ValueEventListener{
                                                override fun onCancelled(databaseError2: DatabaseError) {

                                                }

                                                override fun onDataChange(dataSnapshot2: DataSnapshot) {
                                                    val room : ChatModel = dataSnapshot2.getValue(ChatModel::class.java)!!
                                                    val mapId = HashMap<String, Any>()
                                                    mapId["roomId"] = item.key!!
                                                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(item.key!!).updateChildren(mapId)

                                                    for(i in 0 until participants.size){
                                                        val mapInvite = HashMap<String, Any>()
                                                        mapInvite["invite_time"] = Calendar.getInstance().time.time
                                                        FirebaseDatabase.getInstance().reference.child("chatRooms").child(item.key!!).child("users").child(participants[i]).updateChildren(mapInvite)

                                                        for(j in 0 until participants.size){
                                                            if(participants[i] != participants[j]){
                                                                FirebaseDatabase.getInstance().reference.child("users").child(participants[j]).addListenerForSingleValueEvent(object : ValueEventListener{
                                                                    override fun onCancelled(databaseError3: DatabaseError) {

                                                                    }

                                                                    override fun onDataChange(dataSnapshot3: DataSnapshot) {

                                                                        if(room.roomName[participants[i]].isNullOrEmpty()){
                                                                            room.roomName[participants[i]] = dataSnapshot3.child("name").value.toString()

                                                                        } else{
                                                                            room.roomName[participants[i]] +=  ", " + dataSnapshot3.child("name").value.toString()

                                                                            map[participants[i]] = room.roomName[participants[i]].toString()
                                                                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(item.key!!).child("roomName").updateChildren(map)
                                                                        }
                                                                    }
                                                                })
                                                            }
                                                        }
                                                    }
                                                }
                                            })

                                            intent = Intent(applicationContext, GroupChatActivity::class.java)
                                            intent.putExtra("roomUid", item.key.toString())
                                            startActivity(intent)
                                            finish()
                                        }
                                    }
                                }
                            })
                        }
                    } else {
                        for(friendUid in participants){
                            intent = Intent(applicationContext, ChatActivity::class.java)
                            intent.putExtra("friendUid", friendUid)
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    val roomId = intent.getStringExtra("chatRoomUid")
                    val map1 = HashMap<String, Any>()   //users
                    val map2 = HashMap<String, Any>()   //room name

                    val inviteComment = ChatModel.Comment()
                    inviteComment.uid = uid
                    inviteComment.send_time = Calendar.getInstance().time

                    for(i in participants.indices){
                        map1["status"] = 0
                        map1["invite_time"] = Calendar.getInstance().time.time
                        inviteComment.invite[participants[i]] = true

                        //user 업데이트
                        FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId).child("users").updateChildren(map1).addOnCompleteListener {

                            //방이름 업데이트
                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId).addListenerForSingleValueEvent(object: ValueEventListener{
                                override fun onCancelled(databaseError: DatabaseError) {

                                }

                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val room : ChatModel = dataSnapshot.getValue(ChatModel::class.java)!!

                                    for(item in dataSnapshot.child("users").children){
                                        if(item.getValue(Int::class.java) != 2){
                                            FirebaseDatabase.getInstance().reference.child("users").child(item.key!!).addListenerForSingleValueEvent(object: ValueEventListener{
                                                override fun onCancelled(databaseError2: DatabaseError) {

                                                }

                                                override fun onDataChange(dataSnapshot2: DataSnapshot) {
                                                    val name = dataSnapshot2.child("name").value.toString()
                                                    val dsuid = dataSnapshot2.child("uid").value.toString()

                                                    if(participants[i] != dsuid){
                                                        if(room.roomName[participants[i]].isNullOrEmpty())
                                                            room.roomName[participants[i]] = name
                                                        else{
                                                            room.roomName[participants[i]] += ", $name"

                                                            map2[participants[i]] = room.roomName[participants[i]].toString()
                                                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId).child("roomName").updateChildren(map2)
                                                        }
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            })

                            finish()
                        }
                    }

                    //comment에 추가
                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId).child("comments").push().setValue(inviteComment)
                }
            }

            R.id.ib_back -> {
                finish()
            }
        }
    }

}
