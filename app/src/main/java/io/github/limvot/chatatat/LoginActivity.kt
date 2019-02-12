package io.github.limvot.chatatat

import android.os.Bundle
import android.os.Environment;
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.ArrayAdapter

import java.io.File;
import java.util.Date;

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick


class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            textView("http(s) address of Matrix server")
            val homeserver = editText("https://matrix.org")
            textView("username")
            val username = editText()
            textView("password")
            val password = editText()
            button("Login") { onClick { 
                val dialog = indeterminateProgressDialog(title="logging in")
                val serverURI = homeserver.text.toString()
                val name = username.text.toString()
                val pass = password.text.toString()
                Matrix.login(serverURI, name, pass, getApplicationContext(), { dialog.dismiss(); startActivity<RoomsActivity>() })
            } }
        }
    }
}

