package com.bang.bangtalk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.adapter.NavigationDrawerListViewAdapter
import com.bang.bangtalk.model.ChatModel
import com.bang.bangtalk.model.NotificationModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_chat.*
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private var chatRoomUid: String? = null
private var peopleCount = 0

class ChatActivity : AppCompatActivity() {
    private var chatActivityRecyclerViewAdapter = ChatActivityRecyclerViewAdapter()
    private var comments = chatActivityRecyclerViewAdapter.commentModels
    private var databaseReference : DatabaseReference? = null
    private var valueEventListener : ValueEventListener? = null
    private val uid = FirebaseAuth.getInstance().currentUser!!.uid

    private var ib_exit_isClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val friendUid = intent.getStringExtra("friendUid")

        checkChatRoom()
        buttonActivateAndDeactivate()

        FirebaseDatabase.getInstance().reference.child("users").child(friendUid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                tv_roomName.text = dataSnapshot.child("name").value.toString()
            }
        })

        //Navigation Drawer Swipe Diabled
        drawerlayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun onClick(view: View){

        //val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val friendUid = intent.getStringExtra("friendUid")
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        when(view.id){
            R.id.ib_send -> {
                val chatModel: ChatModel = ChatModel()

                ib_send.isEnabled = false
                chatModel.recent = Calendar.getInstance().time.time

                val comment = ChatModel.Comment()
                comment.uid = uid
                comment.message = et_message.text.toString()
                comment.send_time = Calendar.getInstance().time

                val me = ChatModel.User()
                me.status = 1
                me.invite_time = Calendar.getInstance().time.time

                val friend = ChatModel.User()
                friend.status = 0
                friend.invite_time = Calendar.getInstance().time.time

                if(chatRoomUid.isNullOrEmpty()){
                    chatModel.comments["--"] = comment
                    chatModel.users[uid] = me
                    chatModel.users[friendUid] = friend
                    FirebaseDatabase.getInstance().reference.child("users").child(friendUid).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onCancelled(databaseError: DatabaseError) {

                        }

                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            chatModel.roomName[uid] = dataSnapshot.child("name").value.toString()
                            chatModel.roomName[friendUid] = FirebaseAuth.getInstance().currentUser?.displayName.toString()

                            FirebaseDatabase.getInstance().reference.child("users").child(uid).addListenerForSingleValueEvent(object: ValueEventListener{
                                override fun onCancelled(databaseError2: DatabaseError) {

                                }

                                override fun onDataChange(dataSnapshot2: DataSnapshot) {

                                }

                            })

                            FirebaseDatabase.getInstance().reference.child("chatRooms").push().setValue(chatModel).addOnSuccessListener{
                                checkChatRoom()
                                sendFcm(uid, friendUid, et_message.text.toString())

                                FirebaseDatabase.getInstance().reference.child("chatRooms").orderByChild("users/"+uid).addListenerForSingleValueEvent(object: ValueEventListener{
                                    override fun onCancelled(databaseError2: DatabaseError) {

                                    }

                                    override fun onDataChange(dataSnapshot2: DataSnapshot) {
                                        val mapId = HashMap<String, Any>()

                                        for(item in dataSnapshot2.children){
                                            val model : ChatModel = item.getValue(ChatModel::class.java)!!
                                            if(model.users.containsKey(uid) && model.users.containsKey(friendUid) && model.users[friendUid]?.status != 2){
                                                mapId["roomId"] = item.key!!
                                                FirebaseDatabase.getInstance().reference.child("chatRooms").child(item.key!!).updateChildren(mapId)
                                            }
                                        }
                                    }

                                })
                                et_message.text.clear()
                            }
                        }
                    })

                } else{
                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid!!).child("comments").push().setValue(comment)
                        .addOnSuccessListener {
                            val map = HashMap<String, Any>()
                            map["recent"] = Calendar.getInstance().time.time
                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid!!).updateChildren(map)

                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid!!).child("users").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onCancelled(databaseError: DatabaseError) {

                                }

                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if(dataSnapshot.child(friendUid).child("status").getValue(Int::class.java) == 0){
                                        sendFcm(uid, friendUid, et_message.text.toString())
                                        et_message.text.clear()
                                    } else{
                                        et_message.text.clear()
                                    }
                                }
                            })
                        }
                }
            }

            R.id.ib_close -> {
                updateIsStartThisActivity(0)
                databaseReference?.removeEventListener(valueEventListener!!)
                chatRoomUid = null
                finish()
            }

            R.id.ib_menu -> {
                imm.hideSoftInputFromWindow(et_message.windowToken, 0)
                drawerlayout.openDrawer(GravityCompat.END)
                if(navigation_view != null)
                    navigation_view.isVerticalScrollBarEnabled = false
                imm.showSoftInput(et_message, 0)
            }

            R.id.ib_exit -> {
                if(chatRoomUid != null){
                    val builder = AlertDialog.Builder(this)
                        .setMessage("채팅방에서 나가시겠습니까?")
                        .setPositiveButton("예"){ _: DialogInterface, _: Int ->
                            ib_exit_isClicked = true
                            val reference = FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid!!)
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
                                    FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid!!).updateChildren(map)
                                    chatRoomUid = null
                                }.addOnFailureListener {
                                    constraint_loading.visibility = View.GONE
                                }
                                finish()
                            }
                        }
                        .setNegativeButton("아니요"){ _: DialogInterface, _: Int ->

                        }
                    builder.show()
                } else finish()

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
                notificationModel.data?.roomId = chatRoomUid

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

    private fun checkChatRoom() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val friendUid = intent.getStringExtra("friendUid")
        val manager = LinearLayoutManager(applicationContext)
        val roomId = intent.getStringExtra("roomId")
        rv_comments.layoutManager = manager

        FirebaseDatabase.getInstance().reference.child("chatRooms").orderByChild("users/" + uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (item in dataSnapshot.children) {
                    val chatModel: ChatModel? = item.getValue(ChatModel::class.java)

                    if (chatModel!!.roomId == roomId || (chatModel.users.containsKey(uid) && chatModel.users.containsKey(friendUid) && chatModel.users.size == 2 && chatModel.users[uid]?.status != 2)) {

                        chatRoomUid = item.key.toString()
                        rv_comments.adapter = chatActivityRecyclerViewAdapter
                        if(chatRoomUid!!.isNotEmpty()) {

                            updateIsStartThisActivity(1)

                            FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid!!).child("roomName").addValueEventListener(object : ValueEventListener{
                                override fun onCancelled(databaseError: DatabaseError) {

                                }

                                @Suppress("UNCHECKED_CAST")
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if (dataSnapshot.child(uid).exists())
                                        tv_roomName.text = dataSnapshot.child(uid).value.toString()

//                                    if(!dataSnapshot.child(friendUid).exists()) {
                                        //대화상대 없을 때 보내기 버튼 막기
                                        val participants : HashMap<String?, String?> = dataSnapshot.value as HashMap<String?, String?>

                                        if(participants.size == 1) {
                                            et_message.isEnabled = false
                                            et_message.hint = "대화 상대가 없습니다."
                                            nav_lv_participants.adapter = NavigationDrawerListViewAdapter(null, null)
                                        } else {
                                            nav_lv_participants.adapter = NavigationDrawerListViewAdapter(friendUid, null)
                                        }
//                                    }
                                }
                            })
                        }
                        getMessages()
                    }
                }
            }
        })
    }

    private fun getMessages(){
        databaseReference = FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid.toString()).child("comments")
        valueEventListener = databaseReference!!.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                chatActivityRecyclerViewAdapter.commentModels.clear()
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val readUsersMap : HashMap<String, Any> = hashMapOf()

                for(item in dataSnapshot.children){
                    val comment_origin : ChatModel.Comment = item.getValue(ChatModel.Comment::class.java)!!
                    val comment_modify : ChatModel.Comment = item.getValue(ChatModel.Comment::class.java)!!
                    comment_modify.readUsers[uid] = true
                    readUsersMap[item.key.toString()] = comment_modify

                    comments.add(comment_origin)
                }

                when(comments[comments.size-1].readUsers.containsKey(uid)){
                    true -> {
                        chatActivityRecyclerViewAdapter.notifyDataSetChanged()
                        rv_comments.scrollToPosition(comments.size - 1)
                    }
                    false -> {
                        FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid.toString()).child("comments")
                            .updateChildren(readUsersMap)
                            .addOnCompleteListener {
                                chatActivityRecyclerViewAdapter.notifyDataSetChanged()
                                rv_comments.scrollToPosition(comments.size - 1)
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
        } else {
            updateIsStartThisActivity(0)
            if(valueEventListener != null)
                databaseReference?.removeEventListener(valueEventListener!!)
            chatRoomUid = null
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
        checkChatRoom()
    }

    private fun updateIsStartThisActivity(status: Int){
        val map : HashMap<String, Any> = hashMapOf()
        if(chatRoomUid != null){
            map["status"] = status
            FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid!!).child("users").child(uid).updateChildren(map)
        }
    }

    private fun buttonActivateAndDeactivate(){
        et_message.addTextChangedListener(object: TextWatcher{
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

class ChatActivityRecyclerViewAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var commentModels: ArrayList<ChatModel.Comment> = ArrayList()

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
        var tv_exit : TextView = view.findViewById(R.id.tv_exitMessage)
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

        val str_uid = commentModels[position].uid.toString()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

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

        when{
            commentModels[position].message == null -> {
                holder.layout_other.visibility = View.GONE
                holder.layout_mine.visibility = View.GONE
                holder.tv_exit.visibility = View.VISIBLE
                //holder.tv_exit.text = commentModels[position].message

                FirebaseDatabase.getInstance().reference.child("users").child(commentModels[position].uid!!).addValueEventListener(object: ValueEventListener{
                    override fun onCancelled(databaseError: DatabaseError) {

                    }

                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val name = dataSnapshot.child("name").value.toString()
                        holder.tv_exit.text = name + "님이 나갔습니다."
                    }

                })
            }

            uid == commentModels[position].uid ->{
                holder.layout_other.visibility = View.GONE
                holder.layout_mine.visibility = View.VISIBLE
                holder.tv_exit.visibility = View.GONE

                holder.tv_comment_out.text = commentModels[position].message
                holder.tv_time_out.text = str_sendTime

                getReadUsersCount(position, holder.tv_read_out)
            }

            uid != commentModels[position].uid -> {
                holder.layout_other.visibility = View.VISIBLE
                holder.layout_mine.visibility = View.GONE
                holder.tv_exit.visibility = View.GONE

                //해당 대화를 친 사람 이름과 사진
                FirebaseDatabase.getInstance().reference.child("users").child(str_uid).addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(databaseError: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //이름
                        holder.tv_name.text = dataSnapshot.child("name").value.toString()

                        //사진
                        if(dataSnapshot.child("profileImageUrl").exists()){
                            val url = dataSnapshot.child("profileImageUrl").value.toString()
                            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                            val activity = holder.mView.context as Activity

                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                if(!activity.isFinishing)
                                    Glide.with(holder.iv_profile.context).load(uri).apply(RequestOptions.circleCropTransform()).into(holder.iv_profile)
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

    fun getReadUsersCount(position: Int, textView: TextView){
        if(peopleCount == 0){
            FirebaseDatabase.getInstance().reference.child("chatRooms").child(chatRoomUid.toString()).child("roomName").addListenerForSingleValueEvent(object : ValueEventListener{
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
        } else{
            val count = peopleCount - commentModels[position].readUsers.size

            if(count > 0){
                textView.visibility = View.VISIBLE
                textView.text = count.toString()
            }
            else
                textView.visibility = View.INVISIBLE
        }

    }
}