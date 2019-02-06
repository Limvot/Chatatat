package io.github.limvot.chatatat

import android.os.Bundle
import android.os.Environment
import android.os.Binder
import android.os.Build
import android.app.Activity
import android.app.Service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ArrayAdapter

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.crypto.IncomingRoomKeyRequest;
import org.matrix.androidsdk.crypto.IncomingRoomKeyRequestCancellation;
import org.matrix.androidsdk.crypto.MXCrypto;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.data.metrics.MetricsListener;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.data.store.MXFileStore;
import org.matrix.androidsdk.data.store.MXMemoryStore;
import org.matrix.androidsdk.db.MXLatestChatMessageCache;
import org.matrix.androidsdk.db.MXMediaCache;
import org.matrix.androidsdk.listeners.IMXNetworkEventListener;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.ssl.Fingerprint;
import org.matrix.androidsdk.ssl.UnrecognizedCertificateException;
import org.matrix.androidsdk.util.BingRulesManager;
import org.matrix.androidsdk.util.Log;

public class Matrix : Service() {

    companion object {
        private var session: MXSession? = null
        public fun login(serverURI: String, name: String, pass: String, context: Context, callback: () -> Unit) {
            var synced = false
            var hsConfig = HomeServerConnectionConfig.Builder()
                            .withHomeServerUri(Uri.parse(serverURI))
                            .build();
            LoginRestClient(hsConfig).loginWithUser(name,
                                                    pass,
                                                    object: SimpleApiCallback<Credentials>() {
                                                        override public fun onSuccess(p0: Credentials) {
                                                            hsConfig.setCredentials(p0)
                                                            session = MXSession.Builder(
                                                                hsConfig,
                                                                MXDataHandler(MXMemoryStore(p0, context), p0), context).build()
                                                            session?.getDataHandler()?.addListener(object: MXEventListener() {
                                                                override public fun onInitialSyncComplete(toToken: String) {
                                                                    synced = true
                                                                    callback()
                                                                }
                                                                override public fun onSyncError(matrixError: MatrixError) {
                                                                }
                                                                override public fun onLiveEvent(event: Event, roomState: RoomState) {
                                                                    if (synced && event.type == Event.EVENT_TYPE_MESSAGE) {
                                                                        val room = Matrix.getRoom(event.roomId)
                                                                        val roomName = room.getRoomDisplayName(context)
                                                                        val senderName = room.getMember(event.sender)?.name
                                                                        val message = event.content.getAsJsonObject().get("body").getAsString()
                                                                        Matrix.sendMessageNotification(context, roomName, "$senderName: $message", event.roomId)
                                                                    }
                                                                }
                                                            })
                                                            context.startForegroundService(Intent(context, Matrix::class.java))
                                                        }
                                                    });
        }
        public fun getRoomsWithSummaries(): Collection<Pair<RoomSummary, Room>> {
            return session?.getDataHandler()?.getSummaries(false)?.map { Pair(it, session?.getDataHandler()?.getRoom(it.roomId)!!) }?.filterNotNull() ?: listOf()
        }
        public fun getRoom(id: String): Room {
            return session?.getDataHandler()?.getRoom(id)!!
        }
        val eventChannelID = "EVENT_CHANNEL"
        val messageChannelID = "MESSAGE_CHANNEL"
        public fun setupNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel(eventChannelID) == null) {
                    /*val notificationChannel = NotificationChannel(channelID, "listening for events", NotificationManager.IMPORTANCE_MIN)*/
                    val notificationChannel = NotificationChannel(eventChannelID, "listening for events", NotificationManager.IMPORTANCE_DEFAULT)
                    notificationChannel.description = "listening for events"
                    notificationChannel.setSound(null, null)
                    notificationChannel.setShowBadge(false)
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                if (notificationManager.getNotificationChannel(messageChannelID) == null) {
                    val notificationChannel = NotificationChannel(messageChannelID,
                                                          "giving you messages",
                                                          NotificationManager.IMPORTANCE_DEFAULT)
                    notificationChannel.description = "giving you info"
                    /*notificationChannel.setSound(null, null)*/
                    /*notificationChannel.setShowBadge(false)*/
                    notificationManager.createNotificationChannel(notificationChannel)
                }
            }
        }
        var incrementingID = 1337
        val ROOM_ID = "com.github.limvot.Chatatat:roomID"
        public fun sendMessageNotification(context: Context, title: String, text: String, roomId: String) {
            Matrix.setupNotificationChannels(context)
            val bigTextStyle = NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(title)
            bigTextStyle.bigText(text)
            val intentME = Intent(context, ChatActivity::class.java)
            intentME.putExtra(ROOM_ID, roomId)
            val messageNotification = NotificationCompat.Builder(context, Matrix.messageChannelID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setStyle(bigTextStyle)
                .setContentIntent(PendingIntent.getActivity(context, 0, intentME, Intent.FLAG_ACTIVITY_NEW_TASK))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(incrementingID, messageNotification)
            incrementingID += 1
        }
    }

    override public fun onCreate() {
        super.onCreate()

    }

    override public fun onStartCommand(intent: Intent, flags: Int, startID: Int): Int {
        if (intent != null) {
            Matrix.setupNotificationChannels(this)
            val notification = NotificationCompat.Builder(this, Matrix.eventChannelID)
                .setTicker("ticker")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Title!")
                .setContentText("content")
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, RoomsActivity::class.java), 0))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            startForeground(1, notification)
            Matrix.session?.startEventStream(null)

        }
        return super.onStartCommand(intent, flags, startID)
    }
    override public fun onDestroy() {
    }
    override public fun onBind(intent: Intent): Binder? {
        return null
    }
}
