package com.src.arcade

import android.app.Application
import android.net.Uri
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import java.net.URISyntaxException

class App : Application() {
    lateinit var mSocket: Socket
    lateinit var username:String
    var userId:Int = -1
    lateinit var room_code: String
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