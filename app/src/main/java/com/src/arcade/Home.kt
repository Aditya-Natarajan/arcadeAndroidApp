package com.src.arcade

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Home : AppCompatActivity() {

    private lateinit var btnWordle:Button
    private lateinit var btnWordRank:Button
    private lateinit var btnConnectFour : Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        btnWordle = findViewById(R.id.btnWordle)
        btnWordRank = findViewById(R.id.btnWordRank)
        btnConnectFour = findViewById(R.id.btnConnectFour)

        btnWordle.setOnClickListener {
            startActivity(Intent(this, Wordle::class.java))
        }
        btnWordRank.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnConnectFour.setOnClickListener {
            startActivity(Intent(this,ConnectFour::class.java))
        }
    }
}