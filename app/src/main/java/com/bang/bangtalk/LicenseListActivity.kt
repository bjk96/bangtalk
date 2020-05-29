package com.bang.bangtalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.adapter.LicenseListActivityRecyclerViewAdapter

class LicenseListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_list)

        val rv : RecyclerView = findViewById(R.id.rv_license)
        val ib_back : ImageButton = findViewById(R.id.ib_back)
        val adapter = LicenseListActivityRecyclerViewAdapter(applicationContext)
        rv.layoutManager = LinearLayoutManager(applicationContext)
        rv.adapter = adapter

        ib_back.setOnClickListener {
            finish()
        }
    }
}
