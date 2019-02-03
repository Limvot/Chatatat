package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity

import org.matrix.androidsdk.data.RoomMediaMessage;
import org.matrix.androidsdk.data.timeline.EventTimeline;

class ChatActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var messages_adapter: SimpleListAdaptor? = null

        verticalLayout {
            textView("${Matrix.room?.getRoomDisplayName(getApplicationContext())}: ${Matrix.room?.topic ?: "no topic"}")
            val membersText = textView("members:")
            Matrix.room?.getMembersAsync(CallbackWrapper({ membersText.setText("members: ${it.map { it.name }}") }))

            textView("type message:")
            val message = editText("")
            button("Send") { onClick {
                val text = message.text.toString()
                Matrix.room?.sendTextMessage(text, text, "markdown", object: RoomMediaMessage.EventCreationListener {
                    override fun onEventCreated(message: RoomMediaMessage) {
                        /*toast("event created $message")*/
                    }
                    override fun onEventCreationFailed(message: RoomMediaMessage, error: String) {
                        toast("event creation failed $message : $error")
                    }
                    override fun onEncryptionFailed(message: RoomMediaMessage) {
                        toast("encryption failed failed $message")
                    }
                })
            } }
            listView {
                messages_adapter = SimpleListAdaptor(ctx, mutableListOf())
                adapter = messages_adapter
            }

            Matrix.room?.timeline?.addEventTimelineListener({ event, direction, roomState ->
                if (direction == EventTimeline.Direction.FORWARDS) {
                    messages_adapter!!.add(TextListItem("${event.sender} ${event.content.getAsJsonObject().get("body")}", { toast("gah indeed") }))
                } else {
                    messages_adapter!!.insert(TextListItem("${event.sender} ${event.content.getAsJsonObject().get("body")}", { toast("gah indeed") }), 0)
                }
            })
            Matrix.room?.timeline?.backPaginate(10, null)
        }
    }
}
