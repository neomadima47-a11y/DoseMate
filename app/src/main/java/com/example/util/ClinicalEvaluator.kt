package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.HealthAmber
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthRed

enum class HealthSeverity(val color: Color) {
    NORMAL(HealthGreen),
    ELEVATED(HealthAmber),
    WARNING(Color(0xFFF97316)), // Orange
    CRITICAL(HealthRed),
    INFO(Color(0xFF3B82F6))
}

data class ClinicalEvaluation(
    val classification: String,
    val badgeIcon: String, // 🟢, 🟡, 🟠, 🔴, ⚠️
    val feedbackMessage: String,
    val severity: HealthSeverity,
    val redFlagWarning: String? = null
)

object ClinicalEvaluator {

    /**
     * Evaluates Blood Pressure based on WHO / American Heart Association (AHA) guidelines:
     * - Normal: < 120 and < 80 mmHg
     * - Elevated: 120-129 and < 80 mmHg
     * - Stage 1 Hypertension: 130-139 or 80-89 mmHg
     * - Stage 2 Hypertension: 140+ or 90+ mmHg
     * - Hypertensive Crisis: > 180 or > 120 mmHg
     */
    fun evaluateBloodPressure(
        systolic: Double,
        diastolic: Double,
        symptoms: String = "",
        timing: String = "Morning"
    ): ClinicalEvaluation {
        val timingNote = when (timing) {
            "Post-Exercise" -> " (Recorded post-exercise: temporary elevation is normal. Re-measure after 20-30 min of rest for true baseline.)"
            "Morning" -> " (Recorded in the morning. Morning readings capture baseline cardiac pressure.)"
            "Evening" -> " (Recorded in the evening. Typically reflects daily cumulative physical & mental stressors.)"
            else -> ""
        }

        val (classification, badge, message, severity) = when {
            systolic > 180 || diastolic > 120 -> {
                ClinicalEvaluation(
                    classification = "Hypertensive Crisis",
                    badgeIcon = "⚠️",
                    feedbackMessage = "Critical reading. Rest 5 minutes and re-test, or seek immediate emergency medical care.$timingNote",
                    severity = HealthSeverity.CRITICAL
                )
            }
            systolic >= 140 || diastolic >= 90 -> {
                ClinicalEvaluation(
                    classification = "Stage 2 Hypertension",
                    badgeIcon = "🔴",
                    feedbackMessage = "High blood pressure reading. Consistently high numbers require doctor consultation.$timingNote",
                    severity = HealthSeverity.CRITICAL
                )
            }
            (systolic in 130.0..139.9) || (diastolic in 80.0..89.9) -> {
                ClinicalEvaluation(
                    classification = "Stage 1 Hypertension",
                    badgeIcon = "🟠",
                    feedbackMessage = "Stage 1 High. Recommend recording daily and sharing log with your doctor.$timingNote",
                    severity = HealthSeverity.WARNING
                )
            }
            (systolic in 120.0..129.9) && diastolic < 80.0 -> {
                ClinicalEvaluation(
                    classification = "Elevated",
                    badgeIcon = "🟡",
                    feedbackMessage = "Slightly elevated. Monitor your blood pressure over the coming days.$timingNote",
                    severity = HealthSeverity.ELEVATED
                )
            }
            systolic < 120.0 && diastolic < 80.0 && systolic >= 70.0 && diastolic >= 40.0 -> {
                ClinicalEvaluation(
                    classification = "Normal",
                    badgeIcon = "🟢",
                    feedbackMessage = "Your blood pressure is within the healthy normal range.$timingNote",
                    severity = HealthSeverity.NORMAL
                )
            }
            else -> {
                ClinicalEvaluation(
                    classification = "Low / Out of Range",
                    badgeIcon = "🟡",
                    feedbackMessage = "Reading is lower than standard range. Check for symptoms like dizziness.$timingNote",
                    severity = HealthSeverity.ELEVATED
                )
            }
        }

        val redFlag = checkRedFlagSymptoms(
            isHighBp = (systolic >= 140 || diastolic >= 90),
            symptoms = symptoms
        )

        return ClinicalEvaluation(
            classification = classification,
            badgeIcon = badge,
            feedbackMessage = message,
            severity = severity,
            redFlagWarning = redFlag
        )
    }

    /**
     * Evaluates Blood Glucose (Sugar) tailored to meal/time context:
     * - Fasting / Before Meal (Optimal: 3.9 - 5.5 mmol/L, High: 7.0+)
     * - 2h Post-Meal (Optimal: 3.9 - 7.8 mmol/L, Borderline: 7.9 - 11.0, High: 11.1+)
     * - Bedtime (Optimal: 5.0 - 8.3 mmol/L, Low warning: < 5.0)
     */
    fun evaluateBloodSugar(
        value: Double,
        unit: String,
        symptoms: String = "",
        timing: String = "Fasting"
    ): ClinicalEvaluation {
        // Normalize to mmol/L for evaluation
        // 1 mmol/L ≈ 18.018 mg/dL
        val valueInMmol = if (unit.equals("mg/dL", ignoreCase = true) || value > 35.0) {
            value / 18.0182
        } else {
            value
        }

        val (classification, badge, message, severity) = when {
            // General Hypoglycemia (< 3.9 mmol/L or < 70 mg/dL)
            valueInMmol < 3.9 -> {
                ClinicalEvaluation(
                    classification = "Hypoglycemia (Low)",
                    badgeIcon = "⚠️",
                    feedbackMessage = "Low blood sugar alert! Consume 15-20g of fast-acting carbs (juice, glucose tablet) and re-test in 15 minutes.",
                    severity = HealthSeverity.CRITICAL
                )
            }

            // Bedtime context: Alert if < 5.0 mmol/L due to overnight hypoglycemia risk
            timing == "Bedtime" && valueInMmol < 5.0 -> {
                ClinicalEvaluation(
                    classification = "Borderline Low for Bedtime",
                    badgeIcon = "🟡",
                    feedbackMessage = "Reading is under 5.0 mmol/L at bedtime. A small healthy protein/carb snack is recommended to prevent overnight low blood sugar.",
                    severity = HealthSeverity.ELEVATED
                )
            }

            // Post-Meal Context (2 hours after eating)
            timing == "2h Post-Meal" -> {
                when {
                    valueInMmol <= 7.8 -> {
                        ClinicalEvaluation(
                            classification = "Normal (Post-Meal)",
                            badgeIcon = "🟢",
                            feedbackMessage = "Excellent! Post-meal glucose is below 7.8 mmol/L (healthy normal post-prandial range).",
                            severity = HealthSeverity.NORMAL
                        )
                    }
                    valueInMmol in 7.81..11.0 -> {
                        ClinicalEvaluation(
                            classification = "Elevated (Post-Meal)",
                            badgeIcon = "🟡",
                            feedbackMessage = "Glucose is elevated 2h after your meal (7.8–11.0 mmol/L). A light 10-min walk can assist insulin sensitivity.",
                            severity = HealthSeverity.ELEVATED
                        )
                    }
                    else -> {
                        ClinicalEvaluation(
                            classification = "High Glucose (Post-Meal)",
                            badgeIcon = "🔴",
                            feedbackMessage = "Post-meal glucose exceeds 11.1 mmol/L. Drink water, limit carbohydrate intake, and monitor closely.",
                            severity = HealthSeverity.CRITICAL
                        )
                    }
                }
            }

            // Bedtime Context
            timing == "Bedtime" -> {
                when {
                    valueInMmol in 5.0..8.3 -> {
                        ClinicalEvaluation(
                            classification = "Optimal Bedtime Range",
                            badgeIcon = "🟢",
                            feedbackMessage = "Safe bedtime glucose level (5.0–8.3 mmol/L). Safe target for restful sleep.",
                            severity = HealthSeverity.NORMAL
                        )
                    }
                    else -> {
                        ClinicalEvaluation(
                            classification = "High Bedtime Reading",
                            badgeIcon = "🔴",
                            feedbackMessage = "Bedtime glucose is elevated (> 8.3 mmol/L). Drink plenty of water before bed.",
                            severity = HealthSeverity.WARNING
                        )
                    }
                }
            }

            // Fasting / Before Meal Context (Default standard)
            else -> {
                when {
                    valueInMmol in 3.9..5.55 -> {
                        ClinicalEvaluation(
                            classification = "Normal (Fasting)",
                            badgeIcon = "🟢",
                            feedbackMessage = "Fasting blood sugar is in the healthy normal range (3.9–5.5 mmol/L).",
                            severity = HealthSeverity.NORMAL
                        )
                    }
                    valueInMmol in 5.56..6.99 -> {
                        ClinicalEvaluation(
                            classification = "Pre-diabetes Range (Fasting)",
                            badgeIcon = "🟡",
                            feedbackMessage = "Fasting glucose is slightly elevated (5.6–6.9 mmol/L). A balanced diet and regular activity help keep it normal.",
                            severity = HealthSeverity.ELEVATED
                        )
                    }
                    else -> {
                        ClinicalEvaluation(
                            classification = "Hyperglycemia (Fasting High)",
                            badgeIcon = "🔴",
                            feedbackMessage = "Fasting glucose is 7.0+ mmol/L. Stay well hydrated and consult your healthcare provider.",
                            severity = HealthSeverity.CRITICAL
                        )
                    }
                }
            }
        }

        var redFlag: String? = null
        val lowerSymptoms = symptoms.lowercase()
        if (valueInMmol < 3.9 && (lowerSymptoms.contains("dizzy") || lowerSymptoms.contains("faint") || lowerSymptoms.contains("shak") || lowerSymptoms.contains("sweat"))) {
            redFlag = "Low blood sugar accompanied by dizziness or shakiness requires immediate intake of fast sugars and rest."
        } else if (valueInMmol >= 11.0 && (lowerSymptoms.contains("vomit") || lowerSymptoms.contains("breath") || lowerSymptoms.contains("confus"))) {
            redFlag = "Very high blood sugar with nausea or confusion can indicate ketoacidosis. Contact your healthcare provider immediately."
        }

        return ClinicalEvaluation(
            classification = classification,
            badgeIcon = badge,
            feedbackMessage = message,
            severity = severity,
            redFlagWarning = redFlag
        )
    }

    /**
     * Checks for high risk red flag combinations (e.g., High BP + headache, blurry vision, chest pain)
     */
    private fun checkRedFlagSymptoms(isHighBp: Boolean, symptoms: String): String? {
        if (!isHighBp || symptoms.isBlank()) return null
        val lower = symptoms.lowercase()
        val hasHeadache = lower.contains("headache") || lower.contains("head ache") || lower.contains("migraine")
        val hasVisionChange = lower.contains("vision") || lower.contains("blur") || lower.contains("eyes")
        val hasChestPain = lower.contains("chest") || lower.contains("breath") || lower.contains("shortness")

        return when {
            hasChestPain -> "CRITICAL ALERT: High blood pressure with chest tightness or shortness of breath is a medical emergency. Call emergency services immediately."
            hasHeadache && hasVisionChange -> "High blood pressure accompanied by headache and blurry vision warrants contacting your clinic or doctor urgently."
            hasHeadache -> "High blood pressure accompanied by headache warrants resting and checking in with your doctor if it persists."
            hasVisionChange -> "High blood pressure accompanied by vision changes warrants contacting your healthcare provider."
            else -> null
        }
    }

    /**
     * Helper to parse BP strings like "120/80" or "120 80" or separate values
     */
    fun parseBloodPressure(input: String): Pair<Double, Double>? {
        val parts = input.trim().split(Regex("[/,\\s]+"))
        if (parts.size >= 2) {
            val sys = parts[0].toDoubleOrNull()
            val dia = parts[1].toDoubleOrNull()
            if (sys != null && dia != null) {
                return Pair(sys, dia)
            }
        }
        return null
    }

    /**
     * General evaluation helper given a type and value
     */
    fun evaluate(type: String, value: String): ClinicalEvaluation {
        return if (type.contains("pressure", ignoreCase = true)) {
            val bp = parseBloodPressure(value)
            if (bp != null) {
                evaluateBloodPressure(bp.first, bp.second)
            } else {
                ClinicalEvaluation("Normal", "🟢", "Blood pressure recorded.", HealthSeverity.NORMAL)
            }
        } else if (type.contains("sugar", ignoreCase = true)) {
            val s = value.toDoubleOrNull() ?: 5.4
            evaluateBloodSugar(s, "mmol/L")
        } else {
            ClinicalEvaluation("Symptom Note", "ℹ️", "Symptom logged.", HealthSeverity.INFO)
        }
    }
}
