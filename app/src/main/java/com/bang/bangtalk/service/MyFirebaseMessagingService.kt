package com.bang.bangtalk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bang.bangtalk.MainActivity
import com.bang.bangtalk.R
import com.bang.bangtalk.model.NotificationModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.FirebaseStorage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"].toString()
            val body = remoteMessage.data["body"].toString()
            val sender = remoteMessage.data["sender"].toString()
            val receiver = remoteMessage.data["receiver"].toString()
            val roomId = remoteMessage.data["roomId"].toString()
            sendNotification(title, body, sender, receiver, roomId)
        }

    }

    private fun sendNotification(title : String, body: String, sender: String, receiver: String, roomId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        FirebaseDatabase.getInstance().reference.child("users").child(sender).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.child("profileImageUrl").exists()){
                    val url = dataSnapshot.child("profileImageUrl").value.toString()
                    val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)

                    val ONE_MEGABYTE: Long = 10240 * 10240
                    storageReference.getBytes(ONE_MEGABYTE).addOnCompleteListener{
                        val bitmap = BitmapFactory.decodeByteArray(it.result, 0, it.result!!.size)

                        setNotification(title, body, pendingIntent, bitmap, receiver, roomId)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val d = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        resources.getDrawable(R.drawable.ic_account_circle, null)
                    else
                        resources.getDrawable(R.drawable.ic_account_circle)
                    val bitmap = drawableToBitmap(d)
                    setNotification(title, body, pendingIntent, bitmap, receiver, roomId)
                }
            }
        })
    }

    private fun getSenderProfileCircle(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//        canvas.drawRoundRect(rectF, 500F, 500F, paint)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        bitmap.recycle()

        return output
    }

    private fun setNotification(title: String, body: String, pendingIntent: PendingIntent, bitmap: Bitmap, receiver: String, roomId: String){
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        FirebaseDatabase.getInstance().reference.child("chatRooms").child(roomId).child("roomName").addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(getSenderProfileCircle(bitmap))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setStyle(NotificationCompat.BigTextStyle().setSummaryText(dataSnapshot.child(receiver).value.toString()))
                    .build()

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Since android Oreo notification channel is needed.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_HIGH)
                    //channel.lightColor = Color.BLUE
                    channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                    notificationManager.createNotificationChannel(channel)
                }

                notificationManager.notify(1 /* ID of notification */, notificationBuilder)
            }

        })


    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

}
