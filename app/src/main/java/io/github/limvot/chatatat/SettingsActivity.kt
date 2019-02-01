package io.github.limvot.chatatat

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

import android.os.Bundle
import android.app.Activity

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {

            textView("http(s) address of Matrix server")
            val url_entry = editText("I wish this worked")

            button("Save") { onClick {
                toast("I wish this saved")
            } }
        }
    }
}
