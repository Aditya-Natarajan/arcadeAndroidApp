package com.src.arcade

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var app: App

    private var selectedCategory: String? = null
    private var userId: Int = 18
    private var constantRowInserted = false // Prevent duplicate insert
    private var ans = ""
    private var user_id: Int = -1
    private var lives = 0
    
    private lateinit var spinner: Spinner
    private lateinit var btnPlay :Button
    private lateinit var btnGuess: Button
    private lateinit var btnAns : Button
    private lateinit var btnExit:Button
    private lateinit var btnPlayAgain: Button
    private lateinit var etGuess:EditText
    private lateinit var llContainer:LinearLayout
    private lateinit var tvHint:TextView
    

    private var url = "https://arcade.pivotpt.in/word_gameAPI.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.word_rank)

        spinner = findViewById(R.id.spinner)
        
        btnPlay = findViewById<Button>(R.id.play)
        btnGuess = findViewById<Button>(R.id.guessbtn)
        btnPlayAgain = findViewById<Button>(R.id.playagain)
        btnExit = findViewById<Button>(R.id.exit)
        btnAns = findViewById<Button>(R.id.hint)
        
        etGuess = findViewById<EditText>(R.id.guess)
        val llContainer = findViewById<LinearLayout>(R.id.resultContainer)
        val hint = findViewById<TextView>(R.id.anss)

        app = application as App
        user_id = app.userId
        userId = app.userId

        ArrayAdapter.createFromResource(
            this,
            R.array.dropdown_items,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item)
            spinner.adapter = adapter
        }


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
                        ans = game_state["secret"].toString()

                        if (code != 0) {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            spinner.visibility = View.GONE
                            btnPlay.visibility = View.GONE
                            etGuess.visibility = View.VISIBLE
                            btnGuess.visibility = View.VISIBLE

                            if (!constantRowInserted) {
                                insertConstantRow(llContainer)
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

        btnGuess.setOnClickListener {
            val guessedWord = etGuess.text.toString().trim()
            if (guessedWord.isEmpty()) {
                Toast.makeText(this, "Please enter a guess", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jsonBody = JSONObject().apply {
                put("serviceID", 2)
                put("user_id", user_id)
                put("guess", guessedWord)
                put("choice", selectedCategory)
            }
            val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                { response ->
                    Log.d("SERVER_RESPONSE", response.toString())  // Log entire response

                    // Extract the "0" object

                    val code = response.optInt("Code")
                    val message = response.optString("Message")
                    if(code == 20){
                        Toast.makeText(this,"You entered an invalid word",Toast.LENGTH_LONG).show()
                    }
                    else{
                        val serviceCodeObj = response.optJSONObject("0")
                        val serviceCode =
                            serviceCodeObj?.optInt("Service Code", -1)  // Default to -1 if not found

                        // Extract the "1" object (which contains the guess data)
                        val guessObj = response.optJSONObject("1")
                        val guessArray = guessObj?.optJSONArray("guess")

                        // Extract the guess data if available
                        val guessWord =
                            guessArray?.optJSONObject(guessArray.length()-1)?.optString("word", "unknown").toString()
                        val diffValue =
                            guessArray?.optJSONObject(guessArray.length()-1)?.optInt("diff", 0).toString().toInt()

                        // Extract other values


                        // Log the extracted values
                        Log.d("EXTRACTED_DATA", "Service Code: $serviceCode")
                        Log.d("EXTRACTED_DATA", "Word: $guessWord, Diff: $diffValue")
                        Log.d("EXTRACTED_DATA", "Message: $message")
                        Log.d("EXTRACTED_DATA", "Code: $code")

                        // Show a Toast with a message
                        Toast.makeText(this, "Message: $message", Toast.LENGTH_SHORT).show()
                        val rowLayout = createGuessRow(guessWord, diffValue)
                        val constantIndex =
                            llContainer.indexOfChild(llContainer.findViewWithTag("constant_row"))

                        if (constantIndex != -1) {
                            val childCount = llContainer.childCount
                            if (diffValue > 0) {
                                // Insert above constant row in descending order

                                var insertIndex = 0
                                for (i in 0 until constantIndex) {
                                    val child = llContainer.getChildAt(i) as? LinearLayout
                                    val rankView = child?.getChildAt(1) as? TextView
                                    val rank = rankView?.text?.toString()?.toIntOrNull() ?: continue
                                    if (rank == diffValue) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "this word already exist",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        insertIndex = -1;
                                        break
                                    }
                                    if (rank < diffValue) break // insert before smaller rank
                                    insertIndex = i + 1
                                }
                                if (insertIndex != -1) {
                                    lives = lives + 1
                                    if (lives >= 5) {
                                        btnAns.visibility = View.VISIBLE

                                        hint.text = ans
                                        btnAns.setOnClickListener {
                                            hint.visibility = View.VISIBLE
                                        }

                                    }
                                    llContainer.addView(rowLayout, insertIndex)
                                }

                            } else if (diffValue < 0) {
                                // Insert below constant row in descending order (closer to 0 first)

                                var insertIndex = childCount
                                for (i in constantIndex + 1 until childCount) {
                                    val child = llContainer.getChildAt(i) as? LinearLayout
                                    val rankView = child?.getChildAt(1) as? TextView
                                    val rank = rankView?.text?.toString()?.toIntOrNull() ?: continue
                                    if (rank == diffValue) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "this word already exist",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        insertIndex = -1;
                                        break
                                    }
                                    if (rank < diffValue) {
                                        insertIndex = i
                                        break
                                    }
                                }
                                if (insertIndex != -1) {
                                    lives++
                                    if (lives >= 5) {
                                        btnAns.visibility = View.VISIBLE

                                        hint.text = ans
                                        btnAns.setOnClickListener {
                                            hint.visibility = View.VISIBLE
                                        }

                                    }
                                    llContainer.addView(rowLayout, insertIndex)
                                }
                            }
                        } else {
                            llContainer.addView(rowLayout)
                        }

                        etGuess.text.clear()
                        hideKeyboard(etGuess)

                        if (code == 0 || code == 10) {
                            val constantRow = llContainer.findViewWithTag<LinearLayout>("constant_row")
                            val wordTextView = constantRow?.getChildAt(0) as? TextView
                            wordTextView?.text = guessWord
                            Toast.makeText(this@MainActivity, "code is: $message", Toast.LENGTH_LONG).show()
                            wordTextView?.animate()?.alpha(1f)?.setDuration(1000)?.start()

                            Handler().postDelayed({
                                etGuess.text.clear()
                                llContainer.removeAllViews()
                                spinner.visibility = View.GONE
                                btnPlay.visibility = View.GONE
                                etGuess.visibility = View.GONE
                                btnGuess.visibility = View.GONE
                                selectedCategory = null
                                constantRowInserted = false
                                btnAns.visibility = View.GONE
                                hint.visibility = View.GONE
                                btnPlayAgain.visibility = View.VISIBLE
                                btnExit.visibility = View.VISIBLE
                            }, 2000)
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    }
                    },
                    { error ->
                        Log.e("VOLLEY_ERROR", "Request failed: ${error.message}")
                        Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                )
            VolleySingleton.addToRequestQueue(request)
        }

        etGuess.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(etGuess)
                btnGuess.performClick()
                true
            } else {
                false
            }
        }
        btnPlayAgain.setOnClickListener {
            Toast.makeText(this,"hi",Toast.LENGTH_LONG).show()
        }

//        btnPlayAgain.setOnClickListener {
//            constantRowInserted = false  // ✅ reset here
//
//            selectedCategory = spinner.selectedItem.toString()
//            if (selectedCategory != "Select a Category") {
//                spinner.visibility = View.GONE
//                btnPlay.visibility = View.GONE
//
//                guess.visibility = View.VISIBLE
//                btnGuess.visibility = View.VISIBLE
//
//                if (!constantRowInserted) {
//                    insertConstantRow(llContainer)
//                    constantRowInserted = true
//                }
//
//                Toast.makeText(this, "Selected Category: $selectedCategory", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show()
//            }
//        }


        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmation()
        }
        btnExit.setOnClickListener {
            // Optional: Do any cleanup here
            Toast.makeText(this, "Game Exited!", Toast.LENGTH_SHORT).show()
            finish()
        }

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
            layoutParams =
                LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
            setTypeface(null, Typeface.BOLD)
        }

        rowLayout.addView(wordTextView)
        rowLayout.addView(rankTextView)

        return rowLayout
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

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun showExitConfirmation() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to exit the game?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
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

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


}
