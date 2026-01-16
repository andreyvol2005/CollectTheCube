package com.example.collectthecube

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Stage4Activity : AppCompatActivity() {

    private lateinit var markCompletedButton: Button
    private lateinit var contentContainer: LinearLayout
    private lateinit var currentUser: String
    private val db = Firebase.firestore
    private var stageIndex: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage4)

        if (!AppPreferences.isGuestMode) {
            currentUser = intent.getStringExtra("username") ?: ""
            stageIndex = 3
            loadProgressFromFirebase()
            recordSessionInFirebase(4)
        }
        initViews()
        loadContentFromFirebase()
    }

    private fun recordSessionInFirebase(stageIndex: Int) {
        if (currentUser.isEmpty()) return

        db.collection("users")
            .whereEqualTo("username", currentUser)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val currentStatistic = document.getString("statistic") ?: ""

                    val updatedStatistic = StatisticManager.addSessionToStatistic(
                        currentStatistic,
                        stageIndex
                    )

                    document.reference.update("statistic", updatedStatistic)
                }
            }
    }

    private fun initViews() {
        markCompletedButton = findViewById(R.id.markCompletedButton)
        contentContainer = findViewById(R.id.contentContainer)

        if (!AppPreferences.isGuestMode) {
            markCompletedButton.setOnClickListener {
                markStageAsCompleted()
            }
        } else {
            markCompletedButton.text = "Для сохранения необходима авторизация"
        }
    }

    private fun loadContentFromFirebase() {
        ContentLoader.loadStageContent("stage4",
            onSuccess = { title, sections ->
                findViewById<TextView>(R.id.stageTitleTextView).text = title
                contentContainer.removeAllViews()
                sections.forEach { createSectionView(it) }
            },
            onError = {
                Toast.makeText(this, "Ошибка загрузки контента", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun createSectionView(section: Map<String, Any>) {
        when (section["type"]) {
            "text" -> createTextView(section["content"] as? String ?: "")
            "centered_image" -> createCenteredImage(section)
        }
    }

    private fun createTextView(text: String) {
        contentContainer.addView(TextView(this).apply {
            this.text = text
            textSize = 16f
            setLineSpacing(4f, 1f)
            setPadding(0, 0, 0, 30.dpToPx())
        })
    }

    private fun createCenteredImage(section: Map<String, Any>) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 30.dpToPx())
        }

        // Image
        container.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (section["image_size"] as? Int ?: 150).dpToPx(),
                (section["image_size"] as? Int ?: 150).dpToPx()
            )
            setImageResource(getDrawableResourceId(section["image_name"] as? String ?: ""))
        })

        // Title
        container.addView(TextView(this).apply {
            text = section["title"] as? String ?: ""
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@Stage4Activity, R.color.primary_color))
            setPadding(0, 12.dpToPx(), 0, 0)
        })

        // Description
        container.addView(TextView(this).apply {
            text = section["description"] as? String ?: ""
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 4.dpToPx(), 0, 0)
        })

        contentContainer.addView(container)
    }

    private fun getDrawableResourceId(imageName: String): Int {
        return resources.getIdentifier(imageName, "drawable", packageName)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // Progress methods
    private fun loadProgressFromFirebase() {
        db.collection("users")
            .whereEqualTo("username", currentUser)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val progress = querySnapshot.documents[0].getString("stagesProgress") ?: "00000000"
                    val stagePosition = stageIndex + 1
                    updateButtonState(progress.length > stagePosition && progress[stagePosition] == '1')
                }
            }
    }

    private fun markStageAsCompleted() {
        db.collection("users")
            .whereEqualTo("username", currentUser)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val currentProgress = doc.getString("stagesProgress") ?: "00000000"
                    val stagePosition = stageIndex + 1

                    val newProgress = if (currentProgress.length > stagePosition) {
                        StringBuilder(currentProgress).apply { setCharAt(stagePosition, '1') }.toString()
                    } else {
                        val extendedProgress = currentProgress.padEnd(stagePosition + 1, '0')
                        StringBuilder(extendedProgress).apply { setCharAt(stagePosition, '1') }.toString()
                    }

                    doc.reference.update("stagesProgress", newProgress)
                        .addOnSuccessListener {
                            updateButtonState(true)
                            Toast.makeText(this, "Этап отмечен как изученный!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun updateButtonState(isCompleted: Boolean) {
        markCompletedButton.apply {
            text = if (isCompleted) "Изучено ✓" else "Отметить как изученный"
            setBackgroundColor(ContextCompat.getColor(this@Stage4Activity,
                if (isCompleted) R.color.completed_color else R.color.primary_color))
            isEnabled = !isCompleted
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AppPreferences.isGuestMode) {
            loadProgressFromFirebase()
        }
    }
}