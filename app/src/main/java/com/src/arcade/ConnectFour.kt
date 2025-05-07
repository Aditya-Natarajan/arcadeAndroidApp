package com.src.arcade

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class ConnectFour : AppCompatActivity() {

    private lateinit var btnHost: Button
    private lateinit var btnJoin: Button
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button
    private lateinit var btn5: Button
    private lateinit var btn6: Button
    private lateinit var btn7: Button

    private lateinit var tvPlayer1: TextView
    private lateinit var tvPlayer2: TextView
    private lateinit var tvCurPlayer: TextView
    private lateinit var tvRoomCode: TextView

    private lateinit var llLobby: LinearLayout
    private lateinit var llGame: LinearLayout

    private lateinit var etRoomCode: EditText

    private val url = "https://arcade.pivotpt.in/connectFourAPI.php"

    var board = Array(6) { IntArray(7) }
    var isFilled = IntArray(7)

    private lateinit var app: App
    private lateinit var username: String
    private var user_id: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_four)

        app = application as App
        user_id = app.userId
        username = app.username

        btnHost = findViewById(R.id.btnHost)
        btnJoin = findViewById(R.id.btnJoin)
        btn1 = findViewById(R.id.btnCol1)
        btn2 = findViewById(R.id.btnCol2)
        btn3 = findViewById(R.id.btnCol3)
        btn4 = findViewById(R.id.btnCol4)
        btn5 = findViewById(R.id.btnCol5)
        btn6 = findViewById(R.id.btnCol6)
        btn7 = findViewById(R.id.btnCol7)

        tvPlayer1 = findViewById(R.id.tvPlayer1)
        tvPlayer2 = findViewById(R.id.tvPlayer2)
        tvCurPlayer = findViewById(R.id.tvCurrentPlayer)
        tvRoomCode = findViewById(R.id.tvRoomCode)

        llGame = findViewById(R.id.llGame)
        llLobby = findViewById(R.id.llLobby)

        etRoomCode = findViewById(R.id.etRoomCode)

        btnHost.setOnClickListener {
            hostGame()
        }
        btnJoin.setOnClickListener {
            joinGame()
        }
        btn1.setOnClickListener {
            playGame(0)
        }
        btn2.setOnClickListener {
            playGame(1)
        }
        btn3.setOnClickListener {
            playGame(2)
        }
        btn4.setOnClickListener {
            playGame(3)
        }
        btn5.setOnClickListener {
            playGame(4)
        }
        btn6.setOnClickListener {
            playGame(5)
        }
        btn7.setOnClickListener {
            playGame(6)
        }
    }

    private fun buttonStatus(state:Boolean){
        btn1.isEnabled = state
        btn2.isEnabled = state
        btn3.isEnabled = state
        btn4.isEnabled = state
        btn5.isEnabled = state
        btn6.isEnabled = state
        btn7.isEnabled = state
    }

    private fun hostGame() {
        buttonStatus(false)
        //call socket functions
        val jsonBody = JSONObject().apply {
            put("serviceID", 1)
            put("user_id", user_id)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("Message")
                val code = response.optString("Code")

                tvRoomCode.text = "Room Code: " + response.getString("room_code")

                val gameState = response.getJSONObject("game_state")
                tvPlayer1.text = "Player1: " + gameState.getString("player1")
                tvPlayer2.text = "Player2: " + gameState.getString("player2")
                tvCurPlayer.text = "Current Player: " + gameState.getString("currentPlayer")


                if (code.toInt() != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    llLobby.visibility = View.GONE
                    llGame.visibility = View.VISIBLE
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }

    private fun joinGame() {
        buttonStatus(true)
        //call socket functions
        val jsonBody = JSONObject().apply {
            put("serviceID", 2)
            put("user_id", user_id)
            put("room_code", etRoomCode.text.toString())
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("Message")
                val code = response.optString("Code")

                tvRoomCode.text = "Room Code: " + response.getString("room_code")

                val gameState = response.getJSONObject("game_state")
                tvPlayer1.text = "Player1: " + gameState.getString("player1")
                tvPlayer2.text = "Player2: " + gameState.getString("player2")
                tvCurPlayer.text = "Current Player: " + gameState.getString("currentPlayer")


                if (code.toInt() != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    llLobby.visibility = View.GONE
                    llGame.visibility = View.VISIBLE
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }

    //    @SuppressLint("DiscouragedApi")
    private fun updateBoard(column: Int, row: Int, color: Int) {
        val cellId = resources.getIdentifier("cell_${row}_${column}", "id", packageName)
        val cell = findViewById<ImageView>(cellId)
        when (color) {
            1 -> cell.setImageResource(R.drawable.red_circle)
            2 -> cell.setImageResource(R.drawable.yellow_circle)
            else -> cell.setImageResource(R.drawable.empty_circle) // clear the cell
        }
    }


    private fun playGame(column :Int){
        //socket emit

        val jsonBody = JSONObject().apply {
            put("serviceID", 5)
            put("username", username)
            put("room_code", etRoomCode.text.toString())
            put("column",column)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("Message")
                val code = response.optString("Code")

                if (code.toInt() != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    val gameState = response.getJSONObject("game_state")

                    tvCurPlayer.text = "Current Player: " + gameState.getString("currentPlayer")

                    val boardArr = gameState.getJSONArray("board")
                    val filled = gameState.getJSONArray("isFilled")

                    for(i in 0..7){
                        isFilled[i] = filled.getInt(i)
                    }
                    var flag = true
                    var color = 0
                    var rowIndex = 0
                    for(i in 0..5){
                        val row = boardArr.getJSONArray(i)
                        for(j in 0 .. 6){
                            board[i][j] = row.getInt(j)
                            if(board[i][j] != 0 && flag){
                                rowIndex = i
                                flag = false
                                color = board[i][j]
                            }
                        }
                    }
                    updateBoard(column,rowIndex,color)
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }


}