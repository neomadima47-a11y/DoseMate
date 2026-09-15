package com.example.data

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Singleton client for interacting with Cloud Firestore.
 */
object FirestoreClient {
    /**
     * Get the default FirebaseFirestore instance.
     * Firebase is automatically initialized by the google-services plugin.
     */
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Get a reference to the 'users' collection
     */
    fun getUsersCollection() = db.collection("users")

    /**
     * Get a reference to the 'health_readings' collection
     */
    fun getReadingsCollection() = db.collection("health_readings")

    /**
     * Get a reference to the 'medications' collection
     */
    fun getMedicationsCollection() = db.collection("medications")

    /**
     * Get a reference to the 'dose_logs' collection
     */
    fun getDoseLogsCollection() = db.collection("dose_logs")
}
