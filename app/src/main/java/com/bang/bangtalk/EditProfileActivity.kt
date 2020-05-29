package com.bang.bangtalk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bang.bangtalk.adapter.EditProfileActivityRecyclerViewAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_edit_profile.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class EditProfileActivity : AppCompatActivity() {

    private val PICK_FROM_ALBUM = 10
    private val REQUEST_IMAGE_CAPTURE = 11
    private var imageUri: Uri? = null

    private val PERMISSION_REQUEST_EXTERNAL_STORAGE = 100
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val adapter = EditProfileActivityRecyclerViewAdapter(this)
        val rv : RecyclerView = findViewById(R.id.rv_profile)

        setProfileImage()

        rv.addItemDecoration(DividerItemDecoration(applicationContext, 1))
        rv.layoutManager = LinearLayoutManager(applicationContext)
        rv.adapter = adapter
    }

    fun onClick(view: View){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        when(view.id){
            R.id.iv_profile -> {
                val builder1 = AlertDialog.Builder(this)
                    .setItems(R.array.setImageNoUri, DialogInterface.OnClickListener { dialog, which ->
                        when(which){
                            0 -> {
                                dispatchTakePicture()
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
                                dispatchTakePicture()
                            }
                            1 -> {
                                openAlbum()
                            }
                            2 -> {
                                constraint_loading.visibility = View.VISIBLE
                                FirebaseStorage.getInstance().reference.child("userImages").child(uid).delete().addOnSuccessListener {
                                    iv_profile.setImageResource(R.drawable.ic_account_circle)

                                    FirebaseDatabase.getInstance().reference.child("users").child(uid).child("profileImageUrl").removeValue().addOnSuccessListener {
                                        constraint_loading.visibility = View.GONE
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(this, "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                    constraint_loading.visibility = View.GONE
                                }.addOnCanceledListener {
                                    constraint_loading.visibility = View.GONE
                                }
                            }
                        }
                    })

                val drawableProfile = iv_profile.drawable
                val drawableBasic = resources.getDrawable(R.drawable.ic_account_circle)

                if(castToBitmap(drawableProfile).sameAs(castToBitmap(drawableBasic)))
                    builder1.show()
                else
                    builder2.show()
            }
            R.id.ib_back -> {
                finish()
            }
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseStorage.getInstance().reference.child("userImages").child(uid)
        val map = HashMap<String, Any>()
        map["profileImageUrl"] = ref.toString()

        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                PICK_FROM_ALBUM -> {
                    imageUri = data!!.data                //이미지 경로 원본
                    ref.putFile(imageUri!!).addOnSuccessListener {
                        FirebaseDatabase.getInstance().reference.child("users").child(uid).updateChildren(map).addOnSuccessListener {
                            setProfileImage()
                            constraint_loading.visibility = View.GONE
                        }.addOnFailureListener {
                            Toast.makeText(this, "잠시후에 다시 해주세요.", Toast.LENGTH_SHORT).show()
                            constraint_loading.visibility = View.GONE
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "잠시후에 다시 해주세요.", Toast.LENGTH_SHORT).show()
                        constraint_loading.visibility = View.GONE
                    }
                }

                REQUEST_IMAGE_CAPTURE -> {
//                    val bitmap = data!!.extras.get("data") as Bitmap
//                    val matrix = Matrix()
//                    matrix.postRotate(90F)
//                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//                    val baos = ByteArrayOutputStream()
//                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//                    val data1 = baos.toByteArray()
                    val uploadTask = ref.putFile(photoUri!!)

                    uploadTask.addOnSuccessListener {
                        FirebaseDatabase.getInstance().reference.child("users").child(uid).updateChildren(map).addOnSuccessListener {
                            setProfileImage()
                            constraint_loading.visibility = View.GONE
                        }.addOnFailureListener {
                            Toast.makeText(this, "잠시후에 다시 해주세요.", Toast.LENGTH_SHORT).show()
                            constraint_loading.visibility = View.GONE
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "잠시후에 다시 해주세요.", Toast.LENGTH_SHORT).show()
                        constraint_loading.visibility = View.GONE
                    }
                }
            }
        }
        else
            constraint_loading.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST_EXTERNAL_STORAGE -> {
                if(grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "허용해야 해당 기능을 실행할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    constraint_loading.visibility = View.GONE
                }
                else if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openAlbum()
            }
        }
    }

    fun openAlbum(){
        val intent = Intent()
        constraint_loading.visibility = View.VISIBLE
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            intent.action = Intent.ACTION_PICK
            intent.type = "image/*"
            startActivityForResult(intent, PICK_FROM_ALBUM)
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_EXTERNAL_STORAGE)
        }
    }

    fun dispatchTakePicture(){
        constraint_loading.visibility = View.VISIBLE

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also{ takePictureIntent ->
                takePictureIntent.resolveActivity(packageManager)?.also{
                    val photoFile: File? = try{
                        createImageFile()
                    }catch(e: IOException){
                        null
                    }

                    photoFile?.also{
                        photoUri = FileProvider.getUriForFile(this, "com.bang.bangtalk.fileprovider", it)

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_REQUEST_EXTERNAL_STORAGE)
        }
    }
    
    @SuppressLint("SimpleDateFormat")
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", dir)
    }

    fun setProfileImage(){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance().reference.child("users").child(uid).addValueEventListener(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.child("profileImageUrl").exists()){
                    val url = dataSnapshot.child("profileImageUrl").value.toString()
                    val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                    val activity = this@EditProfileActivity as Activity

                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        constraint_loading.visibility = View.GONE
                        if(!activity.isFinishing)
                            Glide.with(iv_profile.context).load(uri).apply(RequestOptions.circleCropTransform()).into(iv_profile)

                    }.addOnFailureListener{
                        iv_profile.setImageResource(R.drawable.ic_account_circle)
                        constraint_loading.visibility = View.GONE
                    }
                } else {
                    iv_profile.setImageResource(R.drawable.ic_account_circle)
                    constraint_loading.visibility = View.GONE
                }
            }

        })
    }

    fun castToBitmap(drawable: Drawable) : Bitmap {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0,0,canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}
