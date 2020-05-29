package com.bang.bangtalk

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bang.bangtalk.model.UserModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_join.*

class JoinActivity : AppCompatActivity() {

    private val PICK_FROM_ALBUM = 10
    private var imageUri: Uri? = null

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val PERMISSION_REQUEST_EXTERNAL_STORAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)

        //버튼 활성화/비활성화
        buttonActivateAndDeactivate()

    }

    fun onClick(view: View){

        when(view.id){
            R.id.btn_join -> {
                constraint_loading.visibility = View.VISIBLE
                mAuth.createUserWithEmailAndPassword(edit_email.text.toString(), edit_password.text.toString())
                    .addOnCompleteListener(this){ task ->
                        if(task.isSuccessful){
                            val user = mAuth.currentUser
                            val uid = user!!.uid
                            val model = UserModel()
                            val riversRef = FirebaseStorage.getInstance().reference.child("userImages").child(uid)

                            val userProfileChangeRequest : UserProfileChangeRequest = UserProfileChangeRequest.Builder().setDisplayName(edit_name.text.toString()).build()
                            task.result?.user?.updateProfile(userProfileChangeRequest)

                            if(imageUri != null){
                                val uploadTask  = riversRef.putFile(imageUri!!)

                                uploadTask.addOnCompleteListener { task ->
                                    if(task.isSuccessful){
                                        model.email = user.email.toString()
                                        model.name = edit_name.text.toString()
//                                        model.phoneNumber = edit_phoneNumber.text.toString()
                                        model.uid = uid
                                        model.profileImageUrl = riversRef.toString()

                                        FirebaseDatabase.getInstance().reference.child("users").child(uid).setValue(model)

                                        Toast.makeText(this, "가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                        finish()
                                    } else {
                                        constraint_loading.visibility = View.GONE
                                        Toast.makeText(this, "잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }else{
                                model.email = user.email.toString()
                                model.name = edit_name.text.toString()
//                                model.phoneNumber = edit_phoneNumber.text.toString()
                                model.uid = uid

                                FirebaseDatabase.getInstance().reference.child("users").child(uid).setValue(model)

                                Toast.makeText(this, "가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                finish()
                            }

                        }else { // If sign in fails, display a message to the user.
                            constraint_loading.visibility = View.GONE
                            Toast.makeText(this, "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        }

                    }
            }

            R.id.iv_profile -> {
                val builder1 = AlertDialog.Builder(this)
                    .setItems(R.array.setImageNoUri, DialogInterface.OnClickListener { dialog, which ->
                        when(which){
                            0 -> {

                            }
                            1 -> {
                                openAlbum()
                            }
                        }
                    })

                val builder2 = AlertDialog.Builder(this)
                    .setItems(R.array.setImage, DialogInterface.OnClickListener { dialog, which ->
                        when(which){
                            0 -> {

                            }
                            1 -> {
                                openAlbum()
                            }
                            2 -> {
                                imageUri = null
                                iv_profile.setImageResource(R.drawable.ic_account_circle)
                            }
                        }
                    })

                if(imageUri == null)
                    builder1.show()
                else
                    builder2.show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            Glide.with(applicationContext).load(data!!.data).apply(RequestOptions().optionalCircleCrop()).into(iv_profile)
            imageUri = data.data                //이미지 경로 원본
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST_EXTERNAL_STORAGE -> {
                if(grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "허용해야 해당 기능을 실행할 수 있습니다.", Toast.LENGTH_SHORT).show()
                else if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openAlbum()
            }
        }
    }

    fun openAlbum(){
        val intent = Intent()
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            intent.action = Intent.ACTION_PICK
            intent.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(intent, PICK_FROM_ALBUM)
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_EXTERNAL_STORAGE)
        }
    }

    fun buttonActivateAndDeactivate(){

        edit_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (edit_name.length() == 0 || edit_email.length() == 0 || !android.util.Patterns.EMAIL_ADDRESS.matcher(edit_email.text.toString()).matches() || edit_password.length() < 6) {
                    btn_join.setBackgroundResource(R.drawable.button_login_unable)
                    btn_join.isEnabled = false
                } else {
                    btn_join.setBackgroundResource(R.drawable.button_enable)
                    btn_join.isEnabled = true
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        edit_email.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (edit_name.length() == 0 || edit_email.length() == 0 || !android.util.Patterns.EMAIL_ADDRESS.matcher(edit_email.text.toString()).matches() || edit_password.length() < 6) {
                    btn_join.setBackgroundResource(R.drawable.button_login_unable)
                    btn_join.isEnabled = false
                } else {
                    btn_join.setBackgroundResource(R.drawable.button_enable)
                    btn_join.isEnabled = true
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        edit_password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (edit_name.length() == 0 || edit_email.length() == 0 || !android.util.Patterns.EMAIL_ADDRESS.matcher(edit_email.text.toString()).matches() || edit_password.length() < 6 ) {
                    btn_join.setBackgroundResource(R.drawable.button_login_unable)
                    btn_join.isEnabled = false
                } else {
                    btn_join.setBackgroundResource(R.drawable.button_enable)
                    btn_join.isEnabled = true
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }
}
