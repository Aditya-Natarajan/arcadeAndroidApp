package com.src.arcade

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import io.socket.client.Socket
import org.json.JSONObject

class Login : AppCompatActivity() {
    private lateinit var app: App
    private lateinit var mSocket: Socket

    private lateinit var tbSecure: ToggleButton
    private lateinit var tbLogin: ToggleButton
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    private var url = "https://arcade.pivotpt.in/usersAPI.php"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        app = application as App

        tbSecure = findViewById(R.id.tbSecure)
        tbLogin = findViewById(R.id.tbLogin)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)


        tbSecure.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPassword.visibility = View.VISIBLE
            } else {
                etPassword.visibility = View.INVISIBLE
            }
        }
        tbLogin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                btnLogin.setText("Login")
            } else {
                btnLogin.setText("Sign Up")
            }
        }


        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            var password = etPassword.text.toString().trim()
            var serviceID  = 1
            if(tbLogin.isChecked){
                serviceID = 2
            }
            if(!tbSecure.isChecked){
                password = ""
            }
            val jsonBody = JSONObject().apply {
                put("serviceID", serviceID)
                put("username", username)
                put("password", password)
            }
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    val message = response.optString("message")
                    val code = response.optInt("code")
                    val validUsername = response.optString("username")
                    val userId = response.optInt("user_id")
                    if (code != 0) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        etUsername.text.clear()
                        etPassword.text.clear()
                    } else {
                        app.username = validUsername
                        app.userId = userId
                        etUsername.text.clear()
                        etPassword.text.clear()
                        val intent = Intent(this, Home::class.java)
                        startActivity(intent)
                    }
                },
                { error ->
                    println("Volley error: ${error.message}")
                }
            )
            VolleySingleton.addToRequestQueue(request)
        }
    }


}