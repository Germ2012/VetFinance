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
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.vetfinance.data.Appointment
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.viewmodel.VetViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: VetViewModel) {
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val appointments by viewModel.appointmentsOnSelectedDate.collectAsState()
    val showDialog by viewModel.showAddAppointmentDialog.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val petsWithOwners by viewModel.petsWithOwners.collectAsState()
    var appointmentToEdit by remember { mutableStateOf<AppointmentWithDetails?>(null) }
    var appointmentToReschedule by remember { mutableStateOf<AppointmentWithDetails?>(null) }
    var appointmentToDelete by remember { mutableStateOf<AppointmentWithDetails?>(null) }

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

    appointmentToEdit?.let { details ->
        EditAppointmentDialog(
            appointment = details.appointment,
            onDismiss = { appointmentToEdit = null },
            onConfirm = { description ->
                val cleanDescription = description.trim().takeIf { it.isNotEmpty() }
                viewModel.updateAppointment(details.appointment.copy(description = cleanDescription))
                appointmentToEdit = null
            }
        )
    }

    appointmentToReschedule?.let { details ->
        RescheduleAppointmentDialog(
            details = details,
            onDismiss = { appointmentToReschedule = null },
            onConfirm = { newDateMillis ->
                viewModel.updateAppointment(details.appointment.copy(appointmentDate = newDateMillis))
                viewModel.onCalendarDateSelected(
                    Instant.ofEpochMilli(newDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                )
                appointmentToReschedule = null
            }
        )
    }

    appointmentToDelete?.let { details ->
        AlertDialog(
            onDismissRequest = { appointmentToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_appointment_title)) },
            text = {
                Text(stringResource(R.string.confirm_delete_appointment_message, details.pet.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAppointment(details.appointment)
                        appointmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddAppointmentDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_schedule_appointment))
            }
        }
    ) { paddingValues ->
        // --- LÓGICA DE DISEÑO INTEGRADA ---
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                VerticalCalendar(
                    modifier = Modifier.weight(1f), // Ocupa la mitad del ancho
                    state = state,
                    dayContent = { day -> Day(day, selectedDate == day.date) { viewModel.onCalendarDateSelected(it.date) } },
                    monthHeader = { month ->
                        val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
                        val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.uppercase() }
                        MonthHeader(daysOfWeek = daysOfWeek, monthName = "$monthName ${month.yearMonth.year}")
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    AppointmentList(
                        appointments = appointments,
                        onEdit = { appointmentToEdit = it },
                        onReschedule = { appointmentToReschedule = it },
                        onDelete = { appointmentToDelete = it }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                HorizontalCalendar(
                    modifier = Modifier.weight(1f),
                    state = state,
                    dayContent = { day -> Day(day, selectedDate == day.date) { viewModel.onCalendarDateSelected(it.date) } },
                    monthHeader = { month ->
                        val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
                        val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.uppercase() }
                        MonthHeader(daysOfWeek = daysOfWeek, monthName = "$monthName ${month.yearMonth.year}")
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    AppointmentList(
                        appointments = appointments,
                        onEdit = { appointmentToEdit = it },
                        onReschedule = { appointmentToReschedule = it },
                        onDelete = { appointmentToDelete = it }
                    )
                }
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
            .aspectRatio(1f)
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
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppointmentList(
    appointments: List<AppointmentWithDetails>,
    onEdit: (AppointmentWithDetails) -> Unit,
    onReschedule: (AppointmentWithDetails) -> Unit,
    onDelete: (AppointmentWithDetails) -> Unit
) {
    Column {
        HorizontalDivider()
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(appointments, key = { it.appointment.appointmentId }) { appointmentDetails ->
                    AppointmentItem(
                        details = appointmentDetails,
                        onEdit = onEdit,
                        onReschedule = onReschedule,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentItem(
    details: AppointmentWithDetails,
    onEdit: (AppointmentWithDetails) -> Unit,
    onReschedule: (AppointmentWithDetails) -> Unit,
    onDelete: (AppointmentWithDetails) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(details.pet.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(stringResource(R.string.owner_label, details.client.name), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                if (!details.appointment.description.isNullOrBlank()) {
                    Text(details.appointment.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.appointment_options_content_description)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.appointment_edit_menu)) },
                        onClick = {
                            menuExpanded = false
                            onEdit(details)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.appointment_reschedule_menu)) },
                        onClick = {
                            menuExpanded = false
                            onReschedule(details)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.appointment_delete_menu)) },
                        onClick = {
                            menuExpanded = false
                            onDelete(details)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditAppointmentDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var description by remember(appointment.appointmentId) {
        mutableStateOf(appointment.description.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_appointment_dialog_title)) },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.add_appointment_description_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(description) },
                enabled = description.isNotBlank()
            ) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleAppointmentDialog(
    details: AppointmentWithDetails,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = details.appointment.appointmentDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis ?: return@TextButton
                    val selectedDate = Instant.ofEpochMilli(selectedMillis)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate()
                    val newDateMillis = selectedDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onConfirm(newDateMillis)
                }
            ) {
                Text(stringResource(R.string.accept_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(R.string.reschedule_appointment_dialog_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            }
        )
    }
}
