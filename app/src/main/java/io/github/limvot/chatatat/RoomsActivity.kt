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
                val listItems = Matrix.getRooms().map { it -> TextListItem("${it.getRoomDisplayName(getApplicationContext())} ${it.topic ?: ""}",
                                                                           { Matrix.room = it; startActivity<ChatActivity>() }) }
                adapter = SimpleListAdaptor(ctx, listItems)
            }
        }
    }
}
