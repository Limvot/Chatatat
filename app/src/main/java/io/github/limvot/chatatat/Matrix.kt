
import android.os.Bundle
import android.os.Environment;
import android.app.Activity
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

object Matrix {
    private var session: MXSession? = null
    public var room: Room? = null
    public var liveEventListener: ((Event, RoomState) -> Unit)? = null
    public fun login(serverURI: String, name: String, pass: String, context: Context, callback: () -> Unit) {
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
                                                                callback()
                                                            }
                                                            override public fun onSyncError(matrixError: MatrixError) {
                                                            }
                                                            override public fun onLiveEvent(event: Event, roomState: RoomState) {
                                                                liveEventListener?.invoke(event, roomState)
                                                            }
                                                        })
                                                        session?.startEventStream(null)
                                                    }
                                                });
    }
    public fun getRooms(): Collection<Room> {
        return session?.getDataHandler()?.getSummaries(false)?.map { session?.getDataHandler()?.getRoom(it.roomId) }?.filterNotNull() ?: listOf()
    }
}

