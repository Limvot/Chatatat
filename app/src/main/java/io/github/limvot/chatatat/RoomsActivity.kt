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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Matrix.loggedIn) {
            startActivity<LoginActivity>()
            return
        }

        verticalLayout {
            textView("Rooms that you are in:")
            listView {
                val listItems = Matrix.getRoomsWithSummaries().sortedBy { (summary1,_) -> summary1.latestReceivedEvent.eventId }
                                                              .reversed()
                                                              .map { (summary, room) -> Pair("${room.getRoomDisplayName(getApplicationContext())}: ${summary?.latestReceivedEvent?.content?.getAsJsonObject()?.get("body")?.getAsString()}",
                                                                                                     { startActivity<ChatActivity>(Matrix.ROOM_ID to room.roomId) }) }
                adapter = object: ArrayAdapter<Pair<String, () -> Unit>>(ctx, 0, listItems) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
                        val (text, f) = getItem(position)
                        return with(ctx) {
                            linearLayout {
                                textView(text) { onClick { f() } }.lparams() { padding = dip(10) }
                            }
                        }
                    }
                }
            }
        }
    }
}
