package com.example.prm_2_s20514


import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.Alignment
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.prm_2_s20514.ui.theme.PRM_2_s20514Theme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    var imageUrl = ""
    private val db = Firebase.firestore
    var storageRef = Firebase.storage.reference
    var file: File? = null


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PRM_2_s20514Theme {
                val navController = rememberNavController()
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(text = "PRM2_S20514") },
                            actions = {
                                Button(onClick = {
                                    navController.navigate("createEntry")
                                }) {
                                    Text("Create Entry")
                                }
                                Button(onClick = {
                                    navController.navigate("home")
                                }) {
                                    Text(text = "Home")
                                }
                            })
                    },

                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) { innerPadding ->
                    NavHost(
                        navController = navController, startDestination = "home",
                        Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(Modifier.fillMaxSize())
                        }
                        composable("createEntry") {

                            CreateEntryScreen(navController, Modifier)
                        }
                    }
                }
            }
        }
        checkAllPermissions()
    }

    private fun checkAllPermissions() {

        val permissions =
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    0
                )
            }
        }
    }

    @Composable
    fun HomeScreen(modifier: Modifier = Modifier) {
        val entries = remember { mutableStateListOf<DiaryEntry>() }
        val db = Firebase.firestore
        val storageRef = Firebase.storage.reference

        LaunchedEffect(true) {
            db.collection("entries")
                .get()
                .addOnSuccessListener { documents ->
                    entries.clear()
                    for (document in documents) {
                        val entry = document.toObject(DiaryEntry::class.java)
                        entries.add(entry)
                    }
                }
                .addOnFailureListener() {
                    Log.w("Firestore", "Error getting documents.", it)
                }
        }
        Scaffold(
            modifier = modifier
        ) { innerPadding ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(entries) { entry ->
                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val formattedDate = format.format(entry.timestamp)
                    val imagePainter = rememberImagePainter(
                        data = entry.imageUrlEntry,
                        builder = {
                            crossfade(true)
                            placeholder(R.drawable.placeholder)
                            error(R.drawable.error)
                        }
                    )
                    Image(
                        painter = imagePainter,
                        contentDescription = "entry image",
                        modifier = Modifier
                            .height(150.dp)
                            .width(150.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(text = "Date: $formattedDate")
                    Text(text = entry.title, fontWeight = FontWeight.Bold)
                    Text(text = "Entry: ${entry.content}")
                    Text(text = "Address: ${getAddress(entry.latitude!!, entry.longitude!!)}")


                }
            }
        }
    }

    private fun getCurrentLocation(callback: (DiaryEntry?) -> Unit) {
        println("getCurrentLocation: start")
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this) // Stop location updates
                for (location in locationResult.locations) {
                    if (location != null) {
                        println("getCurrentLocation: onSuccess")
                        val diaryEntry = DiaryEntry(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        callback(diaryEntry)
                        println("Location: ${location.latitude}, ${location.longitude}")
                        break
                    } else {
                        println("location is null")
                        callback(null)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CreateEntryScreen(
        navController: NavController,
        modifier: Modifier = Modifier
    ) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var imageUrlEntry by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Enter title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            TextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Enter content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            //Take Picture Button
            Button(
                onClick = {
                    takePictureLauncher.launch(null)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Take Picture")
            }
            //Save Entry Button
            Button(
                onClick = {
                    getCurrentLocation { diaryEntry ->
                        if (diaryEntry != null) {
                            val entry = DiaryEntry(
                                title = title,
                                content = content,
                                latitude = diaryEntry.latitude,
                                longitude = diaryEntry.longitude,
                                imageUrlEntry = imageUrl
                            )
                            saveEntryToFireStore(entry)
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Save Entry")
            }
        }

    }

    private fun getAddress(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
        if (addresses != null) {
            return if (addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                address.locality
            } else {
                "Address not found"
            }
        } else {
            return "Address not found"
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        println("saveImageToInternalStorage: start")
        // Use a file output stream to write the bitmap to internal storage
        val filename = "temp_image_" + System.currentTimeMillis() + ".png"
        val stream: OutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()
        println("saveImageToInternalStorage: end")
        return Uri.fromFile(File(filesDir, filename))
    }

    val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                val uri = saveImageToInternalStorage(it)
                uploadImageToFirebaseStorage(uri) { uploadedImageUrl ->
                    println("Image uploaded to Firebase Storage: $uploadedImageUrl")
                    imageUrl = uploadedImageUrl //Zapisuje poprawną wartość
                }
            }
        }
}





