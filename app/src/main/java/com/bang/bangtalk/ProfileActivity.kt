package com.bang.bangtalk

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    private val MINE_INTENT = 0
    private val OTHER_INTENT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //status bar 색상 변경 SDK 버전 21이상
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = Color.rgb(68, 68, 102)
        }

        //인텐트로 이름과 사진url 가져옴.
        val name : String = intent.getStringExtra("name")

        tv_name.text = name

        if(intent.getIntExtra("check", 0) ==  MINE_INTENT){
            btn_chat.visibility = View.GONE
            btn_edit.visibility = View.VISIBLE
        } else {
            btn_chat.visibility = View.VISIBLE
            btn_edit.visibility = View.GONE
        }

        if(!intent.getStringExtra("profileImageUrl").isNullOrEmpty()){
            val url = intent.getStringExtra(("profileImageUrl"))

            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)

            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(applicationContext).load(uri).apply(RequestOptions.circleCropTransform()).into(iv_profile)
            }
        }
    }

    fun onClick(view: View){
        when(view.id){
            R.id.linear_empty -> {
                finish()
            }

            R.id.ib_close -> {
                finish()
            }

            R.id.btn_chat -> {
                val chatIntent = Intent(this, ChatActivity::class.java)
                chatIntent.putExtra("friendUid", intent.getStringExtra("friendUid"))
                startActivity(chatIntent)
            }

            R.id.btn_edit -> {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
        }
    }
}
