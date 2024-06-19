package com.example.prm_2_s20514

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

fun saveEntryToFireStore(entry: DiaryEntry) {
    println("Saving entry to Firestore: start")
    val db = Firebase.firestore
    db.collection("entries").add(entry)
        .addOnSuccessListener { documentReference ->
            println("saveEntryToFirestore: onSuccess, documentId: ${documentReference.id}")
            Log.d("Firestore", "DocumentSnapshot written with ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            println("saveEntryToFirestore: onFailure, error: ${e.message}")
            Log.w("Firestore", "Error adding document", e)
        }
    println("Saving entry to Firestore: end")
}