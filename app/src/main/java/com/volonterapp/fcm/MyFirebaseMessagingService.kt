package com.volonterapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.volonterapp.R
import com.volonterapp.activities.MainActivity
import com.volonterapp.activities.SignInActivity
import com.volonterapp.firebase.FirestoreClass
import com.volonterapp.utils.Constants


class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Poruka od: ${remoteMessage.from}")

        when (remoteMessage.data.isNotEmpty()) {
            true -> {
                Log.i(TAG, "Sadržaj poruke: ${remoteMessage.data}")

                remoteMessage.data[Constants.FCM_KEY_TITLE]?.let { notificationTitle ->
                    remoteMessage.data[Constants.FCM_KEY_MESSAGE]?.let { notificationMessage ->
                        sendNotification(notificationTitle, notificationMessage)
                    }
                }
            }
        }

        remoteMessage.notification?.body?.also { notificationBody ->
            Log.d(TAG, "Tijelo notifikacije: $notificationBody")
        }
    }



    override fun onNewToken(token: String) {
        Log.e(TAG, "Refreshed token: $token")


        sendRegistrationToServer(token)
    }


    private fun sendRegistrationToServer(token: String?) {
        val sharedPreferences =
            this.getSharedPreferences(Constants.VOLONTERAPP_PREFERENCES, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(Constants.FCM_TOKEN, token)
        editor.apply()
    }

    private fun sendNotification(notificationTitle: String, notificationMessage: String) {
        val targetIntent = when (FirestoreClass().getCurrentUserID().isEmpty()) {
            true -> Intent(this, SignInActivity::class.java)
            false -> Intent(this, MainActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingAction = PendingIntent.getActivity(this, 0, targetIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        val channelIdentifier = resources.getString(R.string.default_notification_channel_id)
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        NotificationCompat.Builder(this, channelIdentifier).apply {
            setSmallIcon(R.drawable.ic_stat_ic_notification)
            setContentTitle(notificationTitle)
            setContentText(notificationMessage)
            setAutoCancel(true)
            setSound(notificationSound)
            setContentIntent(pendingAction)
        }.build().also { notification ->
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(0, notification)
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}