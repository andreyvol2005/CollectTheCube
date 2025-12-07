package com.example.collectthecube

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object StatisticManager {

    fun addSessionToStatistic(currentStatistic: String, stageIndex: Int): String {
        val currentDate = getCurrentDate()
        println("DEBUG: Adding stage $stageIndex for date $currentDate")

        // Если статистика пустая, создаем новую с одним этапом
        if (currentStatistic.isEmpty() || currentStatistic == "null" || currentStatistic == "{}") {
            println("DEBUG: Creating new statistic")
            return createNewStatistic(currentDate, stageIndex)
        }

        try {
            // Очищаем JSON
            var cleanStatistic = currentStatistic
            if (cleanStatistic.startsWith("\"") && cleanStatistic.endsWith("\"")) {
                cleanStatistic = cleanStatistic.substring(1, cleanStatistic.length - 1)
            }
            cleanStatistic = cleanStatistic.replace("\\\"", "\"")

            println("DEBUG: Cleaned statistic: $cleanStatistic")

            // Парсим JSON
            val jsonObject = JSONObject(cleanStatistic)

            if (!jsonObject.has("sessions")) {
                println("DEBUG: No sessions field, creating new")
                return createNewStatistic(currentDate, stageIndex)
            }

            val sessionsArray = jsonObject.getJSONArray("sessions")
            println("DEBUG: Found ${sessionsArray.length()} sessions")

            // Ищем сегодняшнюю дату в сессиях
            var todaySessionIndex = -1
            for (i in 0 until sessionsArray.length()) {
                val session = sessionsArray.getJSONObject(i)
                val sessionDate = session.getString("date")
                if (sessionDate == currentDate) {
                    todaySessionIndex = i
                    println("DEBUG: Found today's session at index $i")
                    break
                }
            }

            if (todaySessionIndex == -1) {
                // Если сегодняшней даты нет, добавляем новую запись
                println("DEBUG: No today's session, adding new one")
                return addNewSessionToStatistic(cleanStatistic, currentDate, stageIndex)
            }

            // Обновляем существующую сессию за сегодня
            val todaySession = sessionsArray.getJSONObject(todaySessionIndex)
            val stagesArray = todaySession.getJSONArray("stages")

            // Проверяем, есть ли уже этот этап
            var stageExists = false
            for (i in 0 until stagesArray.length()) {
                if (stagesArray.getInt(i) == stageIndex) {
                    stageExists = true
                    println("DEBUG: Stage $stageIndex already exists")
                    break
                }
            }

            if (!stageExists) {
                stagesArray.put(stageIndex)
                println("DEBUG: Added stage $stageIndex to today's session")
            }

            return jsonObject.toString()

        } catch (e: Exception) {
            println("DEBUG: Error parsing JSON: ${e.message}")
            e.printStackTrace()
            // При ошибке создаем новую статистику
            return createNewStatistic(currentDate, stageIndex)
        }
    }

    private fun addNewSessionToStatistic(currentStatistic: String, currentDate: String, stageIndex: Int): String {
        try {
            val jsonObject = JSONObject(currentStatistic)
            val sessionsArray = jsonObject.getJSONArray("sessions")

            // Создаем новую сессию для сегодня
            val newSession = JSONObject()
            newSession.put("date", currentDate)

            val stagesArray = JSONArray()
            stagesArray.put(stageIndex)
            newSession.put("stages", stagesArray)

            // Добавляем новую сессию
            sessionsArray.put(newSession)

            // Если сессий больше 7, удаляем самую старую
            if (sessionsArray.length() > 7) {
                val newSessionsArray = JSONArray()
                // Берем последние 7 сессий
                for (i in sessionsArray.length() - 7 until sessionsArray.length()) {
                    newSessionsArray.put(sessionsArray.getJSONObject(i))
                }
                jsonObject.put("sessions", newSessionsArray)
            }

            println("DEBUG: Added new session for today")
            return jsonObject.toString()

        } catch (e: Exception) {
            println("DEBUG: Error adding new session: ${e.message}")
            return createNewStatistic(currentDate, stageIndex)
        }
    }

    private fun createNewStatistic(currentDate: String, stageIndex: Int): String {
        val statistic = JSONObject()
        val sessions = JSONArray()

        // Создаем только одну запись - сегодняшний день
        val todaySession = JSONObject()
        todaySession.put("date", currentDate)

        val stagesArray = JSONArray()
        stagesArray.put(stageIndex)
        todaySession.put("stages", stagesArray)

        sessions.put(todaySession)
        statistic.put("sessions", sessions)

        println("DEBUG: Created new statistic with 1 session")
        return statistic.toString()
    }

    fun getChartData(currentStatistic: String): Pair<List<String>, List<Int>> {
        val dates = mutableListOf<String>()
        val stageCounts = mutableListOf<Int>()

        println("DEBUG: Getting chart data from: $currentStatistic")

        // Всегда создаем 7 дней
        createLast7Days(dates, stageCounts)

        if (currentStatistic.isNotEmpty() && currentStatistic != "null" && currentStatistic != "{}") {
            try {
                var cleanStatistic = currentStatistic
                if (cleanStatistic.startsWith("\"") && cleanStatistic.endsWith("\"")) {
                    cleanStatistic = cleanStatistic.substring(1, cleanStatistic.length - 1)
                }
                cleanStatistic = cleanStatistic.replace("\\\"", "\"")

                val jsonObject = JSONObject(cleanStatistic)

                if (jsonObject.has("sessions")) {
                    val sessionsArray = jsonObject.getJSONArray("sessions")

                    // Очищаем и заполняем заново
                    dates.clear()
                    stageCounts.clear()

                    // Создаем мапу для быстрого доступа
                    val sessionMap = mutableMapOf<String, Int>()

                    for (i in 0 until sessionsArray.length()) {
                        val session = sessionsArray.getJSONObject(i)
                        val dateStr = session.getString("date")
                        val stagesArray = session.getJSONArray("stages")

                        // Считаем уникальные этапы
                        val uniqueStages = mutableSetOf<Int>()
                        for (j in 0 until stagesArray.length()) {
                            uniqueStages.add(stagesArray.getInt(j))
                        }

                        sessionMap[dateStr] = uniqueStages.size
                    }

                    // Заполняем последние 7 дней
                    val calendar = Calendar.getInstance()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    for (i in 6 downTo 0) {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -i)
                        val dateStr = sdf.format(calendar.time)

                        dates.add(formatDateAsDayMonth(dateStr))
                        stageCounts.add(sessionMap[dateStr] ?: 0)
                    }
                }

            } catch (e: Exception) {
                println("DEBUG: Error parsing for chart: ${e.message}")
                // При ошибке возвращаем пустые данные
                createLast7Days(dates, stageCounts)
            }
        }

        println("DEBUG: Chart data - dates: $dates, counts: $stageCounts")
        return Pair(dates, stageCounts)
    }

    private fun createLast7Days(dates: MutableList<String>, stageCounts: MutableList<Int>) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        dates.clear()
        stageCounts.clear()

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(calendar.time)
            dates.add(formatDateAsDayMonth(dateStr))
            stageCounts.add(0)
        }
    }

    private fun formatDateAsDayMonth(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            SimpleDateFormat("d.M", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}