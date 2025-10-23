package com.ssj.powerballwinningnumbers

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssj.powerballwinningnumbers.ui.theme.PowerballWinningNumbersTheme
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: PowerballViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PowerballWinningNumbersTheme {
                PowerballScreen(viewModel)
            }
        }
        viewModel.checkCacheAndFetch()
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDataToPrefs()
    }
}

@Composable
fun PowerballScreen(viewModel: PowerballViewModel) {
    val numbers by viewModel.numbers.observeAsState()
    val isLoading by viewModel.loading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val context = LocalContext.current

    PowerballScreenContent(
        numbers = numbers,
        isLoading = isLoading,
        error = error,
        onFetchClick = { viewModel.fetchLatestNumbers(forceNetwork = true) },
        onWebsiteClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.powerball.com/"))
            context.startActivity(intent)
        }
    )
}

@Composable
fun PowerballScreenContent(
    numbers: PowerballNumbers?,
    isLoading: Boolean,
    error: String?,
    onFetchClick: () -> Unit,
    onWebsiteClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Make the column scrollable
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
            // Removed verticalArrangement to allow scrolling from the top
        ) {
            // Re-added Spacer for top padding in a scrollable view
            Spacer(modifier = Modifier.height(32.dp))
            Title()
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && numbers == null) {
                CircularProgressIndicator()
            }

            error?.let {
                Text(text = it, color = Color.Red, fontSize = 18.sp)
            }

            numbers?.let {
                WinningNumbersDisplay(it)
                Spacer(modifier = Modifier.height(12.dp))
                NextDrawingInfo(it)

                // Increased the space between the jackpot info and the buttons
                Spacer(modifier = Modifier.height(32.dp))

                // This button's state is dependent on isLoading
                Button(
                    onClick = onFetchClick,
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading, // Disable button when loading
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,       // Standby color
                        contentColor = Color.Black,
                        disabledContainerColor = Color.DarkGray, // Deactivated color
                        disabledContentColor = Color.White
                    )
                ) {
                    Text("Get recent Powerball numbers")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // This button is always enabled and has a static color
                Button(
                    onClick = onWebsiteClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,       // Static color
                        contentColor = Color.Black
                    )
                ) {
                    Text("Check Powerball Website")
                }
                // Add spacer at the bottom for padding when scrolled
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun Title() {
    Column(
        modifier = Modifier.width(IntrinsicSize.Max), // Set width to match the widest child
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(text = "Power", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(text = "Ball", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
        // This Divider will now match the width of the Row above
        Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun WinningNumbersDisplay(numbers: PowerballNumbers) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = numbers.drawDateFormatted, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        DateDetails(numbers)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            numbers.numbers.forEach {
                NumberCircle(number = it.toString(), isPowerball = false)
                Spacer(modifier = Modifier.width(8.dp))
            }
            NumberCircle(number = numbers.powerball.toString(), isPowerball = true)
        }
        Spacer(modifier = Modifier.height(16.dp))
        JackpotInfo(numbers)
    }
}

@Composable
fun DateDetails(numbers: PowerballNumbers) {
    val dateDetails = numbers.drawDateObject?.let {
        val diffInDays = (System.currentTimeMillis() - it.time) / (1000 * 60 * 60 * 24)
        when {
            diffInDays == 0L -> "Today"
            diffInDays == 1L -> "Yesterday"
            diffInDays > 1 -> "$diffInDays days ago"
            else -> "Upcoming"
        }
    } ?: ""
    // Use a theme-aware color that adapts to light/dark mode
    Text(text = dateDetails, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun NumberCircle(number: String, isPowerball: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isPowerball) Color.Red else Color.White,
                shape = CircleShape
            )
            .border(1.dp, Color.Black, CircleShape)
    ) {
        Text(
            text = number,
            color = if (isPowerball) Color.White else Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun JackpotInfo(numbers: PowerballNumbers) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Jackpot", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA500))
        Spacer(modifier = Modifier.height(8.dp))
        Text(numbers.jackpotAmount, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006400))
        Spacer(modifier = Modifier.height(4.dp))
        val winnerText = if (numbers.jackpotWinners.equals("None", ignoreCase = true) || numbers.jackpotWinners.isEmpty()) {
            "No Winner"
        } else {
            "Winners: ${numbers.jackpotWinners}"
        }
        // Use a theme-aware color
        Text(winnerText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NextDrawingInfo(numbers: PowerballNumbers) {
    Column(
        modifier = Modifier.width(IntrinsicSize.Max), // Set width to match the widest child
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
        // Use a theme-aware color
        Text("Next Drawing: ${numbers.nextDrawDate}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Estimated Jackpot: ${numbers.nextDrawJackpot}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PowerballWinningNumbersTheme {
        val mockNumbers = PowerballNumbers(
            drawDate = "2025-10-20T00:00:00.000Z",
            drawDateFormatted = "Mon, Oct 20, 2025",
            drawDateObject = Date(),
            numbers = listOf(10, 22, 33, 45, 58),
            powerball = 26,
            multiplier = 2,
            jackpotAmount = "$305 Million",
            cashValue = "$144.1 Million",
            nextDrawJackpot = "$324 Million",
            nextDrawDate = "Wed, Oct 22, 2025",
            jackpotWinners = "1 (GA)",
            nextDrawDateObject = Date()
        )
        PowerballScreenContent(
            numbers = mockNumbers,
            isLoading = false,
            error = null,
            onFetchClick = {},
            onWebsiteClick = {}
        )
    }
}
