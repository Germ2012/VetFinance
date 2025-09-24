// ruta: app/src/main/java/ui/screens/CalendarScreen.kt
package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.viewmodel.VetViewModel
import io.github.bogdanov_alex.compose_calendar.SelectableCalendar
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@Composable
fun CalendarScreen(viewModel: VetViewModel) {

    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val appointmentsOnSelectedDate by viewModel.appointmentsOnSelectedDate.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Abrir diálogo para añadir cita */ }) {
                Icon(Icons.Default.Add, contentDescription = "Agendar Cita")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SelectableCalendar(
                onDateSelected = { date: LocalDate ->
                    viewModel.onCalendarDateSelected(date)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Citas para ${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (appointmentsOnSelectedDate.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay citas para esta fecha.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(appointmentsOnSelectedDate) { appointmentDetails ->
                        AppointmentItem(appointmentDetails)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentItem(details: AppointmentWithDetails) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = sdf.format(Date(details.appointment.appointmentDate))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("$time - ${details.pet.name}", fontWeight = FontWeight.Bold)
            Text("Dueño: ${details.client.name}")
            Text("Motivo: ${details.appointment.description}")
        }
    }
}
