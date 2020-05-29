package com.bang.bangtalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_edit_room_name.*

class EditRoomNameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_room_name)

        et_roomName.setText(intent.getStringExtra("roomName"))

        et_roomName.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                btn_ok.performClick()
            }
            true
        }

        //buttonActivateAndDeactivate()
    }

    fun onClick(view: View) {
        when(view.id){
            R.id.ib_close ->
                finish()
            R.id.btn_ok -> {
                updateRoomName()
            }
        }
    }

    private fun updateRoomName(){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val roomId = intent.getStringExtra("roomId")

        if(et_roomName.length() > 0){
            val map : HashMap<String, Any> = hashMapOf()
            map[uid] = et_roomName.text.toString()
            FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId!!).child("roomName").updateChildren(map)

            finish()
        } else {
            Toast.makeText(applicationContext, "채팅방 이름은 최소 1자 이상이여야 합니다.", Toast.LENGTH_SHORT).show()
        }

    }

    private fun buttonActivateAndDeactivate(){
        et_roomName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(et_roomName.length() <= 0){
                    btn_ok.visibility = View.GONE
                } else{
                    btn_ok.visibility = View.VISIBLE
                }
            }

        })
    }
}
