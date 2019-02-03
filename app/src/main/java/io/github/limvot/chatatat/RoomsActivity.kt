package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity

class RoomsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            textView("Rooms that you are in:")
            listView {
                val listItems = Matrix.getRoomsWithSummaries().sortedBy { (summary1,_) -> summary1.latestReceivedEvent.eventId }
                                                              .reversed()
                                                              .map { (summary, room) -> TextListItem("${room.getRoomDisplayName(getApplicationContext())}: ${summary?.latestReceivedEvent?.content?.getAsJsonObject()?.get("body")?.getAsString()}",
                                                                                                     { Matrix.room = room; startActivity<ChatActivity>() }) }
                adapter = SimpleListAdaptor(ctx, listItems)
            }
        }
    }
}
