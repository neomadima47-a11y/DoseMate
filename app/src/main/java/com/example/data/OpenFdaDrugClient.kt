package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class DrugVerificationResult(
    val brandName: String,
    val genericName: String,
    val dosageGuidance: String,
    val warningsSummary: String,
    val isVerified: Boolean
)

data class DrugInteractionAlert(
    val drugA: String,
    val drugB: String,
    val severity: String, // "High", "Moderate", "Advisory"
    val warningSnippet: String
)

class OpenFdaDrugClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Safely reads the injected openFDA API key
    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.OPENFDA_API_KEY
            if (key.isNullOrBlank() || key == "MY_NEW_API_KEY_DEFAULT_VALUE") "" else key
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Searches OpenFDA Drug Labels to verify drug brand/generic name and retrieve dosage guidance.
     */
    suspend fun verifyDrug(query: String): DrugVerificationResult? = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 3) return@withContext null

        try {
            val apiKey = getApiKey()
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val searchParam = "(openfda.brand_name:$encodedQuery+OR+openfda.generic_name:$encodedQuery)"
            val urlBuilder = StringBuilder("https://api.fda.gov/drug/label.json?")
            if (apiKey.isNotBlank()) {
                urlBuilder.append("api_key=").append(apiKey).append("&")
            }
            urlBuilder.append("search=").append(searchParam).append("&limit=1")

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.d("OpenFdaDrugClient", "API responded with code ${response.code}")
                return@withContext null
            }

            val bodyString = response.body?.string() ?: return@withContext null
            val root = JSONObject(bodyString)
            val results = root.optJSONArray("results")
            if (results == null || results.length() == 0) return@withContext null

            val drugObj = results.getJSONObject(0)
            val openfda = drugObj.optJSONObject("openfda")

            var brand = ""
            var generic = ""
            if (openfda != null) {
                val brandArr = openfda.optJSONArray("brand_name")
                if (brandArr != null && brandArr.length() > 0) brand = brandArr.getString(0)
                val genericArr = openfda.optJSONArray("generic_name")
                if (genericArr != null && genericArr.length() > 0) generic = genericArr.getString(0)
            }

            if (brand.isEmpty()) brand = trimmed
            if (generic.isEmpty()) generic = brand

            // Dosage forms / strengths
            var dosage = ""
            val formsArr = drugObj.optJSONArray("dosage_forms_and_strengths")
            if (formsArr != null && formsArr.length() > 0) {
                dosage = formsArr.getString(0).take(200).replace("\n", " ").trim()
            }

            // Warnings
            var warning = ""
            val boxedArr = drugObj.optJSONArray("boxed_warning")
                ?: drugObj.optJSONArray("warnings")
                ?: drugObj.optJSONArray("warnings_and_precautions")

            if (boxedArr != null && boxedArr.length() > 0) {
                warning = boxedArr.getString(0).take(240).replace("\n", " ").trim() + "..."
            }

            DrugVerificationResult(
                brandName = brand,
                genericName = generic,
                dosageGuidance = dosage,
                warningsSummary = warning,
                isVerified = true
            )
        } catch (e: Exception) {
            Log.e("OpenFdaDrugClient", "Error querying OpenFDA drug API", e)
            null
        }
    }

    /**
     * Checks if there is a known interaction warning between a newly added drug and existing active drugs.
     */
    suspend fun checkInteraction(newDrug: String, existingDrugs: List<String>): List<DrugInteractionAlert> = withContext(Dispatchers.IO) {
        val alerts = mutableListOf<DrugInteractionAlert>()
        val trimmedNew = newDrug.trim()
        if (trimmedNew.length < 3 || existingDrugs.isEmpty()) return@withContext alerts

        val apiKey = getApiKey()
        for (existing in existingDrugs) {
            val trimmedExisting = existing.trim()
            if (trimmedExisting.equals(trimmedNew, ignoreCase = true)) continue

            try {
                val encodedNew = URLEncoder.encode(trimmedNew, "UTF-8")
                val encodedExisting = URLEncoder.encode(trimmedExisting, "UTF-8")
                val searchParam = "(openfda.brand_name:$encodedNew+OR+openfda.generic_name:$encodedNew)+AND+(drug_interactions:$encodedExisting)"

                val urlBuilder = StringBuilder("https://api.fda.gov/drug/label.json?")
                if (apiKey.isNotBlank()) {
                    urlBuilder.append("api_key=").append(apiKey).append("&")
                }
                urlBuilder.append("search=").append(searchParam).append("&limit=1")

                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: continue
                    val root = JSONObject(bodyString)
                    val results = root.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val item = results.getJSONObject(0)
                        val interactions = item.optJSONArray("drug_interactions")
                        var snippet = "Documented clinical interaction on FDA label."
                        if (interactions != null && interactions.length() > 0) {
                            snippet = interactions.getString(0).take(220).replace("\n", " ").trim() + "..."
                        }

                        alerts.add(
                            DrugInteractionAlert(
                                drugA = trimmedNew,
                                drugB = trimmedExisting,
                                severity = "Advisory",
                                warningSnippet = snippet
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("OpenFdaDrugClient", "Failed interaction check for $newDrug and $existing", e)
            }
        }
        alerts
    }
}
