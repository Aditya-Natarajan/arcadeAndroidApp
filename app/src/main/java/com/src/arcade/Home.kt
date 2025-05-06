package com.src.arcade

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Home : AppCompatActivity() {

    private lateinit var btnWordle:Button
    private lateinit var btnLexipass:Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        btnWordle = findViewById(R.id.btnWordle)
        btnLexipass = findViewById(R.id.btnLexipass)

        btnWordle.setOnClickListener {
            startActivity(Intent(this, Wordle::class.java))
        }
        btnLexipass.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}