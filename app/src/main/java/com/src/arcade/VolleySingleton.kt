package com.src.arcade

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

object VolleySingleton {
    private var requestQueue: RequestQueue? = null

    fun init(context: Context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.applicationContext)
        }
    }

    fun <T> addToRequestQueue(request: Request<T>) {
        requestQueue?.add(request)
    }
}