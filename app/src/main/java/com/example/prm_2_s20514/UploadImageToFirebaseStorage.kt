package com.example.prm_2_s20514

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

 fun uploadImageToFirebaseStorage(imageUri: Uri, onSuccess: (String) -> Unit) {
    val storageRef = Firebase.storage.reference
    val imageRef = storageRef.child("images/${imageUri.lastPathSegment}")
    val uploadTask = imageRef.putFile(imageUri)

    uploadTask.addOnSuccessListener {
        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
            val imageUrl = downloadUri.toString()
            onSuccess(imageUrl) // Call the onSuccess with the URL when it is successfully obtained
        }
    }.addOnFailureListener {
        // Handle unsuccessful uploads
        Log.e("Storage", "Upload failed", it)
    }
}
