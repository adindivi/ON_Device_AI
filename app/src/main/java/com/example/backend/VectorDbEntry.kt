package com.example.backend

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class VectorDbMetadata(
    val contextQuery: String = "",
    val dtcCode: String = "",
    val component: String = "",
    val connectorLocation: String = "",
    val dtcs: List<String> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("context_query", contextQuery)
        json.put("dtc_code", dtcCode)
        json.put("component", component)
        json.put("connector_location", connectorLocation)
        val arr = JSONArray()
        dtcs.forEach { arr.put(it) }
        json.put("dtcs", arr)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject?): VectorDbMetadata {
            if (json == null) return VectorDbMetadata()
            val dtcsList = mutableListOf<String>()
            if (json.has("dtcs")) {
                val arr = json.optJSONArray("dtcs")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        dtcsList.add(arr.optString(i, ""))
                    }
                }
            }
            var singleDtc = json.optString("dtc_code", "")
            if (singleDtc.isBlank()) singleDtc = json.optString("dtcCode", "")
            if (singleDtc.isBlank()) singleDtc = json.optString("dtc", "")
            if (singleDtc.isBlank() && dtcsList.isNotEmpty()) {
                singleDtc = dtcsList.first()
            }

            if (dtcsList.isEmpty() && singleDtc.isNotBlank()) {
                dtcsList.add(singleDtc)
            }

            var comp = json.optString("component", "")
            if (comp.isBlank()) comp = json.optString("comp", "")

            var loc = json.optString("connector_location", "")
            if (loc.isBlank()) loc = json.optString("location", "")
            val connName = json.optString("connector_name", "")
            if (connName.isNotBlank() && !loc.contains(connName)) {
                loc = if (loc.isNotBlank()) "$loc ($connName)" else connName
            }

            var query = json.optString("context_query", "")
            if (query.isBlank()) query = json.optString("query", "")

            return VectorDbMetadata(
                contextQuery = query,
                dtcCode = singleDtc,
                component = comp,
                connectorLocation = loc,
                dtcs = dtcsList
            )
        }
    }
}

data class VectorDbEntry(
    val id: String,
    val text: String,
    val embedding: List<Float>,
    val metadata: VectorDbMetadata = VectorDbMetadata(),
    var recommendations: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("text", text)
        
        val embArr = JSONArray()
        embedding.forEach { embArr.put(it.toDouble()) }
        json.put("embedding", embArr)

        json.put("metadata", metadata.toJsonObject())
        json.put("recommendations", recommendations)
        json.put("created_at", createdAt)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): VectorDbEntry {
            var id = json.optString("id", "")
            if (id.isBlank()) id = json.optString("doc_code", "")
            if (id.isBlank()) id = json.optString("code", "")
            if (id.isBlank()) id = "DOC-${UUID.randomUUID().toString().take(8)}"

            var text = json.optString("text", "")
            if (text.isBlank()) text = json.optString("content", "")
            if (text.isBlank()) text = json.optString("fullContent", "")
            if (text.isBlank()) text = json.optString("snippet", "")
            if (text.isBlank()) text = json.optString("title", "")
            
            val embList = mutableListOf<Float>()
            val embArr = json.optJSONArray("embedding")
            if (embArr != null) {
                for (i in 0 until embArr.length()) {
                    embList.add(embArr.optDouble(i, 0.0).toFloat())
                }
            }

            val metadataObj = json.optJSONObject("metadata")
            val meta = if (metadataObj != null) {
                VectorDbMetadata.fromJsonObject(metadataObj)
            } else {
                VectorDbMetadata.fromJsonObject(json)
            }

            val recs = json.optInt("recommendations", json.optInt("recommendationCount", 0))
            val createdAt = json.optLong("created_at", json.optLong("createdAt", System.currentTimeMillis()))

            return VectorDbEntry(
                id = id,
                text = text,
                embedding = embList,
                metadata = meta,
                recommendations = recs,
                createdAt = createdAt
            )
        }
    }
}
