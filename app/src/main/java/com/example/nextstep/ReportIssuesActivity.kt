package com.example.nextstep

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.nextstep.ui.setOnClickWithSound

class ReportIssuesActivity : AppCompatActivity() {

    private lateinit var spinnerIssueType: Spinner
    private lateinit var etIssueDescription: EditText
    private lateinit var etContactEmail: EditText
    private lateinit var btnSubmitReport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_issues)

        spinnerIssueType = findViewById(R.id.spinnerIssueType)
        etIssueDescription = findViewById(R.id.etIssueDescription)
        etContactEmail = findViewById(R.id.etContactEmail)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)

        btnSubmitReport.setOnClickWithSound {
            val issueType = spinnerIssueType.selectedItem.toString()
            val description = etIssueDescription.text.toString().trim()
            val email = etContactEmail.text.toString().trim()

            if (description.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "❌ Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickWithSound
            }

            Toast.makeText(
                this,
                "✅ Thank you! Your $issueType issue has been reported.",
                Toast.LENGTH_LONG
            ).show()

            etIssueDescription.text.clear()
            etContactEmail.text.clear()
        }
    }
}
