package com.kapp.call_app.notifications

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.PowerManager
import android.telecom.Call
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.kapp.call_app.R
import com.kapp.call_app.contacts.CallContactAvatarHelper
import com.kapp.call_app.contacts.getCallContact
import com.kapp.call_app.external_call.DialerActivity
import com.kapp.call_app.external_call.ExternalCallActivity
import com.kapp.call_app.telecom.CallManager



class GSMCallNotificationManager(private val context: Context) {

    companion object {
        const val INTENT_NOTIF_ID = "NOTIFICATION_ID"
        const val INTENT_HANGUP_CALL_NOTIF_ACTION = "com.kapp.call_app.HANGUP_CALL_ACTION"
        const val INTENT_ANSWER_CALL_NOTIF_ACTION = "com.kapp.call_app.ANSWER_CALL_ACTION"

        private const val CALL_NOTIF_ID = 1
        private const val MISSED_CALLS_NOTIF_ID = 2
        const val CHANNEL_ID_HIGH_PRIORITY = "kapp_gsm_call_high_priority"
        const val CHANNEL_ID_DEFAULT_PRIORITY = "kapp_gsm_call"
        const val DEFAULT_NOTIF_CHANNEL_ID = "kapp_gsm_notification"
        const val CALL_NOTIF_CHANNEL_HIGH = "call_notification_channel_high_priority"
        const val CALL_NOTIF_CHANNEL_DEFAULT = "call_notification_channel"
        const val NOTIF_CHANNEL_DEFAULT = "call_notification_channel"
    }

    private val TAG: String = GSMCallNotificationManager::class.java.name

    private val ACCEPT_CALL_CODE = 0
    private val DECLINE_CALL_CODE = 1
    private val notificationManager : NotificationManager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val callContactAvatarHelper = CallContactAvatarHelper(context)
    private var currentCall : Call? = null


    @SuppressLint("NewApi")
    fun setupNotification() {
        Log.i(TAG,"Setup notification called")
        getCallContact(context.applicationContext, CallManager.getPrimaryCall()) { callContact ->
            currentCall = CallManager.getPrimaryCall()
            val callContactAvatar = callContactAvatarHelper.getCallContactAvatar(callContact)
            val callState = CallManager.getState()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isHighPriority = powerManager.isInteractive && callState == Call.STATE_RINGING
            Log.i(TAG,"isHighPriority : $isHighPriority")
            val channelId = if (isHighPriority) CHANNEL_ID_HIGH_PRIORITY  else CHANNEL_ID_DEFAULT_PRIORITY
            val importance = if (isHighPriority) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val name = if (isHighPriority) CALL_NOTIF_CHANNEL_HIGH else CALL_NOTIF_CHANNEL_DEFAULT

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            val notificationChannel = NotificationChannel(channelId, name, importance).apply {
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                setSound(ringtoneUri, audioAttributes)
                vibrationPattern = longArrayOf(0L, 1000L, 500L, 1000L)
            }
            notificationManager.createNotificationChannel(notificationChannel)

            val openAppIntent = ExternalCallActivity.getStartIntent(context)
            val openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_MUTABLE)

            val acceptCallIntent = Intent(context, GSMNotificationBroadcastReceiver::class.java)
            acceptCallIntent.action = INTENT_ANSWER_CALL_NOTIF_ACTION
            acceptCallIntent.putExtra(INTENT_NOTIF_ID, CALL_NOTIF_ID)
            val acceptPendingIntent =
                PendingIntent.getBroadcast(context, ACCEPT_CALL_CODE, acceptCallIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)

            val declineCallIntent = Intent(context, GSMNotificationBroadcastReceiver::class.java)
            declineCallIntent.action = INTENT_HANGUP_CALL_NOTIF_ACTION
            declineCallIntent.putExtra(INTENT_NOTIF_ID, CALL_NOTIF_ID)
            val declinePendingIntent =
                PendingIntent.getBroadcast(context, DECLINE_CALL_CODE, declineCallIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)

            var callerName = callContact.name.ifEmpty { context.getString(R.string.unknown_caller) }
            if (callContact.numberLabel.isNotEmpty()) {
                callerName += " - ${callContact.numberLabel}"
            }

            val contentTextId = when (callState) {
                Call.STATE_RINGING -> R.string.is_calling
                Call.STATE_DIALING -> R.string.dialing
                Call.STATE_DISCONNECTED -> R.string.call_ended
                Call.STATE_DISCONNECTING -> R.string.call_ending
                else -> R.string.ongoing_call
            }

            val notificationLayoutHeadsUp = RemoteViews(context.packageName, R.layout.external_call_incoming_notification_heads_up)
            notificationLayoutHeadsUp.setTextViewText(R.id.external_caller, callContact.name)
            notificationLayoutHeadsUp.setTextViewText(R.id.external_phone_number, callContact.number)
            notificationLayoutHeadsUp.setTextViewText(R.id.external_incoming_call_info, context.getString(contentTextId))


            if (callContactAvatar != null) {
                notificationLayoutHeadsUp.setImageViewBitmap(R.id.external_caller_picture, callContactAvatar)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .addPerson(Person.Builder().setName(callContact.name).build())
                .setSmallIcon(R.drawable.icon_call_answer)
                .setContentTitle(callerName)
                .setContentText(context.getString(contentTextId))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setShowWhen(true)
                .setOngoing(true)
                .setVibrate(longArrayOf(1L, 2L, 3L))
                .setSound(ringtoneUri)
                .setColor(ContextCompat.getColor(context, R.color.primary_color))
                .setFullScreenIntent(openAppPendingIntent, true)
                .addAction(NotificationCompat.Action.Builder(
                    R.drawable.call_hangup,
                    context.getString(R.string.call_context_action_hangup),
                    declinePendingIntent
                ).build())
                .addAction(NotificationCompat.Action.Builder(
                    R.drawable.call_hangup,
                    context.getString(R.string.call_context_action_answer),
                    acceptPendingIntent
                ).build())
                .setCustomHeadsUpContentView(notificationLayoutHeadsUp)
                .setContentIntent(openAppPendingIntent)

            val notification = builder.build()
            notificationManager.notify(CALL_NOTIF_ID, notification)

            //service.startForeground(CALL_NOTIF_ID, notification)
        }
    }

    fun displayMissedCallNotification(call: Call){
        Log.i(TAG,"Setup missed call notification")
        getCallContact(context.applicationContext, call) { callContact ->
            val callContactAvatar = callContactAvatarHelper.getCallContactAvatar(callContact)
            val channelId = DEFAULT_NOTIF_CHANNEL_ID
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val name = NOTIF_CHANNEL_DEFAULT

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            val notificationChannel = NotificationChannel(channelId, name, importance).apply {
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                setSound(ringtoneUri, audioAttributes)
                vibrationPattern = longArrayOf(0L, 1000L, 500L, 1000L)
            }
            notificationManager.createNotificationChannel(notificationChannel)

            val openAppIntent = DialerActivity.getStartIntent(context)
            val openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_MUTABLE)

            var callerName = callContact.name.ifEmpty { context.getString(R.string.unknown_caller) }
            if (callContact.numberLabel.isNotEmpty()) {
                callerName += " - ${callContact.numberLabel}"
            }

            val contentTextId = R.string.call_missed

            val notificationLayoutHeadsUp = RemoteViews(context.packageName, R.layout.external_call_incoming_notification_heads_up)
            notificationLayoutHeadsUp.setTextViewText(R.id.external_caller, callContact.name)
            notificationLayoutHeadsUp.setTextViewText(R.id.external_phone_number, callContact.number)
            notificationLayoutHeadsUp.setTextViewText(R.id.external_incoming_call_info, context.getString(contentTextId))


            if (callContactAvatar != null) {
                notificationLayoutHeadsUp.setImageViewBitmap(R.id.external_caller_picture, callContactAvatar)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .addPerson(Person.Builder().setName(callContact.name).build())
                .setSmallIcon(R.drawable.topbar_missed_call_notification)
                .setContentTitle(callerName)
                .setContentText(context.getString(contentTextId))
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setWhen(call.details.creationTimeMillis)
                .setAutoCancel(false)
                .setShowWhen(true)
                .setOngoing(false)
                .setVibrate(longArrayOf(1L, 2L, 3L))
                .setSound(ringtoneUri)
                .setColor(ContextCompat.getColor(context, R.color.primary_color))
                .setCustomHeadsUpContentView(notificationLayoutHeadsUp)
                .setContentIntent(openAppPendingIntent)

            val notification = builder.build()
            notificationManager.notify(MISSED_CALLS_NOTIF_ID, notification)
        }
    }

    fun cancelNotification() {
        Log.i(TAG,"Notification cancelled")
        notificationManager.cancel(CALL_NOTIF_ID)
    }
}
