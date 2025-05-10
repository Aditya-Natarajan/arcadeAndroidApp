package com.src.arcade

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import io.socket.client.Socket
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

    private var board = Array(6) { IntArray(7) }
    private var isFilled = IntArray(7)
    private var host = ""
    private var winner = ""

    private lateinit var app: App
    private lateinit var mSocket: Socket

    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_four)
        println("onCreate")
        app = application as App
        username = app.username
        mSocket = app.mSocket

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

        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmation()
        }
        initSocketListners()
    }

    private fun initSocketListners() {
        println("initSocketListeners")
        println("setting up socket listeners....")
        mSocket.on("joined_room") { args ->
            println("socket joined room")
            if (args.isNotEmpty()) {
                val data = JSONObject(args[0].toString())
                println("Parsed JSONObject: $data")
                val player = data.getString("name")
                if (player != app.username) {
                    runOnUiThread {
                        Toast.makeText(this, "$player has joined", Toast.LENGTH_SHORT).show()
                        tvPlayer2.text = "Player 2: $player"
                        buttonStatus(true)
                        tvRoomCode.visibility = View.GONE
                    }
                }
            }
        }
        mSocket.on("game_played") { args ->
            println("socket game_played")
            val data = JSONObject(args[0].toString())
            val status = data.getJSONObject("status")
            val code = status.getInt("code")
            val message = status.getString("message")
            runOnUiThread {
                if (code != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    val gameState = data.getJSONObject("game_state")
                    val player = gameState.getString("currentPlayer")
                    tvCurPlayer.text = "Current Player: $player"

                    val boardArr = gameState.getJSONArray("board")
                    board = Array(boardArr.length()) { i ->
                        val rowArray = boardArr.getJSONArray(i)
                        IntArray(rowArray.length()) { j -> rowArray.getInt(j) }
                    }
                    val filled = gameState.getJSONArray("isFilled")
                    isFilled = IntArray(filled.length()) { i -> filled.optInt(i, 0) }
                    updateBoard(board, isFilled)
                }
            }
        }
        mSocket.on("player_left") { args ->
            println("socket player_left")
            runOnUiThread {
                val data = JSONObject(args[0].toString())
                if (data.getString("username") != username) {
                    Toast.makeText(this, "opponent left you Won", Toast.LENGTH_SHORT).show()
                }
                println("calling finish in socket player left")
                finish()
            }

        }
        mSocket.on("game_draw") {
            println("socket game_draw")
            runOnUiThread {
                gameEndBox(2)
            }
        }
        mSocket.on("game_won") { args ->
            println("socket game_won")
            val data = JSONObject(args[0].toString())
            val status = data.getJSONObject("status")
            val code = status.getInt("code")
            val message = status.getString("message")
            runOnUiThread {
                if (code != 80) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    val gameState = data.getJSONObject("game_state")
                    val boardArr = gameState.getJSONArray("board")
                    board = Array(boardArr.length()) { i ->
                        val rowArray = boardArr.getJSONArray(i)
                        IntArray(rowArray.length()) { j ->
                            rowArray.getInt(j)
                        }
                    }
                    winner = gameState.getString("winner")
                    println("b4 calling updateBoard")
                    updateBoard(board, isFilled)
                    Toast.makeText(this, "${this.winner} Won", Toast.LENGTH_SHORT).show()
                    if (winner == username) {
                        gameEndBox(1)
                    } else {
                        gameEndBox(0)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mSocket.connected()) {
            mSocket.off()
        }
    }
    //    override fun onStart() {
//        super.onStart()
//        println("onStart")
//        if (!::app.isInitialized) {
//            app = application as App
//        }
//    }
    private fun gameEndBox(condition: Int) {
        println("gameEndBox")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Status")
        var message = ""
        when (condition) {
            1 -> {
                message = "You Won :D"
            }

            0 -> {
                message = "You Lost :("
            }

            2 -> {
                message = "Game Drawn"
            }
        }
        builder.setMessage(message)
        builder.setPositiveButton("Ok") { dialog, _ ->
            dialog.dismiss()
            leaveGame(true)
            finish()
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showExitConfirmation() {
        println("showExitConfirmation")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to forfeit the game?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            leaveGame(false)
            finish()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun buttonStatus(state: Boolean) {
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
        val jsonBody = JSONObject().apply {
            put("serviceID", 1)
            put("username", username)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message")
                val code = response.optInt("code")

                if (code != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    llLobby.visibility = View.GONE
                    val room_code = response.getString("room_code")
                    tvRoomCode.text = "Room Code: " + room_code
                    mSocket.emit("join_room", room_code)
                    app.room_code = room_code
                    host = username
                    val gameState = response.getJSONObject("game_state")
                    tvPlayer1.text = "Player1: " + gameState.getString("player1")
                    tvPlayer2.text = "Player2: " + gameState.getString("player2")
                    tvCurPlayer.text = "Current Player: " + gameState.getString("currentPlayer")

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
        val roomCode = etRoomCode.text.toString()
        if (roomCode.isEmpty()) {
            Toast.makeText(this, "Please enter Room Code to join", Toast.LENGTH_SHORT).show()
            return;
        }
        val jsonBody = JSONObject().apply {
            put("serviceID", 2)
            put("username", username)
            put("room_code", roomCode)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message")
                val code = response.optInt("code")

                if (code != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    llLobby.visibility = View.GONE

                    val room_code = response.getString("room_code");
                    tvRoomCode.text = "Room Code: " + room_code
                    app.room_code = room_code

                    mSocket.emit("join_room", room_code)

                    val gameState = response.getJSONObject("game_state")
                    tvPlayer1.text = "Player1: " + gameState.getString("player1")
                    tvPlayer2.text = "Player2: " + gameState.getString("player2")
                    tvCurPlayer.text = "Current Player: " + gameState.getString("currentPlayer")

                    tvRoomCode.visibility = View.GONE
                    llGame.visibility = View.VISIBLE
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }

    private fun updateBoard(board: Array<IntArray>, filled: IntArray) {
        println("updateBoard")
        for (row in board.indices) {
            for (col in board[row].indices) {
                val cellId = resources.getIdentifier("cell_${row}_${col}", "id", packageName)
                val cell = findViewById<ImageView>(cellId)
                when (board[row][col]) {
                    1 -> cell.setImageResource(R.drawable.red_circle)
                    2 -> cell.setImageResource(R.drawable.yellow_circle)
                    else -> cell.setImageResource(R.drawable.empty_circle)
                }
            }
        }
        for (i in 0..6) {
            if (filled[i] == 1) {
                val btnId = resources.getIdentifier("btnCol${i + 1}", "id", packageName)
                val btn = findViewById<Button>(btnId)
                btn.visibility = View.INVISIBLE
            }
        }
    }

    private fun playGame(column: Int) {
        println("playGame")
        val jsonBody = JSONObject().apply {
            put("serviceID", 3)
            put("username", username)
            put("room_code", app.room_code)
            put("column", column)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message")
                val code = response.optInt("code")
                println("code is :$code")
                when (code) {
                    80, 81, 0 -> {
                        // pass
                    }

                    else -> {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }

    private fun leaveGame(proper: Boolean) {
        println("leaveGame")
        var serviceID = 4
        if (proper) {
            serviceID = 5
        }
        val jsonBody = JSONObject().apply {
            put("serviceID", serviceID)
            put("username", username)
            put("room_code", app.room_code)
        }
        println("post body :" + jsonBody.toString())
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                println("leave room rc : " + response.toString())
                val message = response.optString("message")
                val code = response.optInt("code")

                if (code != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    mSocket.emit("leave_room", app.room_code)
                }
            },
            { error ->
                println("leaveGame Error : " + error.toString())
                println("Volley error leaveGame : ${error.message}")
            }
        )

        VolleySingleton.addToRequestQueue(request)
    }

}