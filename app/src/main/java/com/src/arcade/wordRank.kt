package com.src.arcade

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class wordRank : AppCompatActivity() {
    private var selectedCategory: String? = null
    private var userId: Int = 18
    private var constantRowInserted = false // Prevent duplicate insert
    private var ans = ""
    private var lives = 0

    private var url = "https://arcade.pivotpt.in/word_gameAPI.php"

    private lateinit var spinner: Spinner

    private lateinit var btnPlay: Button
    private lateinit var btnGuess: Button
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button
    private lateinit var btnHint: Button

    private lateinit var etGuess: EditText

    private lateinit var tvAnswer: TextView

    private lateinit var container: LinearLayout

    private lateinit var app: App
    private var user_id: Int = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.word_rank)


        spinner = findViewById(R.id.spinner)

        btnPlay = findViewById<Button>(R.id.play)
        btnGuess = findViewById<Button>(R.id.guessbtn)
        btnPlayAgain = findViewById<Button>(R.id.playagain)
        btnExit = findViewById<Button>(R.id.exit)


        tvAnswer = findViewById<TextView>(R.id.anss)

        etGuess = findViewById<EditText>(R.id.guess)

        container = findViewById<LinearLayout>(R.id.resultContainer)
        btnHint = findViewById<Button>(R.id.hint)

        ArrayAdapter.createFromResource(
            this,
            R.array.dropdown_items,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item)
            spinner.adapter = adapter
        }

        app = application as App
        userId = app.userId

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedCategory =
                    if (position == 0) null else parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCategory = null
            }
        }

        btnPlay.setOnClickListener {
            playGame()
        }

        btnHint.setOnClickListener {

        }
        btnGuess.setOnClickListener {

        }



    }

    override fun onBackPressed() {
        super.onBackPressed()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to exit the game?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            finish() // Exit the activity
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Just close the dialog
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun createGuessRow(word: String, diff: Int): LinearLayout {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.parseColor("#E0F7FA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
        }

        val wordTextView = TextView(this).apply {
            text = word.capitalize()
            textSize = 18f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#24C62A"))
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTypeface(null, Typeface.BOLD)
        }

        val rankTextView = TextView(this).apply {
            text = diff.toString()
            textSize = 18f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#FFC107"))
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
            setTypeface(null, Typeface.BOLD)
        }

        rowLayout.addView(wordTextView)
        rowLayout.addView(rankTextView)

        return rowLayout
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun insertConstantRow(container: LinearLayout, guess: String? = null) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
            tag = "constant_row"
            setBackgroundColor(Color.parseColor("#FFF3E0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
        }

        val wordTextView = TextView(this).apply {
            text = guess?.capitalize()
            textSize = 20f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#24C62A"))
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTypeface(null, Typeface.BOLD)
        }

        val rankTextView = TextView(this).apply {
            text = "?"
            textSize = 20f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#FFC107"))
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            layoutParams =
                LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
            setTypeface(null, Typeface.BOLD)
        }

        rowLayout.addView(wordTextView)
        rowLayout.addView(rankTextView)

        container.addView(rowLayout)
    }

    private fun playGame() {

        if (selectedCategory == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
        } else {
            val jsonBody = JSONObject().apply {
                put("serviceID", 1)
                put("user_id", user_id)
                put("choice", selectedCategory)
            }
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    val message = response.optString("Message")
                    val code = response.optInt("Code")
                    val game_state = response.optJSONObject("0")
                    val secret = game_state["secret"]
                    val guess = game_state["guess"]
                    if (code != 0) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        spinner.visibility = View.GONE
                        btnPlay.visibility = View.GONE
                        etGuess.visibility = View.VISIBLE
                        btnGuess.visibility = View.VISIBLE

                        if (!constantRowInserted) {
                            insertConstantRow(container)
                            constantRowInserted = true
                        }
                    }
                },
                { error ->
                    println("Volley error: ${error.message}")
                }
            )
            VolleySingleton.addToRequestQueue(request)
        }
    }

    private fun gameHint(){

    }
}