package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity

class ChatActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            textView("${Matrix.room?.getRoomDisplayName(getApplicationContext())}: ${Matrix.room?.topic ?: "no topic"}")
            /*listView {*/
                /*val listItems = Matrix.getRooms().map { it -> TextListItem("${it.getRoomDisplayName(getApplicationContext())} ${it.topic ?: ""}",*/
                                                                           /*{ Matrix.room = it; startActivity<ChatActivity>() }) }*/
                /*adapter = SimpleListAdaptor(ctx, listItems)*/
            /*}*/
        }
    }
}
