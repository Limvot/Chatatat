package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity
import android.widget.AbsListView

import org.matrix.androidsdk.data.RoomMediaMessage;
import org.matrix.androidsdk.data.timeline.EventTimeline;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;

class ChatActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var messages: MutableList<TextListItem> = mutableListOf()
        var messages_adapter: SimpleListAdaptor? = null

        var flag_loading = false;

        verticalLayout {
            textView("${Matrix.room?.getRoomDisplayName(getApplicationContext())}: ${Matrix.room?.topic ?: "no topic"}")
            val membersText = textView("members:")
            Matrix.room?.getMembersAsync(CallbackWrapper({ membersText.setText("members: ${it.map { it.name }}") }))

            textView("type message:")
            val message = editText("")
            button("Send") { onClick {
                val text = message.text.toString()
                Matrix.room?.sendTextMessage(text, text, "markdown", object: RoomMediaMessage.EventCreationListener {
                    override fun onEventCreated(message: RoomMediaMessage) { /*toast("event created $message")*/ }
                    override fun onEventCreationFailed(message: RoomMediaMessage, error: String) { toast("event creation failed $message : $error") }
                    override fun onEncryptionFailed(message: RoomMediaMessage) { toast("encryption failed failed $message") }
                })
            } }
            var messagesList: AbsListView? = null
            messagesList = listView {
                messages_adapter = SimpleListAdaptor(ctx, messages)
                adapter = messages_adapter
                setOnScrollListener(object: AbsListView.OnScrollListener {
                    override public fun onScrollStateChanged(view: AbsListView, scrollState: Int) { }
                    override public fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                        if (firstVisibleItem == 0 && flag_loading == false) {
                            Matrix.room?.timeline?.backPaginate(object: SimpleApiCallback<Int>() {
                                override public fun onSuccess(p0: Int) {
                                    flag_loading = false
                                    messagesList?.smoothScrollBy(10, 0)
                                }
                            })
                        }
                    }
                })
            }

            Matrix.room?.timeline?.addEventTimelineListener({ event, direction, roomState ->
                if (direction == EventTimeline.Direction.FORWARDS) {
                    messages_adapter!!.add(TextListItem("${Matrix.room?.getMember(event.sender)?.name}: ${event.content.getAsJsonObject().get("body").getAsString()}", { toast("$event") }))
                } else {
                    messages_adapter!!.insert(TextListItem("${Matrix.room?.getMember(event.sender)?.name}: ${event.content.getAsJsonObject().get("body").getAsString()}", { toast("$event") }), 0)
                }
            })
            Matrix.room?.timeline?.initHistory()
            var pageinateCallback: SimpleApiCallback<Int>? = null
            pageinateCallback = object: SimpleApiCallback<Int>() {
                override public fun onSuccess(p0: Int) {
                    if (messages.size < 50 && p0 > 0) {
                        Matrix.room?.timeline?.backPaginate(pageinateCallback)
                    }
                }
            }
            Matrix.room?.timeline?.backPaginate(pageinateCallback)
        }
    }
}
