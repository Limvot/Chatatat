package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity
import android.widget.AbsListView

import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.content.Context
import android.view.View
import android.view.ViewGroup

import android.graphics.drawable.GradientDrawable
import android.graphics.Color

import org.matrix.androidsdk.data.RoomMediaMessage;
import org.matrix.androidsdk.data.timeline.EventTimeline;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.Event;

class ChatActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentRoomID = getIntent().getStringExtra(Matrix.ROOM_ID)
        Matrix.clearRoomNotification(intentRoomID)
        val room = Matrix.getRoom(intentRoomID)

        var messages: MutableList<Event> = mutableListOf()
        var messages_adapter: ArrayAdapter<Event>? = null

        var flag_loading = false;

        var messagesList: AbsListView? = null
        verticalLayout {
            textView("${room.getRoomDisplayName(getApplicationContext())}: ${room.topic ?: "no topic"}")
            /*val membersText = textView("members:")*/
            /*room.getMembersAsync(CallbackWrapper({ membersText.setText("members: ${it.map { it.name }}") }))*/

            messagesList = listView {
                messages_adapter = object: ArrayAdapter<Event>(ctx, 0, messages) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
                        val event = getItem(position)
                        val fromSelf = event.sender == Matrix.ourId
                        return with(ctx) {
                            relativeLayout {
                                val nameView = textView(room.getMember(event.sender)?.name) {
                                    id = 1337
                                }.lparams() {
                                    if (fromSelf) {
                                        alignParentRight()
                                        /*rightMargin = dip(10)*/
                                    } else {
                                        alignParentLeft()
                                        /*leftMargin = dip(10)*/
                                    }
                                    bottomPadding = dip(4)
                                }
                                textView(event.content.getAsJsonObject().get("body").getAsString()) {
                                    textSize = 18f
                                    background = GradientDrawable().apply {
                                        shape = GradientDrawable.RECTANGLE
                                        setColor(Color.DKGRAY)
                                        cornerRadii = if (fromSelf) {
                                            floatArrayOf(15f, 15f, 5f, 5f, 15f, 15f, 15f, 15f)
                                        } else {
                                            floatArrayOf(5f, 5f, 15f, 15f, 15f, 15f, 15f, 15f)
                                        }
                                    }
                                }.lparams() {
                                    below(nameView)
                                    padding = dip(10)
                                    elevation = 2f
                                    if (fromSelf) {
                                        alignParentRight()
                                    } else {
                                        alignParentLeft()
                                    }
                                }
                                verticalPadding = dip(10)
                                rightPadding = if (fromSelf) { dip(15) } else { dip(60) }
                                leftPadding  = if (fromSelf) { dip(60) } else { dip(15) }
                                clipToPadding = false
                            }
                        }
                    }
                }
                adapter = messages_adapter
                setOnScrollListener(object: AbsListView.OnScrollListener {
                    override public fun onScrollStateChanged(view: AbsListView, scrollState: Int) { }
                    override public fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                        if (firstVisibleItem == 0 && flag_loading == false) {
                            val messages_len = messages.size
                            room.timeline?.backPaginate(object: SimpleApiCallback<Int>() {
                                override public fun onSuccess(p0: Int) {
                                    flag_loading = false
                                    messagesList?.setSelectionFromTop(messages.size - messages_len, 0)
                                }
                            })
                        }
                    }
                })
            }.lparams() {
                weight = 2.0f
            }

            linearLayout {
                val message = editText("") {
                    hint = "Write a message..."
                }.lparams(width = matchParent) {
                    weight = 2.0f
                }
                button("Send") { onClick {
                    val text = message.text.toString()
                    message.text.clear()
                    messagesList?.setSelectionFromTop(messages.size, 0)
                    room.sendTextMessage(text, text, "markdown", object: RoomMediaMessage.EventCreationListener {
                        override fun onEventCreated(message: RoomMediaMessage) { /*toast("event created $message")*/ }
                        override fun onEventCreationFailed(message: RoomMediaMessage, error: String) { toast("event creation failed $message : $error") }
                        override fun onEncryptionFailed(message: RoomMediaMessage) { toast("encryption failed failed $message") }
                    })
                } }
            }
        }

        room.timeline?.addEventTimelineListener({ event, direction, roomState ->
            if (event.type == Event.EVENT_TYPE_MESSAGE) {
                if (direction == EventTimeline.Direction.FORWARDS) {
                    messages_adapter!!.add(event)
                } else {
                    messages_adapter!!.insert(event, 0)
                }
            }
        })
        room.timeline?.initHistory()
        var pageinateCallback: SimpleApiCallback<Int>? = null
        pageinateCallback = object: SimpleApiCallback<Int>() {
            override public fun onSuccess(p0: Int) {
                messagesList?.setSelectionFromTop(messages.size, 0)
                if (messages.size < 50 && p0 > 0) {
                    room.timeline?.backPaginate(pageinateCallback)
                }
            }
        }
        room.timeline?.backPaginate(pageinateCallback)
    }
}
