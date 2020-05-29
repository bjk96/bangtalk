package com.bang.bangtalk.Fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.ProfileActivity
import com.bang.bangtalk.R
import com.bang.bangtalk.model.UserModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

private val MINE_INTENT = 0
private val OTHER_INTENT = 1

class FriendFragment : Fragment() {

    private var adapter = FriendFragmentRecyclerViewAdapter()



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.fragment_friend, container, false)
        val rv_friends : RecyclerView = view.findViewById(R.id.rv_friends)
        val tv_myName : TextView = view.findViewById(R.id.tv_myName)
        val tv_myComment : TextView = view.findViewById(R.id.tv_myComment)
        val iv_myProfile : ImageView = view.findViewById(R.id.iv_myProfile)
        val linear_myProfile : LinearLayout = view.findViewById(R.id.linear_myProfile)
        val loading : ConstraintLayout = view.findViewById(R.id.constraint_loading)
        val uid : String = FirebaseAuth.getInstance().currentUser!!.uid

        rv_friends.addItemDecoration(DividerItemDecoration(inflater.context, 1))
        val manager = LinearLayoutManager(inflater.context)

        rv_friends.layoutManager = manager
        rv_friends.adapter = adapter

        FirebaseDatabase.getInstance().reference.child("users").orderByChild("/name").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                adapter.userModels.clear()

                for(item in dataSnapshot.children){
                    if(uid != item.key.toString()){
                        val model: UserModel = item.getValue<UserModel>(UserModel::class.java)!!
                        adapter.userModels.add(model)
                    }
                }
                adapter.notifyDataSetChanged()
            }
        })

        FirebaseDatabase.getInstance().reference.child("users").child(uid).addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val myProfile : UserModel = dataSnapshot.getValue(UserModel::class.java)!!

                tv_myName.text = myProfile.name
                tv_myComment.text = myProfile.comment

                if(dataSnapshot.child("profileImageUrl").exists()){
                    val url = dataSnapshot.child("profileImageUrl").value.toString()
                    val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                    val activity = view.context as Activity

                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        loading.visibility = View.GONE
                        if(!activity.isFinishing)
                            Glide.with(iv_myProfile.context).load(uri).apply(RequestOptions.circleCropTransform()).into(iv_myProfile)

                    }.addOnFailureListener{
                        loading.visibility = View.GONE
                        iv_myProfile.setImageResource(R.drawable.ic_account_circle)
                    }
                }else{
                    loading.visibility = View.GONE
                    iv_myProfile.setImageResource(R.drawable.ic_account_circle)
                }

                //내 프로필 클릭 시 내 프로필 보기
                linear_myProfile.setOnClickListener { v ->
                    val intent: Intent = Intent(v!!.context, ProfileActivity::class.java)
                    intent.putExtra("name", myProfile.name)
                    intent.putExtra("check", MINE_INTENT)

                    if(dataSnapshot.child("profileImageUrl").exists()){
                        val url = dataSnapshot.child("profileImageUrl").getValue().toString()
                        intent.putExtra("profileImageUrl", url)
                    }

                    startActivity(intent)
                }
            }

        })

        return view
    }
}

class FriendFragmentRecyclerViewAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var userModels: ArrayList<UserModel> = ArrayList()

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tv_name : TextView = view.findViewById(R.id.tv_friendName)
        var tv_comment : TextView = view.findViewById(R.id.tv_friendComment)
        var iv_image : ImageView = view.findViewById(R.id.iv_friendProfile)
        val mView : View = view

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return MyViewHolder(view)
    }
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder : MyViewHolder = viewHolder as MyViewHolder

        //친구 이름
        holder.tv_name.text = userModels[position].name.toString()

        if(userModels[position].comment != null)
            holder.tv_comment.text = userModels[position].comment.toString()

        //친구 사진
        if(!userModels[position].profileImageUrl.isNullOrEmpty()){
            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(userModels[position].profileImageUrl!!)

            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(holder.mView.context).load(uri).apply(RequestOptions.circleCropTransform()).into(holder.iv_image)
            }.addOnFailureListener{
                holder.iv_image.setImageResource(R.drawable.ic_account_circle)
            }
        }else{
            holder.iv_image.setImageResource(R.drawable.ic_account_circle)
        }

        //친구 클릭했을 때 ProfileActivity 띄우기
        holder.mView.setOnClickListener { v ->
            val intent: Intent = Intent(v!!.context, ProfileActivity::class.java)

            intent.putExtra("name", holder.tv_name.text.toString())
            intent.putExtra("check", OTHER_INTENT)
            intent.putExtra("friendUid", userModels[position].uid.toString())

            if(!userModels[position].profileImageUrl.isNullOrEmpty())
                intent.putExtra("profileImageUrl", userModels[position].profileImageUrl.toString())

            v.context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int {
        return userModels.size
    }


}