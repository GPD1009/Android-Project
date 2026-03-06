package com.example.nids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val vm: MainViewModel = viewModel()

            MaterialTheme(colorScheme = darkColorScheme(
                background = Color.Black,
                primary = Color(0xFFD0BCFF), // Purple accent
                surface = Color(0xFF1C1B1F)
            )) {
                NavHost(navController, startDestination = "home", modifier = Modifier.background(Color.Black)) {
                    composable("home") { HomeScreen(navController) }
                    composable("active") { ListScreen("Server Queries", vm.activeThreats, vm, true) }
                    composable("history") { ListScreen("Past Actions", vm.history, vm, false) }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(nav: NavController) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("NIDS MONITOR", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFD0BCFF), modifier = Modifier.padding(32.dp))
        Button(onClick = { nav.navigate("active") }, Modifier.fillMaxWidth(0.8f)) { Text("View Server Queries") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { nav.navigate("history") }, Modifier.fillMaxWidth(0.8f)) { Text("View Past Actions") }
    }
}

@Composable
fun ListScreen(title: String, flow: kotlinx.coroutines.flow.Flow<List<ThreatEntity>>, vm: MainViewModel, showActions: Boolean) {
    val items by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        LazyColumn {
            items(items) { threat ->
                Card(Modifier.padding(vertical = 8.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(threat.verdict, color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("Confidence: ${threat.confidence}", color = Color.White)
                        if (showActions) {
                            Row(Modifier.padding(top = 8.dp)) {
                                Button(onClick = { vm.setAction(threat.id, "ALLOW") }) { Text("ALLOW") }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { vm.setAction(threat.id, "BLOCK") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("BLOCK") }
                            }
                        } else {
                            Text("Action: ${threat.userAction}", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}