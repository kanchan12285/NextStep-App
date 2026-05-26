package com.example.nextstep

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.util.Log
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CAREActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCareSubtitle: TextView
    private lateinit var tvResponse: TextView

    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_care)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        progressBar = findViewById(R.id.progressBar)
        tvCareSubtitle = findViewById(R.id.tvCareSubtitle)
        tvResponse = findViewById(R.id.tvResponse)

        // Remove or hide the button since automatic fetch on start

        // Automatically fetch advice when activity opens. No button click needed.
        fetchFieldOfInterestBasedOnScores()
    }

    private fun fetchFieldOfInterestBasedOnScores() {
        Log.d("CAREActivity", "Fetching fields of interest started")
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            hideLoadingState()
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val scores = document.get("personalityScores") as? Map<String, Long>
                    if (scores != null) {
                        val topTraits = scores.entries.sortedByDescending { it.value }
                            .take(5)
                            .joinToString(", ") { "${it.key.replace("_", " ")}: ${it.value}" }

                        val prompt = """
                            Act as a career mentor.
                            The user has the following personality trait scores from a career assessment game:
                            $topTraits

                            Based on these traits, suggest 3-5 fields of interest or domains that suit them well.
                            Respond with a JSON object with key:
                            - "fields": List of strings naming fields of interest.
                        """.trimIndent()

                        callOpenRouterApiForFields(prompt)
                    } else {
                        Toast.makeText(this, "No personality scores found. Please play games first.", Toast.LENGTH_LONG).show()
                        hideLoadingState()
                    }
                } else {
                    Toast.makeText(this, "User data not found. Please create a profile.", Toast.LENGTH_LONG).show()
                    hideLoadingState()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show()
                hideLoadingState()
            }
    }

    private fun callOpenRouterApiForFields(prompt: String) {
        showLoadingState()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = "YOUR_API_KEY"
                val apiUrl = "https://openrouter.ai/api/v1/chat/completions"
                val mediaType = "application/json".toMediaType()
                val requestBody = gson.toJson(
                    mapOf(
                        "model" to "mistralai/mistral-7b-instruct:free",
                        "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                        "response_format" to mapOf("type" to "json_object")
                    )
                ).toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "Bearer $apiKey")
                    .header("HTTP-Referer", "https://nextstep.com")
                    .header("X-title", "NextStep App")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val openAIResponseType = object : TypeToken<OpenAIResponse>() {}.type
                    val openAIResponse: OpenAIResponse = gson.fromJson(responseBody, openAIResponseType)
                    val jsonContentString = openAIResponse.choices?.get(0)?.message?.content

                    // Custom class to parse fields list
                    data class FieldsResponse(val fields: List<String>)

                    val fieldType = object : TypeToken<FieldsResponse>() {}.type
                    val fieldsResponse: FieldsResponse = gson.fromJson(jsonContentString, fieldType)

                    runOnUiThread { displayFieldsOfInterest(fieldsResponse.fields) }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@CAREActivity, "AI service error. Try again.", Toast.LENGTH_LONG).show()
                        hideLoadingState()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@CAREActivity, "Network error. Check your connection.", Toast.LENGTH_LONG).show()
                    hideLoadingState()
                }
            }
        }
    }

    private fun showLoadingState() {
        progressBar.visibility = View.VISIBLE
        tvResponse.visibility = View.GONE   // Hide response while loading
    }

    private fun hideLoadingState() {
        progressBar.visibility = View.GONE
        tvResponse.visibility = View.VISIBLE // Show response after loading
    }

    private fun displayFieldsOfInterest(fields: List<String>) {
        val builder = StringBuilder()
        builder.append("Fields of Interest for you:\n\n")
        fields.forEach { field ->
            builder.append("• $field\n")
        }
        tvResponse.text = builder.toString()
        hideLoadingState()
    }
}
