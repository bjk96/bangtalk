package com.bang.bangtalk

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bang.bangtalk.Fragment.ChatroomFragment
import com.bang.bangtalk.Fragment.FriendFragment
import com.bang.bangtalk.Fragment.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {
    var permissions =
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA)

    val MULTIPLE_PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //이 액티비티가 뜨자마자 뜨는 Fragment
        supportFragmentManager.beginTransaction().replace(R.id.main_frame, FriendFragment()).commit()

        //bottomNavigationView의 메뉴를 선택할 시 동작하는 리스너
        bottomNavigationView.setOnNavigationItemSelectedListener(object : BottomNavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                when(item.itemId){
                    R.id.action_friends -> {
                        supportFragmentManager.beginTransaction().replace(R.id.main_frame, FriendFragment()).commit()
                        return true
                    }

                    R.id.action_chatroom -> {
                        supportFragmentManager.beginTransaction().replace(R.id.main_frame, ChatroomFragment()).commit()
                        return true
                    }

                    R.id.action_settings -> {
                        supportFragmentManager.beginTransaction().replace(R.id.main_frame, SettingsFragment()).commit()
                        return true
                    }
                }
                return false
            }
        })

        passPushTokenToServer()

        checkPermissions()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
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

    fun passPushTokenToServer(){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        //토큰 생성
        val token = FirebaseInstanceId.getInstance().token
        //메인화면이 켜지자마자 토큰을 업데이트하기위해 HashMap 사용
        val map : HashMap<String, Any> = hashMapOf()
        map["pushToken"] = token!!
        //db에서 해당 유저의 pushToken 업데이트
        FirebaseDatabase.getInstance().reference.child("users").child(uid).updateChildren(map)
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
}