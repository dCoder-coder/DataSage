package com.retailiq.datasage.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retailiq.datasage.data.api.CreateEventRequest
import com.retailiq.datasage.data.api.EventDto
import com.retailiq.datasage.ui.components.EmptyStateView
import com.retailiq.datasage.ui.components.ShimmerLoadingRow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCalendarScreen(
    viewModel: EventCalendarViewModel = hiltViewModel()
) {
    val upcomingState by viewModel.upcomingEventsState.collectAsState()
    val monthState by viewModel.monthEventsState.collectAsState()

    var showAddEventSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var showEventDetailSheet by remember { mutableStateOf<List<EventDto>?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Event Calendar") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddEventSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upcoming Events
            UpcomingEventsCard(upcomingState)

            // Monthly Calendar
            Text("This Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val events = if (monthState is EventListState.Loaded) {
                (monthState as EventListState.Loaded).events
            } else emptyList()

            CalendarGrid(
                events = events,
                onDayClick = { date, dayEvents ->
                    if (dayEvents.isNotEmpty()) {
                        showEventDetailSheet = dayEvents
                    } else {
                        selectedDate = date
                        showAddEventSheet = true
                    }
                }
            )
        }
    }

    if (showAddEventSheet) {
        val initialDateStr = selectedDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: ""
        AddEventBottomSheet(
            viewModel = viewModel,
            initialDate = initialDateStr,
            onDismiss = { showAddEventSheet = false }
        )
    }

    showEventDetailSheet?.let { eventsForDay ->
        EventDetailBottomSheet(
            events = eventsForDay,
            onDismiss = { showEventDetailSheet = null }
        )
    }
}

// ── Upcoming Events Card ────────────────────────────────────────────────────────

@Composable
fun UpcomingEventsCard(state: EventListState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DateRange, contentDescription = "Upcoming Events")
                Text("Upcoming Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            when (state) {
                is EventListState.Loading -> {
                    ShimmerLoadingRow()
                    ShimmerLoadingRow()
                    ShimmerLoadingRow()
                }
                is EventListState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is EventListState.Loaded -> {
                    if (state.events.isEmpty()) {
                        Text("No upcoming events", color = Color.Gray)
                    } else {
                        state.events.take(5).forEach { event ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(event.name, fontWeight = FontWeight.SemiBold)
                                Text(event.startDate, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Calendar Grid ─────────────────────────────────────────────────────────────

@Composable
fun CalendarGrid(events: List<EventDto>, onDayClick: (Date, List<EventDto>) -> Unit) {
    val calendar = Calendar.getInstance()
    // Go to first day of month
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-based, Sunday=0

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthPrefix = SimpleDateFormat("yyyy-MM-", Locale.getDefault()).format(calendar.time)

    val getEventColor = { type: String ->
        when (type) {
            "HOLIDAY" -> Color.Red
            "FESTIVAL" -> Color(0xFFFFA000) // Orange
            "PROMOTION" -> Color(0xFF009688) // Teal
            "CLOSURE" -> Color.Gray
            else -> Color.Blue
        }
    }

    Column {
        // Weekday headers
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().height(300.dp)
        ) {
            // Empty slots for start of month
            items(firstDayOfWeek) { Spacer(Modifier.size(40.dp)) }

            // Actual days
            items(maxDays) { dayIndex ->
                val day = dayIndex + 1
                val dayStr = String.format("%02d", day)
                val fullDateStr = "$monthPrefix$dayStr"

                val dayEvents = events.filter {
                    // Simple check if date is within start/end
                    it.startDate <= fullDateStr && it.endDate >= fullDateStr
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .clickable {
                            val c = Calendar.getInstance()
                            c.set(Calendar.DAY_OF_MONTH, day)
                            onDayClick(c.time, dayEvents)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.toString(), fontWeight = if (dayEvents.isNotEmpty()) FontWeight.Bold else FontWeight.Normal)
                        if (dayEvents.isNotEmpty()) {
                            // Colored dot below date
                            Box(
                                modifier = Modifier.size(6.dp).clip(CircleShape)
                                    .background(getEventColor(dayEvents.first().type))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Bottom Sheets ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailBottomSheet(events: List<EventDto>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Events on ${events.first().startDate}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(events) { event ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.name, fontWeight = FontWeight.Bold)
                        Text("Type: ${event.type}")
                        Text("Dates: ${event.startDate} to ${event.endDate}")
                        event.expectedImpactPct?.let {
                            Text("Expected Sales Lift: $it%")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventBottomSheet(
    viewModel: EventCalendarViewModel,
    initialDate: String,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PROMOTION") }
    var startDate by remember { mutableStateOf(initialDate) }
    var endDate by remember { mutableStateOf(initialDate) }
    var impact by remember { mutableStateOf("") }

    val types = listOf("HOLIDAY", "FESTIVAL", "PROMOTION", "CLOSURE")

    val createState by viewModel.createState.collectAsState()

    LaunchedEffect(createState) {
        if (createState is EventCreateState.Success) {
            viewModel.resetCreateState()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add New Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Event Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Simple type selector
            Text("Event Type", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                types.forEach { t ->
                    Surface(
                        modifier = Modifier.clickable { type = t }.padding(4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (type == t) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(t, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = startDate, onValueChange = { startDate = it },
                label = { Text("Start Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate, onValueChange = { endDate = it },
                label = { Text("End Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = impact, onValueChange = { impact = it },
                label = { Text("Expected sales lift % (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.createEvent(
                        CreateEventRequest(
                            name = name,
                            type = type,
                            startDate = startDate,
                            endDate = endDate,
                            expectedImpactPct = impact.toDoubleOrNull()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank() && createState !is EventCreateState.InFlight
            ) {
                Text(if (createState is EventCreateState.InFlight) "Saving..." else "Save Event")
            }

            if (createState is EventCreateState.Error) {
                Text((createState as EventCreateState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
