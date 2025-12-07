package com.example.collectthecube

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import org.json.JSONArray

object ContentLoader {
    private val db = Firebase.firestore

    fun loadStageContent(stageId: String, onSuccess: (String, List<Map<String, Any>>) -> Unit, onError: (Exception) -> Unit) {
        fun convertJsonObject(obj: JSONObject): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            val keys = obj.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.get(key)

                map[key] = when (value) {
                    is JSONObject -> convertJsonObject(value)
                    is JSONArray -> (0 until value.length()).map { i ->
                        val item = value.get(i)
                        if (item is JSONObject) convertJsonObject(item) else item
                    }
                    else -> value
                }
            }
            return map
        }

        db.collection("stages_content").document(stageId).get()
            .addOnSuccessListener { document ->
                val jsonString = document.getString("data")!!
                val jsonObject = JSONObject(jsonString)
                val title = jsonObject.getString("title")
                val sectionsArray = jsonObject.getJSONArray("sections")

                val sectionsList = (0 until sectionsArray.length()).map { i ->
                    convertJsonObject(sectionsArray.getJSONObject(i))
                }

                onSuccess(title, sectionsList)
            }
            .addOnFailureListener(onError)
    }
}