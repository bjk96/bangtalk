package com.bang.bangtalk

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bang.bangtalk.model.UserModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private val RC_SIGN_IN = 200
    private var googleSignInClient: GoogleSignInClient? = null

    var permissions =
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA)

    val MULTIPLE_PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)



        //firebaseAuth!!.signOut()

        //로그인 버튼 활성화/비활성화
        buttonActivateAndDeactivate(btn_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            var user: FirebaseUser? = firebaseAuth.currentUser

            if(user != null){
                //로그인
                val intent: Intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
                finish()
            }else{
                checkPermissions()
            }
        }

        btn_google.setOnClickListener {
            val signInIntent = googleSignInClient?.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    fun onClick(view: View){
        when (view.id) {
            R.id.btn_login -> {
                constraint_loading.visibility = View.VISIBLE
                loginEvent()

            }
            R.id.btn_join -> {
                startActivity(Intent(this, JoinActivity::class.java))
            }

            R.id.btn_google -> {
                val signInIntent = googleSignInClient?.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }

    fun loginEvent(){
        val str1: String = edit_id.text.toString()
        val str2: String = edit_password.text.toString()
        firebaseAuth?.signInWithEmailAndPassword(str1, str2)?.addOnCompleteListener(this, OnCompleteListener<AuthResult?> { task ->
            if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information

            } else { // If sign in fails, display a message to the user.
                constraint_loading.visibility = View.GONE
                Toast.makeText(this, "로그인에 실패했습니다.",Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun buttonActivateAndDeactivate(button: Button){
        edit_id.addTextChangedListener(object : TextWatcher {
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
                if (edit_id.length() == 0 || !android.util.Patterns.EMAIL_ADDRESS.matcher(edit_id.text.toString()).matches() || edit_password.length() < 6) {
                    button.setBackgroundResource(R.drawable.button_login_unable)
                    button.setEnabled(false)
                } else if (edit_id.length() > 0 && edit_password.length() >= 6) {
                    button.setBackgroundResource(R.drawable.button_login)
                    button.setEnabled(true)
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
                if (edit_id.length() == 0 || edit_password.length() < 6) {
                    button.setBackgroundResource(R.drawable.button_login_unable)
                    button.setEnabled(false)
                } else if (edit_id.length() > 0 && edit_password.length() >= 6) {
                    button.setBackgroundResource(R.drawable.button_login)
                    button.setEnabled(true)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth?.addAuthStateListener(authStateListener)
    }

    override fun onStop(){
        super.onStop()
        firebaseAuth?.removeAuthStateListener(authStateListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            MULTIPLE_PERMISSIONS_REQUEST_CODE -> {
                if(grantResults.isNotEmpty()){
                    for((i, permission) in permissions.withIndex()){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(applicationContext, "권한 획득 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        var rejectedPermissionList = ArrayList<String>()

        //필요한 퍼미션들을 하나씩 끄집어내서 현재 권한을 받았는지 체크
        for(permission in permissions){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                //만약 권한이 없다면 rejectedPermissionList에 추가
                rejectedPermissionList.add(permission)
            }
        }
        //거절된 퍼미션이 있다면...
        if(rejectedPermissionList.isNotEmpty()){
            //권한 요청!
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(this, rejectedPermissionList.toArray(array), MULTIPLE_PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                RC_SIGN_IN -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)
                }
            }

        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth!!.signInWithCredential(credential).addOnCompleteListener(this) {
            if(it.isSuccessful){
                val user = firebaseAuth?.currentUser
                val model = UserModel()
                model.uid = user!!.uid
                model.name = user.displayName
                model.email = user.email
                FirebaseDatabase.getInstance().reference.child("users").child(user.uid).setValue(model)
            } else
                Toast.makeText(this, "다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        }
    }
}
