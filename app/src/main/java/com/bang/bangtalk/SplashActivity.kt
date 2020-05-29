package com.bang.bangtalk

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : AppCompatActivity() {

    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        remoteConfig.setConfigSettings(configSettings)
        remoteConfig.setDefaults(R.xml.default_config)

        remoteConfig.fetch(0)
            .addOnCompleteListener(this,
                OnCompleteListener<Void?> { task ->
                    if (task.isSuccessful) {
                        // After config data is successfully fetched, it must be activated before newly fetched
                        // values are returned.
                        remoteConfig.activateFetched()
                    } else {

                    }
                    displayMessage()
                })

    }

    private fun displayMessage() {
        var splash_background: String? = remoteConfig.getString("splash_background")
        var caps: Boolean? = remoteConfig.getBoolean("splash_message_caps")
        var splash_message: String? = remoteConfig.getString("splash_message")

        linear_splashactivity.setBackgroundColor(Color.parseColor(splash_background))

        if(caps == true){

            val builder = AlertDialog.Builder(this)
            builder.setMessage(splash_message)
            builder.setPositiveButton("확인"){dialog, id ->
                finish()
            }
            builder.create().show()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
