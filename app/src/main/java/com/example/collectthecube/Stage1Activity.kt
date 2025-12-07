package com.example.collectthecube

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Stage1Activity : AppCompatActivity() {

    private lateinit var markCompletedButton: Button
    private lateinit var contentContainer: LinearLayout
    private lateinit var currentUser: String
    private val db = Firebase.firestore
    private var stageIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage1)

        currentUser = intent.getStringExtra("username") ?: ""
        stageIndex = intent.getIntExtra("stage_index", 0)

        initViews()
        loadContentFromFirebase()
        loadProgressFromFirebase()
        recordSessionInFirebase(1)
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

        markCompletedButton.setOnClickListener {
            markStageAsCompleted()
        }
    }

    private fun loadContentFromFirebase() {
        ContentLoader.loadStageContent("stage1",
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
            "image_with_title" -> createImageWithTitle(section)
            "image_grid" -> createImageGrid(section)
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

    private fun createImageWithTitle(section: Map<String, Any>) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 30.dpToPx())
        }

        // Image
        layout.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (section["image_width"] as? Int ?: 150).dpToPx(),
                (section["image_height"] as? Int ?: 120).dpToPx()
            )
            setImageResource(getDrawableResourceId(section["image_name"] as? String ?: ""))
        })

        // Title
        layout.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = section["title"] as? String ?: ""
            textSize = 25f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(16.dpToPx(), 0, 0, 0)
        })

        contentContainer.addView(layout)
    }

    private fun createImageGrid(section: Map<String, Any>) {
        val items = section["items"] as? List<Map<String, Any>> ?: emptyList()
        val gridLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 30.dpToPx())
        }

        items.forEach { item ->
            gridLayout.addView(createImageGridItem(item))
        }

        contentContainer.addView(gridLayout)
    }

    private fun createImageGridItem(item: Map<String, Any>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // Image
            addView(ImageView(this@Stage1Activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (item["image_size"] as? Int ?: 120).dpToPx(),
                    (item["image_size"] as? Int ?: 120).dpToPx()
                )
                setImageResource(getDrawableResourceId(item["image_name"] as? String ?: ""))
            })

            // Title
            addView(TextView(this@Stage1Activity).apply {
                text = item["title"] as? String ?: ""
                textSize = if ((item["image_size"] as? Int ?: 120) <= 100) 12f else 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.primary_color))
                setPadding(0, 8.dpToPx(), 0, 0)
            })

            // Description
            addView(TextView(this@Stage1Activity).apply {
                text = item["description"] as? String ?: ""
                textSize = if ((item["image_size"] as? Int ?: 120) <= 100) 10f else 12f
                gravity = Gravity.CENTER
                setPadding(0, 4.dpToPx(), 0, 0)
            })
        }
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
            setBackgroundColor(ContextCompat.getColor(this@Stage1Activity,
                if (isCompleted) R.color.completed_color else R.color.primary_color))
            isEnabled = !isCompleted
        }
    }

    override fun onResume() {
        super.onResume()
        loadProgressFromFirebase()
    }
}