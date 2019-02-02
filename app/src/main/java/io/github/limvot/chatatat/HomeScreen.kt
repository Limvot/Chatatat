package io.github.limvot.chatatat

import android.os.Bundle
import android.os.Environment;
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.ArrayAdapter

import java.io.File;
import java.util.Date;

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.crypto.IncomingRoomKeyRequest;
import org.matrix.androidsdk.crypto.IncomingRoomKeyRequestCancellation;
import org.matrix.androidsdk.crypto.MXCrypto;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
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

class HomeScreen : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val applicationContext = getApplicationContext()
        toast("context is $applicationContext")


        // Homescreen GUI
        var adapter_save: SimpleListAdaptor? = null
        verticalLayout {
            button("Chats")          { onClick { startActivity<ChatsActivity>() } }
            button("Settings")       { onClick { startActivity<SettingsActivity>() } }
            textView("http(s) address of Matrix server")
            val homeserver = editText("https://room409.xyz")
            textView("username")
            val username = editText("miloignis")
            textView("password")
            val password = editText("hunter2")
            listView {
                val listItems = mutableListOf(TextListItem("our event stream!", { toast("hello") }))
                adapter_save = SimpleListAdaptor(ctx, listItems)
                adapter = adapter_save
            }
            button("Login") { onClick { 
                val serverURI = homeserver.text.toString()
                val name = username.text.toString()
                val pass = password.text.toString()
                var hsConfig = HomeServerConnectionConfig.Builder()
                                .withHomeServerUri(Uri.parse(serverURI))
                                .build();
                LoginRestClient(hsConfig).loginWithUser(name,
                                                        pass,
                                                        object: SimpleApiCallback<Credentials>() {
                                                            override public fun onSuccess(p0: Credentials) {
                                                                toast("logged in to $serverURI with username $name and password $pass");
                                                                adapter_save!!.add(TextListItem("Logged in", { toast("gah indeed") }))
                                                                hsConfig.setCredentials(p0)
                                                                var session = MXSession.Builder(
                                                                    hsConfig,
                                                                    MXDataHandler(MXMemoryStore(p0, applicationContext), p0),
                                                                    applicationContext).build()
                                                                session.getDataHandler().addListener(object: MXEventListener() {
                                                                    override public fun onLiveEvent(event: Event, roomState: RoomState) {
                                                                        toast("live event $event $roomState")
                                                                        adapter_save!!.add(TextListItem("live event $event $roomState", { toast("gah indeed") }))
                                                                    }
                                                                    override public fun onInitialSyncComplete(toToken: String) {
                                                                        toast("inital sync complete")
                                                                        adapter_save!!.add(TextListItem("inital sync complete", { toast("gah indeed") }))
                                                                    }
                                                                    override public fun onSyncError(matrixError: MatrixError) {
                                                                        toast("sync error $matrixError")
                                                                        adapter_save!!.add(TextListItem("sync error $matrixError", { toast("gah indeed") }))
                                                                    }
                                                                })
                                                                session.startEventStream(null)
                                                            }
                                                        });
            } }
        }
    }
}

