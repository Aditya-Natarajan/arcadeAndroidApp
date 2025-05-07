package com.src.arcade

import android.app.Application
import android.net.Uri
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import java.net.URISyntaxException

class App : Application() {
    private lateinit var mSocket: Socket
    var username:String = ""
    var userId:Int = -1

    override fun onCreate() {
        super.onCreate()
        VolleySingleton.init(this)
    }
    fun connectSocket(name:String, email:String):Socket {
        try {
            val query = buildQuery(
                mapOf(
                    "name" to name,
                    "email" to email
                )
            )
            val options = IO.Options.builder()
                //.setForceNew(true)
                .setTransports(arrayOf(WebSocket.NAME))
                .setQuery(query)
                .build()
            mSocket = IO.socket("https://arcade.pivotpt.in",options) // Replace with your Socket.IO server URL
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }

        mSocket.connect()
        return mSocket
    }
    fun getSocket(): Socket = mSocket
    fun buildQuery(params: Map<String, String>): String {
        return params.map { (key, value) ->
            "${key}=${Uri.encode(value)}"
        }.joinToString("&")
    }
}


/*
    private fun validateLoginForm(): Boolean {
        name = etName.text.toString().trim()
        email = etEmail.text.toString().trim()

        if (name.isEmpty() || name.length < 3) {
            Toast.makeText(this, "Please enter name (at least 3 char).", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter valid email.", Toast.LENGTH_SHORT).show()
            return false
        }

        Toast.makeText(this, "Welcome, $name!\nEmail: $email", Toast.LENGTH_LONG).show()
        return true
    }



    fun getJsonRequest() {

        val url = "https://arcade.pivotpt.in/echo.php?"
        val query = app.buildQuery(
            mapOf(
                "name" to "Natarajan Thanikachalam",
                "email" to "someone@email.com"
            )
        )

        val request = JsonObjectRequest(
            Request.Method.GET, url + query, null,
            { response: JSONObject ->
                // ✅ Parse the JSON response
                //val id = response.optInt("id")
                val name = response.optString("name")
                val email = response.optString("email")
                println("Name: $name, Email: $email")
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )

        VolleySingleton.addToRequestQueue(request)
    }

    fun sendJsonPostRequest() {

        val user_id = 1
        // Create a JSON body
        val jsonBody = JSONObject().apply {
            put("word", "guess")
            put("user_id", user_id)
            put("serviceIdD",2)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                // ✅ JSON response received
                val service_code = response.optString("Service Code")
                val message = response.optString("Message")
                val code = response.optInt("Code")
                println("Service Code: $service_code, Message: $message, Code : $code")
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }
    */