package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.viewmodel.VetViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: VetViewModel) {
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val appointments by viewModel.appointmentsOnSelectedDate.collectAsState()

    // Configuración del estado del calendario
    val currentMonth = YearMonth.now()
    val startMonth = currentMonth.minusMonths(100)
    val endMonth = currentMonth.plusMonths(100)
    val firstDayOfWeek = firstDayOfWeekFromLocale()

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Implementar diálogo para añadir cita */ }) {
                Icon(Icons.Default.Add, contentDescription = "Agendar Cita")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Calendario horizontal
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(
                        day = day,
                        isSelected = selectedDate == day.date,
                        hasAppointment = appointments.any {
                            // Comprueba si la fecha de la cita coincide con el día del calendario
                            val appointmentDate = java.time.Instant.ofEpochMilli(it.appointment.appointmentDate)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            appointmentDate == day.date
                        }
                    ) { clickedDay ->
                        viewModel.onCalendarDateSelected(clickedDay.date)
                    }
                },
                monthHeader = { month ->
                    // Encabezado del mes con los días de la semana
                    val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
                    val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }
                    val year = month.yearMonth.year
                    MonthHeader(daysOfWeek = daysOfWeek, monthName = "$monthName $year")
                }
            )
            HorizontalDivider()

            // Lista de citas para el día seleccionado
            AppointmentList(appointments = appointments)
        }
    }
}

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>, monthName: String) {
    Column {
        Text(
            text = monthName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            // Muestra las iniciales de los días de la semana (L, M, M, etc.)
            for (dayOfWeek in daysOfWeek) {
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "ES")),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun Day(day: CalendarDay, isSelected: Boolean, hasAppointment: Boolean, onClick: (CalendarDay) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Celda cuadrada
            .padding(4.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                width = if (day.date == LocalDate.now()) 2.dp else 0.dp,
                color = if (day.date == LocalDate.now()) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick(day) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified
            )
            // Indicador visual si el día tiene una cita
            if (hasAppointment) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun AppointmentList(appointments: List<AppointmentWithDetails>) {
    if (appointments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay citas para el día seleccionado.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(appointments) { appointmentDetails ->
                AppointmentItem(appointmentDetails)
            }
        }
    }
}

@Composable
fun AppointmentItem(details: AppointmentWithDetails) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(details.pet.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Dueño: ${details.client.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(details.appointment.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

