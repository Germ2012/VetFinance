package com.example.vetfinance.ui.screens

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vetfinance.R
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.viewmodel.VetViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.VerticalCalendar
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
    val showDialog by viewModel.showAddAppointmentDialog.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val petsWithOwners by viewModel.petsWithOwners.collectAsState()

    if (showDialog) {
        AddAppointmentDialog(
            clients = clients,
            petsWithOwners = petsWithOwners,
            selectedDate = selectedDate,
            onDismiss = { viewModel.onDismissAddAppointmentDialog() },
            onConfirm = {
                viewModel.addAppointment(it)
                viewModel.onDismissAddAppointmentDialog()
            }
        )
    }

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

    // AÑADIDO: Detectar la orientación del dispositivo
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddAppointmentDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_schedule_appointment))
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // CORREGIDO: Se elige el tipo de calendario según la orientación
            if (isLandscape) {
                // Calendario Vertical para modo horizontal
                VerticalCalendar(
                    modifier = Modifier.weight(1f),
                    state = state,
                    dayContent = { day -> Day(day, selectedDate == day.date) { viewModel.onCalendarDateSelected(it.date) } },
                    monthHeader = { month ->
                        val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
                        val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.uppercase() }
                        MonthHeader(daysOfWeek = daysOfWeek, monthName = "$monthName ${month.yearMonth.year}")
                    }
                )
            } else {
                // Calendario Horizontal para modo vertical
                Column(modifier = Modifier.weight(1f)) {
                    HorizontalCalendar(
                        state = state,
                        dayContent = { day -> Day(day, selectedDate == day.date) { viewModel.onCalendarDateSelected(it.date) } },
                        monthHeader = { month ->
                            val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
                            val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.uppercase() }
                            MonthHeader(daysOfWeek = daysOfWeek, monthName = "$monthName ${month.yearMonth.year}")
                        }
                    )
                }
            }

            // La lista de citas ahora ocupa el espacio restante o toda la pantalla si es necesario
            Column(modifier = Modifier.weight(1f)) {
                HorizontalDivider()
                AppointmentList(appointments = appointments)
            }
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
            for (dayOfWeek in daysOfWeek) {
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun Day(day: CalendarDay, isSelected: Boolean, onClick: (CalendarDay) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.7f else 1f) // Ajusta el aspect ratio en landscape
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
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified
        )
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
            Text(stringResource(R.string.no_appointments_for_selected_day))
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
            Text(stringResource(R.string.owner_label, details.client.name), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(details.appointment.description ?: "", style = MaterialTheme.typography.bodyLarge)
        }
    }
}