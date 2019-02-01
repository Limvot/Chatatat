package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity

class ChatsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            listView {
                val listItems = listOf(TextListItem("a test!", { toast("hello") }))
                adapter = SimpleListAdaptor(ctx, listItems)
            }
        }
    }
}
