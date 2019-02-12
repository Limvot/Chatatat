package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity

import android.widget.ArrayAdapter
import android.content.Context
import android.view.View
import android.view.ViewGroup

class RoomsActivity : Activity() {
    val items = mutableListOf<Triple<String, String, String>>()
    var roomsAdapter: ArrayAdapter<Triple<String, String, String>>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Matrix.loggedIn) {
            startActivity<LoginActivity>()
            return
        }

        setTitle("Rooms:")

        verticalLayout {
            listView {
                roomsAdapter = object: ArrayAdapter<Triple<String, String, String>>(ctx, 0, items) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
                        val (name, lastMessage, roomId) = getItem(position)
                        return with(ctx) {
                            linearLayout {
                                textView("$name: ${if (lastMessage.length > 100) { lastMessage.substring(0,97) + "..." } else { lastMessage }}").lparams() { padding = dip(10) }
                                onClick { startActivity<ChatActivity>(Matrix.ROOM_ID to roomId) }
                            }
                        }
                    }
                }
                adapter = roomsAdapter
            }
        }
    }
    override fun onResume() {
        super.onResume()
        Matrix.roomsActivityUpdate = {
            items.clear()
            items.addAll(Matrix.getRoomsWithSummaries().sortedBy { (summary1,_) -> summary1.latestReceivedEvent.eventId }
                                                                  .reversed()
                                                                  .map { (summary, room) -> Triple(room.getRoomDisplayName(getApplicationContext()),
                                                                                                   summary?.latestReceivedEvent?.content?.getAsJsonObject()?.get("body")?.getAsString() ?: "",
                                                                                                   room.roomId) })
            roomsAdapter?.notifyDataSetChanged()
        }
        Matrix.roomsActivityUpdate?.invoke()
    }
    override fun onPause() {
        super.onPause()
        Matrix.roomsActivityUpdate = null
    }
}
