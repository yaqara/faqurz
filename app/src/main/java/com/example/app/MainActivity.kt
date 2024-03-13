package com.example.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MainActivity : ComponentActivity() {
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras
            val bitmap = bundle?.get("data") as Bitmap
            recognizeText(bitmap)
        }
    }
    private var recognizedText by mutableStateOf("")
    private var isTextRec = false

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val text = remember { mutableStateOf("") }

                Button(
                    onClick = {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        activityResultLauncher.launch(intent)
                    }
                ) {
                    Text(
                        text = "Take photo",
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                    )
                }
                if (isTextRec) {
                    TextField(value = recognizedText, onValueChange = { recognizedText = it })
                    Button(onClick = {
                        sendRequest(recognizedText)
                    }) {
                        Text("Send")
                    }
                }
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    private fun recognizeText(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener {
                recognizedText = it.text
                isTextRec = true
            }
    }

    private fun sendRequest(text : String) {
        val url = URL("https://api.openai.com/v1/completions")
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        val apiKey = ""
        val request = CompletionRequest(
            model = "text-davinci-002",
            prompt = "Hello, how are you?"
        )
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true

            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes(request.toString())
            outputStream.flush()
            outputStream.close()

            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonResponse = reader.use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun CompletionRequest(model: String, prompt: String): Any {
        return model.toUri()
    }

}