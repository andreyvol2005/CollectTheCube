package com.example.collectthecube

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object StatisticManager {

    fun addSessionToStatistic(currentStatistic: String, stageIndex: Int): String {
        val currentDate = getCurrentDate()

        if (currentStatistic.isEmpty()) {
            return createNewStatistic(currentDate, stageIndex)
        }

        var cleanStatistic = currentStatistic
        if (cleanStatistic.startsWith("\"") && cleanStatistic.endsWith("\"")) {
            cleanStatistic = cleanStatistic.substring(1, cleanStatistic.length - 1)
        }
        cleanStatistic = cleanStatistic.replace("\\\"", "\"")

        val jsonObject = JSONObject(cleanStatistic)
        val sessionsArray = jsonObject.getJSONArray("sessions")

        var todaySessionIndex = -1
        for (i in 0 until sessionsArray.length()) {
            val session = sessionsArray.getJSONObject(i)
            if (session.getString("date") == currentDate) {
                todaySessionIndex = i
                break
            }
        }

        if (todaySessionIndex == -1) {
            return addNewSessionToStatistic(cleanStatistic, currentDate, stageIndex)
        }

        val todaySession = sessionsArray.getJSONObject(todaySessionIndex)
        val stagesArray = todaySession.getJSONArray("stages")

        var stageExists = false
        for (i in 0 until stagesArray.length()) {
            if (stagesArray.getInt(i) == stageIndex) {
                stageExists = true
                break
            }
        }

        if (!stageExists) {
            stagesArray.put(stageIndex)
        }

        return jsonObject.toString()
    }

    private fun addNewSessionToStatistic(currentStatistic: String, currentDate: String, stageIndex: Int): String {
        val jsonObject = JSONObject(currentStatistic)
        val sessionsArray = jsonObject.getJSONArray("sessions")

        val newSession = JSONObject()
        newSession.put("date", currentDate)

        val stagesArray = JSONArray()
        stagesArray.put(stageIndex)
        newSession.put("stages", stagesArray)

        sessionsArray.put(newSession)

        if (sessionsArray.length() > 7) {
            val newSessionsArray = JSONArray()
            for (i in sessionsArray.length() - 7 until sessionsArray.length()) {
                newSessionsArray.put(sessionsArray.getJSONObject(i))
            }
            jsonObject.put("sessions", newSessionsArray)
        }

        return jsonObject.toString()
    }

    private fun createNewStatistic(currentDate: String, stageIndex: Int): String {
        val statistic = JSONObject()
        val sessions = JSONArray()

        val todaySession = JSONObject()
        todaySession.put("date", currentDate)

        val stagesArray = JSONArray()
        stagesArray.put(stageIndex)
        todaySession.put("stages", stagesArray)

        sessions.put(todaySession)
        statistic.put("sessions", sessions)

        return statistic.toString()
    }

    fun getChartData(currentStatistic: String): Pair<List<String>, List<Int>> {
        val dates = mutableListOf<String>()
        val stageCounts = mutableListOf<Int>()

        createLast7Days(dates, stageCounts)

        if (currentStatistic.isNotEmpty()) {
            var cleanStatistic = currentStatistic
            if (cleanStatistic.startsWith("\"") && cleanStatistic.endsWith("\"")) {
                cleanStatistic = cleanStatistic.substring(1, cleanStatistic.length - 1)
            }
            cleanStatistic = cleanStatistic.replace("\\\"", "\"")

            val jsonObject = JSONObject(cleanStatistic)

            if (jsonObject.has("sessions")) {
                val sessionsArray = jsonObject.getJSONArray("sessions")

                val sessionMap = mutableMapOf<String, Int>()

                for (i in 0 until sessionsArray.length()) {
                    val session = sessionsArray.getJSONObject(i)
                    val dateStr = session.getString("date")
                    val stagesArray = session.getJSONArray("stages")

                    val uniqueStages = mutableSetOf<Int>()
                    for (j in 0 until stagesArray.length()) {
                        uniqueStages.add(stagesArray.getInt(j))
                    }

                    sessionMap[dateStr] = uniqueStages.size
                }

                dates.clear()
                stageCounts.clear()

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
        }

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
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr)
        return SimpleDateFormat("d.M", Locale.getDefault()).format(date)
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}