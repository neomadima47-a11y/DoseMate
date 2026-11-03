package com.example.util

enum class HealthSeverity(val colorHex: String) {
    NORMAL("#10B981"),
    ELEVATED("#F59E0B"),
    WARNING("#F97316"),
    CRITICAL("#EF4444"),
    INFO("#3B82F6")
}

data class ClinicalEvaluation(
    val classification: String,
    val badgeIcon: String, // 🟢, 🟡, 🟠, 🔴, ⚠️
    val feedbackMessage: String,
    val severity: HealthSeverity,
    val isEmergency: Boolean = false,
    val requiresImmediateAttention: Boolean = false
)

object ClinicalEvaluator {

    fun evaluateBloodPressure(sysDouble: Double, diaDouble: Double, notes: String = "", whenTaken: String = ""): ClinicalEvaluation {
        val sys = sysDouble.toInt()
        val dia = diaDouble.toInt()
        return when {
            sys >= 180 || dia >= 120 -> ClinicalEvaluation(
                classification = "Hypertensive Crisis (Medical Emergency)",
                badgeIcon = "🔴",
                feedbackMessage = "CRITICAL: Blood pressure is dangerously elevated ($sys/$dia mmHg). Rest immediately and contact emergency medical services or proceed to the nearest emergency room.",
                severity = HealthSeverity.CRITICAL,
                isEmergency = true,
                requiresImmediateAttention = true
            )
            sys in 140..179 || dia in 90..119 -> ClinicalEvaluation(
                classification = "Stage 2 Hypertension",
                badgeIcon = "🟠",
                feedbackMessage = "HIGH: Blood pressure reading ($sys/$dia mmHg) indicates Stage 2 Hypertension. Take prescribed antihypertensive medication and record vitals regularly.",
                severity = HealthSeverity.WARNING
            )
            sys in 130..139 || dia in 80..89 -> ClinicalEvaluation(
                classification = "Stage 1 Hypertension",
                badgeIcon = "🟡",
                feedbackMessage = "ELEVATED: Blood pressure ($sys/$dia mmHg) is in Stage 1 Hypertension. Reduce sodium intake and manage daily stress.",
                severity = HealthSeverity.ELEVATED
            )
            sys in 120..129 && dia < 80 -> ClinicalEvaluation(
                classification = "Elevated Blood Pressure",
                badgeIcon = "🟡",
                feedbackMessage = "BORDERLINE: Systolic pressure ($sys/$dia mmHg) is slightly elevated. Maintain physical activity and hydration.",
                severity = HealthSeverity.ELEVATED
            )
            sys in 90..119 && dia in 60..79 -> ClinicalEvaluation(
                classification = "Normal Optimal Blood Pressure",
                badgeIcon = "🟢",
                feedbackMessage = "OPTIMAL: Blood pressure ($sys/$dia mmHg) is in the healthy optimal clinical range.",
                severity = HealthSeverity.NORMAL
            )
            sys < 90 || dia < 60 -> ClinicalEvaluation(
                classification = "Hypotension (Low Blood Pressure)",
                badgeIcon = "⚠️",
                feedbackMessage = "LOW: Blood pressure ($sys/$dia mmHg) is lower than average. Drink fluids, rest, and sit slowly if feeling lightheaded.",
                severity = HealthSeverity.WARNING
            )
            else -> ClinicalEvaluation(
                classification = "Recorded Blood Pressure",
                badgeIcon = "📊",
                feedbackMessage = "Logged blood pressure ($sys/$dia mmHg).",
                severity = HealthSeverity.INFO
            )
        }
    }

    fun evaluateBloodSugar(glucoseMmoll: Double, unit: String = "mmol/L", notes: String = "", whenTaken: String = ""): ClinicalEvaluation {
        return when {
            glucoseMmoll >= 13.9 -> ClinicalEvaluation(
                classification = "Severe Hyperglycemia (High Blood Sugar)",
                badgeIcon = "🔴",
                feedbackMessage = "ALERT: Blood glucose ($glucoseMmoll $unit) is significantly high. Check for ketone symptoms and drink water. Take prescribed insulin or diabetes medication.",
                severity = HealthSeverity.CRITICAL,
                requiresImmediateAttention = true
            )
            glucoseMmoll >= 10.0 -> ClinicalEvaluation(
                classification = "Elevated Glucose (Hyperglycemia)",
                badgeIcon = "🟠",
                feedbackMessage = "HIGH: Blood glucose ($glucoseMmoll $unit) is elevated. Avoid sugary foods, hydrate with water, and take scheduled glycemic medication.",
                severity = HealthSeverity.WARNING
            )
            glucoseMmoll in 4.0..8.5 -> ClinicalEvaluation(
                classification = "Normal Glucose Range",
                badgeIcon = "🟢",
                feedbackMessage = "OPTIMAL: Fasting blood glucose ($glucoseMmoll $unit) is in the ideal clinical target range.",
                severity = HealthSeverity.NORMAL
            )
            glucoseMmoll < 3.9 -> ClinicalEvaluation(
                classification = "Hypoglycemia (Low Blood Sugar Alert)",
                badgeIcon = "🔴",
                feedbackMessage = "CRITICAL: Blood glucose ($glucoseMmoll $unit) is low. Apply the 15-15 rule: consume 15g fast-acting sugar (fruit juice, candy) and retest in 15 minutes.",
                severity = HealthSeverity.CRITICAL,
                isEmergency = true,
                requiresImmediateAttention = true
            )
            else -> ClinicalEvaluation(
                classification = "Recorded Glucose",
                badgeIcon = "📊",
                feedbackMessage = "Logged glucose level ($glucoseMmoll $unit).",
                severity = HealthSeverity.INFO
            )
        }
    }
}
