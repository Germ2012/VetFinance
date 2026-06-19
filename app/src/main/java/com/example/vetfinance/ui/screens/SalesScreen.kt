package com.example.vetfinance.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vetfinance.R
import com.example.vetfinance.data.SaleDetailLine
import com.example.vetfinance.data.SaleListItem
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.SalesHistoryViewModel
import com.example.vetfinance.ui.components.HighVolumeModeToggle
import com.example.vetfinance.ui.components.HighVolumeSuggestion
import com.example.vetfinance.ui.utils.formatCurrency
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SalesScreen(
    navController: NavController,
    viewModel: SalesHistoryViewModel = hiltViewModel()
) {
    val sales = viewModel.salesPaginated.collectAsLazyPagingItems()
    LaunchedEffect(sales) {
        viewModel.pagingRefreshEvents.collect {
            sales.refresh()
        }
    }
    val salesSummary by viewModel.salesListSummary.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedSaleDateFilter.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var saleToDelete by remember { mutableStateOf<SaleListItem?>(null) }
    var secureSaleToDelete by remember { mutableStateOf<SaleListItem?>(null) }
    var selectedSale by remember { mutableStateOf<SaleListItem?>(null) }
    var highVolumeMode by rememberSaveable { mutableStateOf(true) }


    if (saleToDelete != null) {
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.confirm_delete_sale_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        secureSaleToDelete = saleToDelete
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete_button)) }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    secureSaleToDelete?.let { sale ->
        SecurityPinDialog(
            settings = appSettings,
            actionLabel = "eliminar venta",
            onDismiss = { secureSaleToDelete = null },
            onAuthorized = {
                viewModel.deleteSale(sale)
                secureSaleToDelete = null
            }
        )
    }

    selectedSale?.let { sale ->
        val detailFlow = remember(sale.saleId) { viewModel.getSaleDetailLines(sale.saleId) }
        val details by detailFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        SaleDetailSheet(
            sale = sale,
            details = details,
            onDismiss = { selectedSale = null },
            onDelete = {
                saleToDelete = sale
                selectedSale = null
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {

                            val localDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC")).toLocalDate()

                            val startOfDayMillis = localDate.atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()

                            viewModel.onSaleDateFilterSelected(startOfDayMillis)
                        } else {
                            viewModel.onSaleDateFilterSelected(null)
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.accept_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }


    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddSale.route) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.register_sale_fab))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.sales_history_title), style = MaterialTheme.typography.headlineMedium)
                FilterChip(
                    selected = selectedDate != null,
                    onClick = { showDatePicker = true },
                    label = {
                        val labelText = if (selectedDate != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            stringResource(R.string.date_filter_label, sdf.format(Date(selectedDate!!)))
                        } else {
                            stringResource(R.string.filter_by_date_chip)
                        }
                        Text(labelText)
                    },
                    trailingIcon = {
                        if (selectedDate != null) {
                            IconButton(onClick = { viewModel.clearSaleDateFilter() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_filter_content_description))
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SalesSummaryStrip(
                saleCount = salesSummary.salesCount,
                totalAmount = salesSummary.salesTotal,
                selectedDate = selectedDate
            )
            Spacer(modifier = Modifier.height(12.dp))
            HighVolumeModeToggle(
                enabled = highVolumeMode,
                onEnabledChange = { highVolumeMode = it }
            )
            if (!highVolumeMode) {
                Spacer(modifier = Modifier.height(8.dp))
                HighVolumeSuggestion(
                    itemCount = salesSummary.salesCount,
                    threshold = 1000,
                    onEnable = { highVolumeMode = true }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))


            if (sales.loadState.refresh is LoadState.Loading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(if (highVolumeMode) 0.dp else 8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(8, contentType = { "sale-placeholder" }) {
                        SaleListPlaceholder(highVolumeMode = highVolumeMode)
                    }
                }
            } else if (sales.itemCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val message = if (selectedDate != null) stringResource(R.string.no_sales_for_date)
                    else stringResource(R.string.no_sales_recorded)
                    SalesEmptyState(
                        message = message,
                        onAddClick = { navController.navigate(Screen.AddSale.route) }
                    )
                }
            } else {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(
                            count = sales.itemCount,
                            key = sales.itemKey { it.saleId },
                            contentType = { if (highVolumeMode) "sale-row-dense" else "sale-row" }
                        ) { index ->
                            val sale = sales[index]
                            if (sale != null) {
                                if (highVolumeMode) {
                                    SaleListRow(
                                        sale = sale,
                                        highVolumeMode = true,
                                        onOpen = { selectedSale = sale },
                                        onDelete = { saleToDelete = sale }
                                    )
                                } else {
                                    Column {
                                        SaleListRow(
                                            sale = sale,
                                            highVolumeMode = false,
                                            onOpen = { selectedSale = sale },
                                            onDelete = { saleToDelete = sale }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                                    }
                                }
                            } else {
                                SaleListPlaceholder(highVolumeMode = highVolumeMode)
                            }
                        }
                        if (sales.loadState.append is LoadState.Loading) {
                            item(contentType = "sale-placeholder") {
                                SaleListPlaceholder(highVolumeMode = highVolumeMode)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesSummaryStrip(
    saleCount: Int,
    totalAmount: Double,
    selectedDate: Long?
) {
    val periodText = if (selectedDate == null) {
        "Ventas de hoy"
    } else {
        val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        "Ventas del ${sdf.format(Date(selectedDate))}"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PointOfSale,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    periodText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$saleCount ventas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = stringResource(R.string.text_prefix_gs) + " " + formatCurrency(totalAmount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SalesEmptyState(
    message: String,
    onAddClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onAddClick, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.register_sale_fab))
            }
        }
    }
}

@Composable
private fun SaleListRow(
    sale: SaleListItem,
    highVolumeMode: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateText = remember(sale.date) { dateFormat.format(Date(sale.date)) }
    val clientText = sale.clientName ?: "Cliente general"
    val totalText = remember(sale.totalAmount) { "Gs. ${formatCurrency(sale.totalAmount)}" }
    val rowInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (highVolumeMode) 52.dp else 76.dp)
            .clickable(
                interactionSource = rowInteractionSource,
                indication = null,
                onClick = onOpen
            )
            .padding(vertical = if (highVolumeMode) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!highVolumeMode) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (highVolumeMode) 1.dp else 3.dp)
        ) {
            Text(
                if (highVolumeMode) clientText else dateText,
                style = if (highVolumeMode) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (highVolumeMode) "$dateText | ${sale.itemCount} items" else clientText,
                style = if (highVolumeMode) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!highVolumeMode) {
                Text(
                    "${sale.itemCount} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = totalText,
            style = if (highVolumeMode) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (!highVolumeMode) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_description_delete_sale))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaleDetailSheet(
    sale: SaleListItem,
    details: List<SaleDetailLine>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Detalle de venta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        dateFormat.format(Date(sale.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        sale.clientName ?: "Cliente general",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_description_delete_sale))
                }
            }
            HorizontalDivider()
            if (details.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_details_available),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = details,
                        key = { it.crossRefId },
                        contentType = { "sale-detail-line" }
                    ) { detail ->
                        SaleDetailRow(detail = detail)
                    }
                }
            }
            Text(
                text = stringResource(R.string.sale_item_total_prefix, formatCurrency(sale.totalAmount)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun SaleDetailRow(detail: SaleDetailLine) {
    val isDoseSale = detail.overridePrice != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                detail.productName ?: "Producto eliminado",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )


            if (isDoseSale && !detail.notes.isNullOrBlank()) {
                Text(
                    text = "\"${detail.notes}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }


            if (!isDoseSale) {
                Text(
                    text = stringResource(
                        R.string.sale_detail_item_quantity_prefix,
                        formatCurrency(detail.quantitySold).removeSuffix(".00").removeSuffix(",00")
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        val priceToShow = detail.overridePrice ?: (detail.priceAtTimeOfSale * detail.quantitySold)
        Text(
            text = formatCurrency(priceToShow),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SaleListPlaceholder(highVolumeMode: Boolean = false) {
    if (highVolumeMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Cargando venta...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Cargando venta...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
