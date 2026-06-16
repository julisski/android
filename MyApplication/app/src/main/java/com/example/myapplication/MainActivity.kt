package com.example.myapplication

import android.os.Bundle                                     // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                   // base Activity that can host a Compose UI
import androidx.activity.compose.setContent                  // installs a Compose UI tree as the Activity content
import androidx.activity.enableEdgeToEdge                    // draw behind the system bars for a modern look
import androidx.compose.foundation.content.MediaType.Companion.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.End
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

// --- Compose foundation: layout, scrolling, drawing, lists --------------------
import androidx.compose.foundation.layout.fillMaxSize        // modifier: take all width AND height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding            // modifier: add empty space AROUND content
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme

// --- Material 3 ---------------------------------------------------------------
import androidx.compose.material3.Scaffold                   // standard screen frame (top bar + insets)
import androidx.compose.material3.Surface

// --- Compose runtime / state --------------------------------------------------
import androidx.compose.runtime.Composable                   // marks a function/lambda as emitting UI

// --- Compose UI ---------------------------------------------------------------
import androidx.compose.ui.Modifier                          // the "how to size/decorate/position" object

import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier)
{
    Column(modifier = Modifier
        .fillMaxSize()               // BE the whole screen (not just wrap content)
        .systemBarsPadding()         // clear the status + navigation bars
        .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment =  Alignment.End
        )
    {

        // RESULT comes right under the controls, so when you drag a slider you
        // SEE the effect immediately — the code reference sits below it.
        Text("One")
        Text("Two")
        Text("Threee")
        Text("Four")

    }





}

