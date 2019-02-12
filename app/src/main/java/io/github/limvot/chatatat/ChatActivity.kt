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
        setTitle("${room.getRoomDisplayName(getApplicationContext())}${ if (room.topic != null) { ": ${room.topic}" } else { ""} }")
        verticalLayout {
            /*val membersText = textView("members:")*/
            /*room.getMembersAsync(CallbackWrapper({ membersText.setText("members: ${it.map { it.name }}") }))*/

            messagesList = listView {
                dividerHeight = 0
                divider = null
                messages_adapter = object: ArrayAdapter<Event>(ctx, 0, messages) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
                        val event = getItem(position)
                        val fromSelf = event.sender == Matrix.ourId
                        val name = room.getMember(event.sender)?.name
                        val time = event.formattedOriginServerTs()
                        val prevTimeSame   = position > 0              && getItem(position-1).formattedOriginServerTs() == time
                        val prevSenderSame = position > 0              && room.getMember(getItem(position-1).sender)?.name == name
                        val nextSenderSame = position + 1 < getCount() && room.getMember(getItem(position+1).sender)?.name == name
                        return with(ctx) {
                            relativeLayout {
                                val nameView = if (!prevSenderSame) {
                                    textView(name) {
                                        id = 1337
                                    }.lparams() {
                                        if (fromSelf) {
                                            alignParentRight()
                                        } else {
                                            alignParentLeft()
                                        }
                                    }
                                } else {
                                    null
                                }

                                val timeView = if (!prevTimeSame) {
                                    textView(time) {
                                        id = 1338
                                        textSize = 10f
                                    }.lparams() {
                                        if (nameView != null) {
                                            if (fromSelf) {
                                                leftOf(nameView)
                                                rightMargin = dip(10)
                                            } else {
                                                rightOf(nameView)
                                                leftMargin = dip(10)
                                            }
                                            padding = dip(10)
                                        }
                                    }
                                } else {
                                    null
                                }

                                relativeLayout {
                                    background = GradientDrawable().apply {
                                        shape = GradientDrawable.RECTANGLE
                                        setColor(Color.DKGRAY)
                                        cornerRadii = if (fromSelf) {
                                            floatArrayOf(15f, 15f, 5f, 5f, 15f, 15f, 15f, 15f)
                                        } else {
                                            floatArrayOf(5f, 5f, 15f, 15f, 15f, 15f, 15f, 15f)
                                        }
                                    }

                                    textView(event.content?.getAsJsonObject()?.get("body")?.getAsString()) {
                                        textSize = 18f
                                    }.lparams() {
                                        padding = dip(10)
                                        elevation = 2f
                                    }
                                }.lparams() {
                                    if (nameView != null) {
                                        below(nameView)
                                    } else if (timeView != null) {
                                        below(timeView)
                                    }
                                    elevation = 2f
                                    if (fromSelf) {
                                        alignParentRight()
                                    } else {
                                        alignParentLeft()
                                    }
                                }

                                if (!prevSenderSame) {
                                    topPadding = dip(10)
                                } else if (!prevTimeSame) {
                                    topPadding = dip(20)
                                }
                                if (!nextSenderSame) {
                                    bottomPadding = dip(10)
                                } else {
                                    bottomPadding = dip(4)
                                }
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
