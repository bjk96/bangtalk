package com.bang.bangtalk.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.R
import com.bang.bangtalk.SelectFriendsActivity
import com.bang.bangtalk.adapter.ChatroomFragmentRecyclerViewAdapter
import com.bang.bangtalk.model.ChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_chatroom.*

class ChatroomFragment() : Fragment() {

    private val adapter = ChatroomFragmentRecyclerViewAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.fragment_chatroom, container, false)
        val rv_chatroom : RecyclerView = view.findViewById(R.id.rv_chatRooms)
        val ib_addChat : ImageButton = view.findViewById(R.id.ib_addChat)
        val loading : ConstraintLayout = view.findViewById(R.id.constraint_loading)

        rv_chatroom.addItemDecoration(DividerItemDecoration(inflater.context, 1))
        val manager : LinearLayoutManager = LinearLayoutManager(inflater.context)
        rv_chatroom.layoutManager = manager
        rv_chatroom.adapter = adapter

        ib_addChat.setOnClickListener {
            startActivity(Intent(it.context, SelectFriendsActivity::class.java))
        }

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().reference.child("chatRooms").orderByChild("recent").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                adapter.chatModels.clear()
                for(item in dataSnapshot.children){
                    val model : ChatModel = item.getValue<ChatModel>(ChatModel::class.java)!!
                    if(model.comments.isNotEmpty() && model.users.containsKey(uid) && model.users[uid]?.status != 2){
                        adapter.chatModels.add(0, model)
                        adapter.keys.add(item.key.toString())
                    }
                }
                adapter.notifyDataSetChanged()
                loading.visibility = View.GONE
            }
        })

        return view
    }
}