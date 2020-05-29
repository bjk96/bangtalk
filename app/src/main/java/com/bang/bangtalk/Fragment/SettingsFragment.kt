package com.bang.bangtalk.Fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.bang.bangtalk.EditProfileActivity
import com.bang.bangtalk.LicenseListActivity
import com.bang.bangtalk.LoginActivity
import com.bang.bangtalk.R
import com.bang.bangtalk.adapter.SettingsFragmentListViewAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.fragment_settings, container, false)
        val lv_menu : ListView = view.findViewById(R.id.lv_settings)
        val adapter = SettingsFragmentListViewAdapter(view.context)
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        lv_menu.adapter = adapter

        lv_menu.onItemClickListener =
            AdapterView.OnItemClickListener { _, view1, position, _ ->
                when(position){
                    adapter.count-1 ->
                        startActivity(Intent(view1.context, LicenseListActivity::class.java))
                    1 -> {
                        val builder = AlertDialog.Builder(view1.context)
                            .setMessage("로그아웃하시겠습니까?")
                            .setPositiveButton("예"){ _: DialogInterface, _: Int ->
                                FirebaseDatabase.getInstance().reference.child("users").child(uid).child("pushToken").removeValue()
                                FirebaseAuth.getInstance().signOut()
                                activity!!.finish()
                                startActivity(Intent(view1.context, LoginActivity::class.java))
                            }
                            .setNegativeButton("아니요"){ _: DialogInterface, _: Int ->

                            }
                        builder.show()
                    }
                    0 ->
                        startActivity(Intent(view1.context, EditProfileActivity::class.java))
                }
            }

        return view
    }
}