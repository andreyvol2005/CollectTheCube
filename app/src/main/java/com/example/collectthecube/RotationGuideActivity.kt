package com.example.collectthecube

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RotationGuideActivity : AppCompatActivity() {

    private lateinit var markCompletedButton: Button
    private lateinit var contentContainer: LinearLayout
    private lateinit var currentUser: String
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rotation_guide)

        if (!AppPreferences.isGuestMode) {
            currentUser = intent.getStringExtra("username") ?: ""
            loadProgressFromFirebase()
            recordSessionInFirebase(0)
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
                markRotationGuideAsCompleted()
            }
        } else {
            markCompletedButton.text = "Для сохранения необходима авторизация"
        }
    }

    private fun loadContentFromFirebase() {
        ContentLoader.loadStageContent("rotation_guide",
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
            "rotation_item" -> createRotationItemView(section)
            "algorithm" -> createAlgorithmView(section)
            "image_section" -> createImageSectionView(section)
            "gif" -> createGifView(section["gif_name"] as? String ?: "")
        }
    }

    private fun createTextView(text: String) {
        contentContainer.addView(TextView(this).apply {
            this.text = text
            textSize = 16f
            setLineSpacing(4f, 1f)
            setPadding(0, 0, 0, 32.dpToPx())
        })
    }

    private fun createRotationItemView(section: Map<String, Any>) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 24.dpToPx())
        }

        // Image
        layout.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(80.dpToPx(), 80.dpToPx())
            setImageResource(getDrawableResourceId(section["image_name"] as? String ?: ""))
        })

        // Text content
        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(16.dpToPx(), 0, 0, 0)
        }

        textLayout.addView(TextView(this).apply {
            text = section["notation"] as? String ?: ""
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })

        textLayout.addView(TextView(this).apply {
            text = section["description"] as? String ?: ""
            textSize = 14f
            setLineSpacing(2f, 1f)
        })

        layout.addView(textLayout)
        contentContainer.addView(layout)
    }

    private fun createAlgorithmView(section: Map<String, Any>) {
        // Title
        contentContainer.addView(TextView(this).apply {
            text = section["name"] as? String ?: ""
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 16.dpToPx(), 0, 12.dpToPx())
        })

        // Description
        createTextView(section["description"] as? String ?: "")

        // Moves sequence
        val sequence = section["sequence"] as? List<String> ?: emptyList()
        if (sequence.isNotEmpty()) {
            val movesLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24.dpToPx())
            }

            sequence.forEach { move ->
                movesLayout.addView(TextView(this).apply {
                    text = move
                    textSize = 32f
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx()).apply {
                        setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                    }
                })
            }
            contentContainer.addView(movesLayout)
        }
    }

    private fun createImageSectionView(section: Map<String, Any>) {
        // Title
        contentContainer.addView(TextView(this).apply {
            text = section["title"] as? String ?: ""
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 15.dpToPx(), 0, 15.dpToPx())
        })

        // Image
        contentContainer.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20.dpToPx())
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            setImageResource(getDrawableResourceId(section["image_name"] as? String ?: ""))
        })
    }

    private fun createGifView(gifName: String) {
        contentContainer.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300.dpToPx(), 300.dpToPx()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            Glide.with(this@RotationGuideActivity)
                .load(getDrawableResourceId(gifName))
                .into(this)
        })
    }

    private fun getDrawableResourceId(imageName: String): Int {
        return resources.getIdentifier(imageName, "drawable", packageName)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // Progress methods (остаются без изменений)
    private fun loadProgressFromFirebase() {
        db.collection("users")
            .whereEqualTo("username", currentUser)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val progress = querySnapshot.documents[0].getString("stagesProgress") ?: "00000000"
                    updateButtonState(progress.isNotEmpty() && progress[0] == '1')
                }
            }
    }

    private fun markRotationGuideAsCompleted() {
        db.collection("users")
            .whereEqualTo("username", currentUser)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val currentProgress = doc.getString("stagesProgress") ?: "00000000"
                    val newProgress = "1" + currentProgress.substring(1)

                    doc.reference.update("stagesProgress", newProgress)
                        .addOnSuccessListener {
                            updateButtonState(true)
                            Toast.makeText(this, "Язык вращений изучен!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun updateButtonState(isCompleted: Boolean) {
        markCompletedButton.apply {
            text = if (isCompleted) "Изучено ✓" else "Отметить как изученный"
            setBackgroundColor(ContextCompat.getColor(this@RotationGuideActivity,
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