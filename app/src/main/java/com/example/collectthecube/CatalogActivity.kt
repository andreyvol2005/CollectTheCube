package com.example.collectthecube

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CatalogActivity : AppCompatActivity() {

    private lateinit var stagesRecyclerView: RecyclerView
    private lateinit var currentUser: String
    private lateinit var barChart: BarChart
    private lateinit var TitleState: TextView
    private var userStatistic: String = ""

    private val db = Firebase.firestore

    private lateinit var catalogItems: List<CatalogItem>
    private var stagesProgress: String = "00000000"

    data class CatalogItem(
        val title: String,
        val imageResId: Int,
        val activityClass: Class<*>,
        val isRotationGuide: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        if (!AppPreferences.isGuestMode) {
            currentUser = intent.getStringExtra("username") ?: ""
            stagesProgress = intent.getStringExtra("stages_progress") ?: "00000000"
        }

        initCatalogData()
        initViews()
        setupRecyclerView()

        barChart = findViewById(R.id.barChart)
        TitleState = findViewById(R.id.TitleStats)

        if (!AppPreferences.isGuestMode) {
            TitleState.text = "Статистика обучения за неделю"
        } else {
            TitleState.text = "Статистика не доступна в режиме Гость"
        }
        loadUserStatistics()
    }

    private fun loadUserStatistics() {
        val currentUser = intent.getStringExtra("username") ?: ""

        db.collection("users")
            .whereEqualTo("username", currentUser)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    userStatistic = document.getString("statistic") ?: ""
                    setupChart()
                }
            }
    }

    private fun setupChart() {
        val (dates, stageCounts) = StatisticManager.getChartData(userStatistic)

        if (dates.isEmpty()) {
            barChart.visibility = View.GONE
            return
        }

        barChart.visibility = View.VISIBLE

        // Создаем данные для графика
        val entries = ArrayList<BarEntry>()
        for (i in stageCounts.indices) {
            entries.add(BarEntry(i.toFloat(), stageCounts[i].toFloat()))
        }

        val dataSet = BarDataSet(entries, "Количество изученных этапов")

        // Зеленый цвет для всех столбцов
        dataSet.color = ContextCompat.getColor(this, R.color.completed_color)
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.primary_color)

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        // Настройка оси X
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = object : IndexAxisValueFormatter(dates.toTypedArray()) {}
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelCount = dates.size
        xAxis.textSize = 12f
        xAxis.textColor = ContextCompat.getColor(this, R.color.primary_color)
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = dates.size.toFloat() - 0.5f

        // Настройка оси Y
        val yAxis = barChart.axisLeft
        yAxis.granularity = 1f
        yAxis.setDrawGridLines(true)
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 8f // Максимум 8 этапов
        yAxis.textSize = 11f
        yAxis.textColor = ContextCompat.getColor(this, R.color.primary_color)

        // Скрываем правую ось Y
        barChart.axisRight.isEnabled = false

        // Другие настройки графика
        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.legend.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.setDrawBorders(false)
        barChart.setDrawGridBackground(false)

        barChart.invalidate()
    }

    override fun onResume() {
        super.onResume()
        if (!AppPreferences.isGuestMode) {
            updateStagesProgressFromFirebase()
            loadUserStatistics()
        }
    }

    private fun initCatalogData() {
        catalogItems = listOf(
            CatalogItem("Язык вращения", R.drawable.cube_, RotationGuideActivity::class.java, true),
            CatalogItem("Этап 1", R.drawable.cube1, Stage1Activity::class.java),
            CatalogItem("Этап 2", R.drawable.cube2, Stage2Activity::class.java),
            CatalogItem("Этап 3", R.drawable.cube3, Stage3Activity::class.java),
            CatalogItem("Этап 4", R.drawable.cube4, Stage4Activity::class.java),
            CatalogItem("Этап 5", R.drawable.cube5, Stage5Activity::class.java),
            CatalogItem("Этап 6", R.drawable.cube6, Stage6Activity::class.java),
            CatalogItem("Этап 7", R.drawable.cube7, Stage7Activity::class.java)
        )
    }

    private fun initViews() {
        stagesRecyclerView = findViewById(R.id.stagesRecyclerView)
    }

    private fun setupRecyclerView() {
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_stage, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val catalogItem = catalogItems[position]
                val stageCubeImageView = holder.itemView.findViewById<ImageView>(R.id.stageCubeImageView)
                val stageTitleTextView = holder.itemView.findViewById<TextView>(R.id.stageTitleTextView)
                val stageStatusTextView = holder.itemView.findViewById<TextView>(R.id.stageStatusTextView)

                stageCubeImageView.setImageResource(catalogItem.imageResId)
                stageTitleTextView.text = catalogItem.title

                if (!AppPreferences.isGuestMode) {
                    val isCompleted = stagesProgress[position] == '1'

                    if (isCompleted) {
                        stageStatusTextView.text = "Изучено"
                        stageStatusTextView.setTextColor(
                            ContextCompat.getColor(
                                holder.itemView.context,
                                R.color.completed_color
                            )
                        )
                    } else {
                        stageStatusTextView.text = "Изучить"
                        stageStatusTextView.setTextColor(
                            ContextCompat.getColor(
                                holder.itemView.context,
                                R.color.pending_color
                            )
                        )
                    }
                }
                holder.itemView.setOnClickListener {
                    onCatalogItemClick(position)
                }
            }

            override fun getItemCount(): Int = catalogItems.size
        }

        stagesRecyclerView.layoutManager = LinearLayoutManager(this)
        stagesRecyclerView.adapter = adapter
    }

    private fun onCatalogItemClick(position: Int) {
        val catalogItem = catalogItems[position]

        Intent(this, catalogItem.activityClass).apply {
            if (!catalogItem.isRotationGuide) {
                putExtra("stage_index", position - 1)
            }
            if (!AppPreferences.isGuestMode) {
                putExtra("username", currentUser)
                startActivity(this)
            } else {
                startActivity(this)
            }
        }
    }

    private fun updateStagesProgressFromFirebase() {
        db.collection("users")
            .whereEqualTo("username", currentUser)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val newProgress = document.getString("stagesProgress") ?: "00000000"
                    if (newProgress != stagesProgress) {
                        stagesProgress = newProgress
                        stagesRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            }
    }
}