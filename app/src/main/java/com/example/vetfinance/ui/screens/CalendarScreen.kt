package com.example.vetfinance.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vetfinance.R
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.data.Supplier
import com.example.vetfinance.data.SupplierDebtWithSupplier
import com.example.vetfinance.viewmodel.VetViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ui.utils.NumberTransformation
import ui.utils.formatCurrency

@Composable
fun CalendarScreen(viewModel: VetViewModel) {
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val appointments by viewModel.appointmentsOnSelectedDate.collectAsState()
    val supplierDebts by viewModel.supplierDebtsOnSelectedDate.collectAsState()
    val showDialog by viewModel.showAddAppointmentDialog.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val petsWithOwners by viewModel.petsWithOwners.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }
    var showSupplierDebtDialog by remember { mutableStateOf(false) }

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

    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            title = { Text(stringResource(R.string.calendar_add_menu_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.onShowAddAppointmentDialog()
                            showAddMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.calendar_add_appointment_option)) }
                    Button(
                        onClick = {
                            showSupplierDebtDialog = true
                            showAddMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.calendar_add_supplier_debt_option)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    if (showSupplierDebtDialog) {
        AddSupplierDebtDialog(
            suppliers = suppliers,
            selectedDate = selectedDate,
            onDismiss = { showSupplierDebtDialog = false },
            onConfirm = { supplierId, description, amount, dueDate, note ->
                viewModel.addSupplierDebt(supplierId, description, amount, dueDate, note)
                showSupplierDebtDialog = false
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
            FloatingActionButton(onClick = { showAddMenu = true }) {
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
                    AppointmentList(appointments = appointments, supplierDebts = supplierDebts, onMarkSupplierDebtPaid = { viewModel.markSupplierDebtAsPaid(it) })
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
                    AppointmentList(appointments = appointments, supplierDebts = supplierDebts, onMarkSupplierDebtPaid = { viewModel.markSupplierDebtAsPaid(it) })
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
    supplierDebts: List<SupplierDebtWithSupplier>,
    onMarkSupplierDebtPaid: (String) -> Unit
) {
    Column {
        HorizontalDivider()
        if (appointments.isEmpty() && supplierDebts.isEmpty()) {
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
                if (supplierDebts.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.calendar_supplier_debts_title), style = MaterialTheme.typography.titleMedium)
                    }
                    items(supplierDebts, key = { it.debtId }) { debt ->
                        SupplierDebtItem(debt, onMarkSupplierDebtPaid)
                    }
                }
                if (appointments.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.calendar_appointments_title), style = MaterialTheme.typography.titleMedium)
                    }
                }
                items(appointments) { appointmentDetails ->
                    AppointmentItem(appointmentDetails)
                }
            }
        }
    }
}

@Composable
fun SupplierDebtItem(
    debt: SupplierDebtWithSupplier,
    onMarkPaid: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (debt.isPaid) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(debt.supplierName ?: stringResource(R.string.label_no_supplier), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(debt.description)
            Text(stringResource(R.string.supplier_debt_amount_label, formatCurrency(debt.amount)))
            if (!debt.isPaid) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onMarkPaid(debt.debtId) }) {
                    Text(stringResource(R.string.supplier_debt_mark_paid_button))
                }
            } else {
                Text(stringResource(R.string.supplier_debt_paid_label))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierDebtDialog(
    suppliers: List<Supplier>,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (supplierId: String?, description: String, amount: Double, dueDate: Long, note: String?) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var supplierExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDate by remember(selectedDate) { mutableStateOf(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        dueDate = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.accept_button)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.supplier_debt_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = !supplierExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: stringResource(R.string.label_select_supplier_optional),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_supplier)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(supplierExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = supplierExpanded, onDismissRequest = { supplierExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_no_supplier)) },
                            onClick = {
                                selectedSupplier = null
                                supplierExpanded = false
                            }
                        )
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.name) },
                                onClick = {
                                    selectedSupplier = supplier
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.supplier_debt_description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.supplier_debt_amount_input_label)) },
                    prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = NumberTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.supplier_debt_due_date_label), modifier = Modifier.weight(1f))
                    Text(sdf.format(Date(dueDate)))
                    TextButton(onClick = { showDatePicker = true }) { Text(stringResource(R.string.select_date_content_description)) }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.debt_adjustment_note_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedSupplier?.supplierId, description, amount.toDoubleOrNull() ?: 0.0, dueDate, note.ifBlank { null })
                },
                enabled = description.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text(stringResource(R.string.save_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}

@Composable
fun AppointmentItem(details: AppointmentWithDetails) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(details.pet.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(stringResource(R.string.owner_label, details.client.name), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            if (!details.appointment.description.isNullOrBlank()) {
                Text(details.appointment.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
