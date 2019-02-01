package io.github.limvot.chatatat

import android.os.Bundle
import android.os.Environment;
import android.app.Activity
import android.content.Intent
import android.net.Uri

import java.io.File;
import java.util.Date;

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

class HomeScreen : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Homescreen GUI
        verticalLayout {
            button("Chats")          { onClick { startActivity<ChatsActivity>() } }
            button("Settings")       { onClick { startActivity<SettingsActivity>() } }
        }
    }
}

