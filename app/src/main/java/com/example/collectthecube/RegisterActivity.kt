package com.example.collectthecube

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var actionButton: Button
    private lateinit var messageTextView: TextView

    private val db = Firebase.firestore

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupToggleGroup()
        setupClickListeners()
    }

    private fun initViews() {
        toggleGroup = findViewById(R.id.toggleGroup)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        actionButton = findViewById(R.id.actionButton)
        messageTextView = findViewById(R.id.messageTextView)
    }

    private fun setupToggleGroup() {
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnLogin -> switchToLoginMode()
                    R.id.btnRegister -> switchToRegisterMode()
                }
            }
        }
    }

    private fun setupClickListeners() {
        actionButton.setOnClickListener {
            if (isLoginMode) {
                attemptLogin()
            } else {
                attemptRegistration()
            }
        }
    }

    private fun switchToLoginMode() {
        isLoginMode = true
        actionButton.text = "Войти"
        clearMessages()
        updateButtonStyles()
    }

    private fun switchToRegisterMode() {
        isLoginMode = false
        actionButton.text = "Зарегистрироваться"
        clearMessages()
        updateButtonStyles()
    }

    private fun updateButtonStyles() {
        if (isLoginMode) {
            btnLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
            btnLogin.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnRegister.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_color))
            btnRegister.setTextColor(ContextCompat.getColor(this, R.color.primary_color))
        } else {
            btnRegister.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
            btnRegister.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_color))
            btnLogin.setTextColor(ContextCompat.getColor(this, R.color.primary_color))
        }
    }

    private fun attemptLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Заполните все поля", true)
            return
        }

        // Ищем пользователя по логину
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    showMessage("Пользователь не найден", true)
                } else {
                    val document = querySnapshot.documents[0]
                    val savedPassword = document.getString("password") ?: ""
                    val stagesProgress = document.getString("stagesProgress") ?: "00000000"

                    if (password == savedPassword) {
                        showMessage("Вход успешен!", false)
                        navigateToCatalog(username, stagesProgress)
                    } else {
                        showMessage("Неверный пароль", true)
                    }
                }
            }
            .addOnFailureListener {
                showMessage("Ошибка подключения", true)
            }
    }

    private fun attemptRegistration() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Заполните все поля", true)
            return
        }

        if (username.length < 3) {
            showMessage("Имя пользователя должно быть не менее 3 символов", true)
            return
        }

        if (password.length < 6) {
            showMessage("Пароль должен быть не менее 6 символов", true)
            return
        }

        // Проверяем, существует ли пользователь
        checkUsernameExists(username) { usernameExists ->
            if (usernameExists) {
                showMessage("Пользователь с таким именем уже существует", true)
            } else {
                // Создаем статистику для нового пользователя
                val currentDate = getCurrentDate()
                val emptyStatistic = createEmptyStatistic(currentDate)

                val userData = hashMapOf(
                    "username" to username,
                    "password" to password,
                    "stagesProgress" to "00000000",
                    "statistic" to emptyStatistic
                )

                db.collection("users")
                    .add(userData)
                    .addOnSuccessListener { documentReference ->
                        showMessage("Регистрация успешна!", false)
                        navigateToCatalog(username, "00000000")
                    }
                    .addOnFailureListener {
                        showMessage("Ошибка регистрации", true)
                    }
            }
        }
    }

    private fun createEmptyStatistic(currentDate: String): String {
        val statistic = JSONObject()
        val sessions = JSONArray()

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.time = sdf.parse(currentDate) ?: Date()

        // Создаем 7 пустых дней
        for (i in 6 downTo 0) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.time = calendar.time
            dayCalendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(dayCalendar.time)

            val dayStats = JSONObject()
            dayStats.put("date", dateStr)
            dayStats.put("stages", JSONArray())
            sessions.put(dayStats)
        }

        statistic.put("sessions", sessions)
        return statistic.toString()
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun checkUsernameExists(username: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                callback(!querySnapshot.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun navigateToCatalog(username: String, stagesProgress: String) {
        android.os.Handler().postDelayed({
            val intent = Intent(this, CatalogActivity::class.java).apply {
                putExtra("username", username)
                putExtra("stages_progress", stagesProgress)
            }
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun showMessage(message: String, isError: Boolean) {
        messageTextView.text = message
        messageTextView.setTextColor(
            ContextCompat.getColor(
                this,
                if (isError) R.color.error_color else R.color.completed_color
            )
        )
        messageTextView.visibility = android.view.View.VISIBLE
    }

    private fun clearMessages() {
        messageTextView.visibility = android.view.View.GONE
        passwordEditText.text?.clear()
    }
}