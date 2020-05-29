package com.bang.bangtalk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.adapter.NavigationDrawerListViewAdapter
import com.bang.bangtalk.model.ChatModel
import com.bang.bangtalk.model.NotificationModel
import com.bang.bangtalk.model.UserModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_chat.et_message
import kotlinx.android.synthetic.main.activity_chat.ib_send
import kotlinx.android.synthetic.main.activity_chat.rv_comments
import kotlinx.android.synthetic.main.activity_group_chat.*
import kotlinx.android.synthetic.main.activity_group_chat.drawerlayout
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private var roomId : String? = null
private var peopleCount = 0

class GroupChatActivity : AppCompatActivity() {
    private var databaseReference : DatabaseReference? = null
    private var valueEventListener : ValueEventListener? = null
    private var adapter = GroupChatActivityRecyclerViewAdapter()
    private var comments = adapter.commentModels
    private var uid = FirebaseAuth.getInstance().currentUser!!.uid
    private var participants_intent : ArrayList<String> = ArrayList()

    private var ib_exit_isClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        roomId = intent.getStringExtra("roomUid")
        updateIsStartThisActivity(1)

        buttonActivateAndDeactivate()

        rv_comments.layoutManager = LinearLayoutManager(applicationContext)
        rv_comments.adapter = adapter

        setNavigationListView()
        getMessages()

        //Navigation Drawer Swipe Diabled
        drawerlayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).child("roomName").addValueEventListener(object: ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                participants_intent.clear()
                for(item in dataSnapshot.children){
                    //val model = item.getValue(String::class.java)
                    participants_intent.add(item.key!!)
                }
            }
        })
    }

    fun onClick(view : View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        when(view.id){
            R.id.ib_send -> {
                val comment = ChatModel.Comment()
                comment.uid = uid
                comment.message = et_message.text.toString()
                comment.send_time = Calendar.getInstance().time

                FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).child("comments").push().setValue(comment).addOnCompleteListener{
                    val map = HashMap<String, Any>()
                    map["recent"] = Calendar.getInstance().time.time
                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).updateChildren(map)

                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).child("users").addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onCancelled(databaseError: DatabaseError) {

                        }

                        @Suppress("UNCHECKED_CAST")
                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                            for(item in dataSnapshot.children){
                                if(item.child("status").getValue(Int::class.java) == 0){
                                    sendFcm(uid, item.key!!, et_message.text.toString())
                                }
                            }
                            et_message.text.clear()
                        }

                    })
                }
            }

            R.id.ib_close -> {
                updateIsStartThisActivity(0)
                if(valueEventListener != null)
                    databaseReference?.removeEventListener(valueEventListener!!)

                val ref = FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId.toString())

                ref.addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(databaseError: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if(!dataSnapshot.child("comments").exists()){
                            ref.removeValue()
                        }
                    }

                })
                finish()
            }

            R.id.ib_menu -> {
                imm.hideSoftInputFromWindow(et_message.windowToken, 0)
                drawerlayout.openDrawer(GravityCompat.END)
                if(navigation_view != null)
                navigation_view.isVerticalScrollBarEnabled = false
            }

            R.id.ib_exit -> {
                val builder = AlertDialog.Builder(this)
                    .setMessage("채팅방에서 나가시겠습니까?")
                    .setPositiveButton("예"){ _: DialogInterface, _: Int ->
                        ib_exit_isClicked = true
                        val reference = FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!)
                        val exitComment = ChatModel.Comment()
                        //exitComment.message = FirebaseAuth.getInstance().currentUser?.displayName + "님께서 퇴장하셨습니다."
                        exitComment.send_time = Calendar.getInstance().time
                        exitComment.uid = uid

                        constraint_loading.visibility = View.VISIBLE

                        reference.child("roomName").child(uid).removeValue().addOnCompleteListener{
                            updateIsStartThisActivity(2)
                            reference.child("comments").push().setValue(exitComment).addOnCompleteListener {
                                val map = HashMap<String, Any>()
                                map["recent"] = Calendar.getInstance().time.time
                                FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).updateChildren(map)
                            }.addOnFailureListener {
                                constraint_loading.visibility = View.GONE
                            }
                            finish()
                        }
                    }
                    .setNegativeButton("아니요"){ _: DialogInterface, _: Int ->

                    }

                builder.show()

            }

            R.id.btn_invite -> {
                intent = Intent(this, SelectFriendsActivity::class.java)
                intent.putExtra("chatRoomUid", roomId)
                intent.putExtra("friends", this.participants_intent)

                if(this.participants_intent.size > 0)
                    startActivity(intent)
            }
        }
    }

    private fun sendFcm(uid : String, friendUid : String, message : String){
        val gson = Gson()

        val notificationModel = NotificationModel()

        FirebaseDatabase.getInstance().reference.child("users").child(friendUid).addValueEventListener(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                notificationModel.to = dataSnapshot.child("pushToken").value.toString()
                //notificationModel.notification?.title = FirebaseAuth.getInstance().currentUser?.displayName
                //notificationModel.notification?.body = message
                notificationModel.data?.title = FirebaseAuth.getInstance().currentUser?.displayName
                notificationModel.data?.body = message
                notificationModel.data?.sender = FirebaseAuth.getInstance().currentUser!!.uid
                notificationModel.data?.receiver = friendUid
                notificationModel.data?.roomId = roomId

                val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8"), gson.toJson(notificationModel))

                val request : Request = Request.Builder()
                    .header("Content-Type", "application/json")
                    .addHeader("Authorization", "key=AAAA4-nSpBw:APA91bHOLI74doJL5mLHcFtOkaM6nMySTr11IMW3bMHgaND0fHuao7lVyvtilRXHQI0i9cvkl7WPXdKtTP-zuvVeOZReq1_R6IRwe2HaZ66LPKFviVzovCMzUuVNBcCJec9GHSQisY3q")
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {

                    }

                })
            }
        })
    }

    private fun getMessages(){
        databaseReference = FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId.toString()).child("comments")
        valueEventListener = databaseReference!!.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                comments.clear()
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val readUsersMap : HashMap<String, Any> = hashMapOf()

                for(item in dataSnapshot.children){
                    val comment_origin : ChatModel.Comment = item.getValue(ChatModel.Comment::class.java)!!
                    val comment_modify : ChatModel.Comment = item.getValue(ChatModel.Comment::class.java)!!
                    comment_modify.readUsers[uid] = true
                    readUsersMap[item.key.toString()] = comment_modify

                    comments.add(comment_origin)
                }

                if(comments.size > 0){
                    when(comments[comments.size-1].readUsers.containsKey(uid)){
                        true -> {
                            adapter.notifyDataSetChanged()
                            rv_comments.scrollToPosition(comments.size - 1)
                        }
                        false -> {
                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId.toString()).child("comments")
                                .updateChildren(readUsersMap)
                                .addOnCompleteListener {
                                    adapter.notifyDataSetChanged()
                                    rv_comments.scrollToPosition(comments.size - 1)
                                }
                        }
                    }
                }

            }
        })
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        if(drawerlayout.isDrawerOpen(GravityCompat.END)){
            drawerlayout.closeDrawer(GravityCompat.END)
        }else {
            updateIsStartThisActivity(0)
            if(valueEventListener != null)
                databaseReference?.removeEventListener(valueEventListener!!)

            val ref = FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId.toString())

            ref.addValueEventListener(object : ValueEventListener{
                override fun onCancelled(databaseError: DatabaseError) {

                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(!dataSnapshot.child("comments").exists()){
                        ref.removeValue()
                    }
                }

            })
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if(!ib_exit_isClicked)
            updateIsStartThisActivity(0)
    }

    override fun onRestart() {
        super.onRestart()
        updateIsStartThisActivity(1)

        getMessages()

    }

    private fun updateIsStartThisActivity(status : Int){
        val map : HashMap<String, Any> = hashMapOf()
        map["status"] = status
        FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).child("users").child(uid).updateChildren(map)

//        FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).child(uid).addListenerForSingleValueEvent(object: ValueEventListener{
//            override fun onCancelled(databaseError: DatabaseError) {
//
//            }
//
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                if(dataSnapshot.exists())
//                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).child("users").updateChildren(map)
//            }
//
//        })
    }

    private fun setNavigationListView(){
        FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).addValueEventListener(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                tv_roomName.text = dataSnapshot.child("roomName").child(uid).value.toString()
                tv_nav_roomname.text = dataSnapshot.child("roomName").child(uid).value.toString()

                //대화상대 없을 때 보내기 버튼 막기
                val participants : HashMap<String, String>? = dataSnapshot.child("roomName").value as HashMap<String, String>?

                if(participants?.size == 1) {
                    et_message.isEnabled = false
                    et_message.hint = "대화 상대가 없습니다."
                    nav_lv_participants.adapter = NavigationDrawerListViewAdapter(null, null)
                } else {
                    nav_lv_participants.adapter = NavigationDrawerListViewAdapter(null, roomId)
                }
            }
        })
    }

    private fun buttonActivateAndDeactivate(){
        et_message.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(et_message.length() <= 0){
                    ib_send.isEnabled = false
                    ib_send.setBackgroundResource(R.drawable.button_ok_unable)
                } else{
                    ib_send.isEnabled = true
                    ib_send.setBackgroundResource(R.drawable.button_ok)
                }
            }

        })
    }

}

class GroupChatActivityRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    var commentModels: ArrayList<ChatModel.Comment> = ArrayList()
    private var uid = FirebaseAuth.getInstance().currentUser!!.uid

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var layout_message : LinearLayout = view.findViewById(R.id.layout_message)
        var linear_date : LinearLayout = view.findViewById(R.id.linear_date)
        var layout_other : ConstraintLayout = view.findViewById(R.id.constraint_comment_in)
        var layout_mine : LinearLayout = view.findViewById(R.id.linear_comment_out)
        var tv_name : TextView = view.findViewById(R.id.tv_name)
        var tv_comment : TextView = view.findViewById(R.id.tv_comment)
        var tv_comment_out : TextView = view.findViewById(R.id.tv_comment_out)
        var tv_time_in : TextView = view.findViewById(R.id.tv_time_in)
        var tv_time_out : TextView = view.findViewById(R.id.tv_time_out)
        var tv_date : TextView = view.findViewById(R.id.tv_date)
        var tv_read_in : TextView = view.findViewById(R.id.tv_read)
        var tv_read_out : TextView = view.findViewById(R.id.tv_read_out)
        var iv_profile : ImageView = view.findViewById(R.id.iv_profile)
        var tv_exit_message : TextView = view.findViewById(R.id.tv_exitMessage)
        val mView : View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return commentModels.size
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder : MyViewHolder = viewHolder as MyViewHolder

        val sendTime = commentModels[position].send_time?.time
        val str_sendTime: String? = SimpleDateFormat("a h:mm", Locale.getDefault()).format(sendTime)
        val str_sendDate: String? = SimpleDateFormat("yyyy년 M월 d일 E요일", Locale.getDefault()).format(sendTime)

        if(position - 1 == -1){
            holder.linear_date.visibility = View.VISIBLE
            holder.tv_date.text = str_sendDate
        }else{
            val beforeSendTime = commentModels[position-1].send_time?.time
            val str_beforSendDate = SimpleDateFormat("yyyy년 M월 d일 E요일", Locale.getDefault()).format(beforeSendTime)
            val t1 = SimpleDateFormat("yyyy M d a h mm", Locale.getDefault()).format(commentModels[position].send_time!!.time)
            val t2 = SimpleDateFormat("yyyy M d a h mm", Locale.getDefault()).format(commentModels[position-1].send_time!!.time)

            if(str_sendDate != str_beforSendDate){
                holder.linear_date.visibility = View.VISIBLE
                holder.tv_date.text = str_sendDate
            } else{
                holder.linear_date.visibility = View.GONE
            }

            //현 메시지와 전 메시지의 보낸 사람과 보낸 시간이 같을 때
            if(t1 == t2 && commentModels[position].uid.equals(commentModels[position-1].uid)){
                holder.iv_profile.visibility = View.GONE
                holder.tv_name.visibility = View.GONE
            } else {
                holder.iv_profile.visibility = View.VISIBLE
                holder.tv_name.visibility = View.VISIBLE
            }
        }

        when {
            commentModels[position].message == null -> {
                holder.layout_other.visibility = View.GONE
                holder.layout_mine.visibility = View.GONE
                holder.tv_exit_message.visibility = View.VISIBLE

                if(commentModels[position].invite.isNullOrEmpty()){
                    //나간 거
                    FirebaseDatabase.getInstance().reference.child("users").child(commentModels[position].uid!!).addValueEventListener(object: ValueEventListener{
                        override fun onCancelled(databaseError: DatabaseError) {

                        }

                        @SuppressLint("SetTextI18n")
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val name = dataSnapshot.child("name").value.toString()
                            holder.tv_exit_message.text = name + "님이 나갔습니다."
                        }
                    })


                } else {
                    //초대한 거
                    var inviteText : String? = null
                    FirebaseDatabase.getInstance().reference.child("users").child(commentModels[position].uid!!).addValueEventListener(object: ValueEventListener{
                        override fun onCancelled(databaseError: DatabaseError) {

                        }

                        @SuppressLint("SetTextI18n")
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val host = dataSnapshot.child("name").value.toString()


                            for(guest in commentModels[position].invite.keys){
                                FirebaseDatabase.getInstance().reference.child("users").child(guest).addValueEventListener(object: ValueEventListener{
                                    override fun onCancelled(databaseError2: DatabaseError) {

                                    }

                                    override fun onDataChange(dataSnapshot2: DataSnapshot) {
                                        val name = dataSnapshot2.child("name").value.toString()
                                        if(inviteText == null)
                                            inviteText = host + "님이 " + name + "님"
                                        else
                                            inviteText += ", " + name + "님"

                                        holder.tv_exit_message.text = inviteText + "을 초대했습니다."
                                    }

                                })
                            }
                        }
                    })
                }


                //대화 친 사람이 자신
            }
            uid == commentModels[position].uid -> {
                holder.layout_other.visibility = View.GONE
                holder.layout_mine.visibility = View.VISIBLE
                holder.tv_exit_message.visibility = View.GONE

                holder.tv_comment_out.text = commentModels[position].message.toString()
                holder.tv_time_out.text = str_sendTime
                getReadUsersCount(position, holder.tv_read_out)

                //대화 친 사람이 상대
            }
            uid != commentModels[position].uid -> {
                val str_uid = commentModels[position].uid.toString()

                holder.layout_other.visibility = View.VISIBLE
                holder.layout_mine.visibility = View.GONE
                holder.tv_exit_message.visibility = View.GONE

                FirebaseDatabase.getInstance().reference.child("users").child(str_uid).addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(databaseError: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //이름
                        holder.tv_name.text = dataSnapshot.child("name").value.toString()

                        if(dataSnapshot.child("profileImageUrl").exists()){
                            val url = dataSnapshot.child("profileImageUrl").value.toString()
                            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                            val activity = holder.mView.context as Activity

                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                if(!activity.isFinishing){
                                    Glide.with(holder.iv_profile.context).load(uri).apply(RequestOptions.circleCropTransform()).into(holder.iv_profile)
                                }
                            }.addOnFailureListener {
                                holder.iv_profile.setImageResource(R.drawable.ic_account_circle_chat)
                            }
                        }else{
                            holder.iv_profile.setImageResource(R.drawable.ic_account_circle_chat)
                        }
                    }

                })

                //그 사람이 친 대화
                holder.tv_comment.text = commentModels[position].message.toString()
                holder.tv_time_in.text = str_sendTime
                getReadUsersCount(position, holder.tv_read_in)
            }
        }

        if(position+1 <= commentModels.size-1 && commentModels[position].uid.equals(commentModels[position+1].uid)){
            val t1 = SimpleDateFormat("yyyy M d a h mm", Locale.getDefault()).format(commentModels[position].send_time?.time)
            val t2 = SimpleDateFormat("yyyy M d a h mm", Locale.getDefault()).format(commentModels[position+1].send_time?.time)

            holder.layout_message.setPadding(0, 20, 0, 0)

            //현 메시지와 뒤 메시지의 보낸 시간이 같을 때
            if(t1 == t2) {
                holder.tv_time_out.visibility = View.GONE
                holder.tv_time_in.visibility = View.GONE
            }else {
                holder.tv_time_out.visibility = View.VISIBLE
                holder.tv_time_in.visibility = View.VISIBLE
            }
        } else {
            holder.tv_time_out.visibility = View.VISIBLE
            holder.tv_time_in.visibility = View.VISIBLE
            holder.layout_message.setPadding(0, 20, 0, 20)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getReadUsersCount(position: Int, textView: TextView){
        FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId.toString()).child("roomName").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val users : HashMap<String, String> = dataSnapshot.value as HashMap<String, String>
                peopleCount = users.size
                val count = peopleCount - commentModels[position].readUsers.size

                if(count > 0){
                    textView.visibility = View.VISIBLE
                    textView.text = count.toString()
                }
                else
                    textView.visibility = View.INVISIBLE
            }
        })
    }
}
