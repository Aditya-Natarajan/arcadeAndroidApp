package com.src.arcade

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Socket
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import java.util.Locale

class Wordle : AppCompatActivity() {
    private lateinit var app: App
    private lateinit var mSocket: Socket

    private lateinit var etGuess: EditText
    private lateinit var tvAnswer: TextView
    private lateinit var tvLives: TextView

    private lateinit var options: LinearLayout
    private lateinit var gameSpace: LinearLayout

    private lateinit var board4x3: View
    private lateinit var board5x4: View
    private lateinit var board6x5: View
    private lateinit var board7x6: View
    private lateinit var curBoard: View

    private lateinit var btnStart: Button
    private lateinit var btnGuess: Button

    private lateinit var radioGroup: RadioGroup

    private lateinit var secret: String

    private var url = "https://arcade.pivotpt.in/wordleAPI.php"
    private var selected: Int = 5
    private var gameStarted: Boolean = false
    private var guessed: Boolean = false
    private var letters: Int = 5
    private var user_id :Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wordle)

        radioGroup = findViewById(R.id.rgLetters)

        btnStart = findViewById(R.id.btnStartGame)
        btnGuess = findViewById(R.id.btnGuess)

        etGuess = findViewById(R.id.etGuess)
        tvAnswer = findViewById(R.id.tvAnswer)
        tvLives = findViewById(R.id.tvLives)

        options = findViewById(R.id.options)
        gameSpace = findViewById(R.id.gameSpace)

        board4x3 = findViewById(R.id.board4x3)
        board5x4 = findViewById(R.id.board5x4)
        board6x5 = findViewById(R.id.board6x5)
        board7x6 = findViewById(R.id.board7x6)

        app = application as App
        user_id = app.userId
        leaveGame()
        etGuess.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                guessWord()
                true  // consume the event
            } else {
                false  // let the system handle other actions
            }
        }

        etGuess.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = etGuess.text.toString()

                if (input.matches(Regex(".*\\d.*"))) {
                    etGuess.setText(input.replace(Regex("\\d"), ""))
                    etGuess.setSelection(etGuess.text.length)
                } else if (input != input.uppercase()) {
                    etGuess.setText(input.uppercase())
                    etGuess.setSelection(input.length)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnStart.setOnClickListener {
            startGame()
        }

        btnGuess.setOnClickListener {
            guessWord()
        }

        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmation()
        }
    }

    private fun showExitConfirmation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to exit the game?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            leaveGame()
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

    private fun startGame() {
        selected = radioGroup.checkedRadioButtonId
        letters = findViewById<RadioButton>(selected).text.toString().toInt()
        tvLives.setText("Lives ${letters + 1}")
        etGuess.filters = arrayOf(InputFilter.LengthFilter(letters))

        val jsonBody = JSONObject().apply {
            put("serviceID", 1)
            put("user_id", user_id)
            put("length", letters)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("Message")
                val code = response.optString("Code")
                if (code.toInt() != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    gameStarted = true
                    options.visibility = View.GONE
                    gameSpace.visibility = View.VISIBLE

                    if (letters == 3) {
                        board4x3.visibility = View.VISIBLE
                        curBoard = board4x3
                    } else if (letters == 4) {
                        board5x4.visibility = View.VISIBLE
                        curBoard = board5x4
                    } else if (letters == 5) {
                        board6x5.visibility = View.VISIBLE
                        curBoard = board6x5
                    } else {
                        board7x6.visibility = View.VISIBLE
                        curBoard = board7x6
                    }
                    gameSpace.visibility = View.VISIBLE
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showTwoOptionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Play Again?")
        builder.setMessage("Wanna Play Again")

        builder.setPositiveButton("Yes") { dialog, which ->
            playAgain()
            dialog.dismiss()
            gameStarted = true
        }

        builder.setNegativeButton("No") { dialog, which ->
            leaveGame()
            dialog.dismiss()
            finish()
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun guessWord() {
        val guess = etGuess.text.toString().lowercase()
        etGuess.text.clear()
        hideKeyboard(etGuess)

        val jsonBody = JSONObject().apply {
            put("serviceID", 2)
            put("user_id", user_id)
            put("word", guess)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("Message")
                val code = response.optInt("Code")
                val guesses = response.optJSONArray("guesses")
                val bitmaps = response.optJSONArray("bitmaps")
                val lives = response.optInt("lives")
                val playing = response.optBoolean("playing")
                secret = response.optString("secret")
                if (code != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    tvLives.text = "Lives: $lives"
                    if (guesses != null && bitmaps != null) {
                        updateBoard(guesses, bitmaps, playing)
                    }
                    Toast.makeText(this, "You have $lives left", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }

    private fun updateBoard(guesses: JSONArray, bitmaps: JSONArray, playing: Boolean) {
        val boardGroup = curBoard as LinearLayout

        for (i in 0 until boardGroup.childCount) {
            val row = boardGroup.getChildAt(i)
            if (row is TableRow) {
                val guessWord = guesses.optString(i, "").uppercase()
                val bitmapRow = bitmaps.optString(i, "")

                for (j in 0 until row.childCount) {
                    val cell = row.getChildAt(j)
                    if (cell is TextView) {
                        if (j < guessWord.length) {
                            cell.text = guessWord[j].toString()
                        } else {
                            cell.text = ""
                        }
                        if(bitmapRow.isNotEmpty() && bitmapRow.all { it == '2' }) {
                            guessed = true
                        }
                        if (j < bitmapRow.length) {
                            when (bitmapRow[j]) {
                                '2' -> cell.setBackgroundResource(R.drawable.rounded_box_2)
                                '1' -> cell.setBackgroundResource(R.drawable.rounded_box_1)
                                '0' -> cell.setBackgroundResource(R.drawable.rounded_box_0)
                                else -> cell.setBackgroundResource(R.drawable.rounded_box)
                            }
                        }
                    }
                }
            }
        }
        if (!playing) {
            tvAnswer.text = "SECRET: ${secret.uppercase(Locale.getDefault())}"
            tvAnswer.visibility = View.VISIBLE
            showTwoOptionDialog()
        }
        else if(guessed){
            tvAnswer.text = "Congratulations You Got It"
            tvAnswer.visibility = View.VISIBLE
            showTwoOptionDialog()
        }
    }

    private fun playAgain() {
        board4x3.visibility = View.GONE
        board5x4.visibility = View.GONE
        board6x5.visibility = View.GONE
        board7x6.visibility = View.GONE
        tvAnswer.visibility = View.GONE
        gameSpace.visibility = View.GONE
        options.visibility = View.VISIBLE
        val boardGroup = curBoard as LinearLayout
        guessed = false
        for (i in 0 until boardGroup.childCount) {
            val row = boardGroup.getChildAt(i)
            if (row is TableRow) {
                for (j in 0 until row.childCount) {
                    val cell = row.getChildAt(j)
                    if (cell is TextView) {
                        cell.setBackgroundResource(R.drawable.rounded_box)
                        cell.text = "_"
                    }
                }
            }
        }


        btnStart.setOnClickListener {
            selected = radioGroup.checkedRadioButtonId
            letters = findViewById<RadioButton>(selected).text.toString().toInt()
            etGuess.filters = arrayOf(InputFilter.LengthFilter(letters))
            tvLives.text = "Lives: ${1+letters}"
            val jsonBody = JSONObject().apply {
                put("serviceID", 4)
                put("user_id", user_id)
                put("length", letters)
            }
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    val message = response.optString("Message")
                    val code = response.optString("Code")
                    if (code.toInt() != 0) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    } else {
                        gameStarted = true
                        options.visibility = View.GONE
                        gameSpace.visibility = View.VISIBLE

                        if (letters == 3) {
                            board4x3.visibility = View.VISIBLE
                            curBoard = board4x3
                        } else if (letters == 4) {
                            board5x4.visibility = View.VISIBLE
                            curBoard = board5x4
                        } else if (letters == 5) {
                            board6x5.visibility = View.VISIBLE
                            curBoard = board6x5
                        } else {
                            board7x6.visibility = View.VISIBLE
                            curBoard = board7x6
                        }
                        gameSpace.visibility = View.VISIBLE
                    }
                },
                { error ->
                    println("Volley error: ${error.message}")
                }
            )
            VolleySingleton.addToRequestQueue(request)
        }
    }

    private fun leaveGame() {
        val jsonBody = JSONObject().apply {
            put("serviceID", 3)
            put("user_id", user_id)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("Message")
                val code = response.optString("Code")
                if (code.toInt() != 0) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                println("Volley error: ${error.message}")
            }
        )
        VolleySingleton.addToRequestQueue(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveGame()
        finish()
    }
}
