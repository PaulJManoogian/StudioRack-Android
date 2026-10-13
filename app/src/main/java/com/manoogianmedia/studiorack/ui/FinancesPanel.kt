package com.manoogianmedia.studiorack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manoogianmedia.studiorack.data.CachedRecord
import com.manoogianmedia.studiorack.data.SupportingRecord
import org.json.JSONObject
import java.time.LocalDate

private enum class FinanceEditor { INCOME, EXPENSE, MILEAGE, CONTRACT, CLOSEOUT }

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FinancesPanel(model: StudioRackViewModel) {
    val events by model.events.collectAsState()
    val income by model.incomeEntries.collectAsState()
    val expenses by model.expenseEntries.collectAsState()
    val mileage by model.mileageEntries.collectAsState()
    val contracts by model.eventContracts.collectAsState()
    val templates by model.contractTemplates.collectAsState()
    val closeouts by model.eventCloseouts.collectAsState()
    val gearReturns by model.eventGearReturns.collectAsState()
    val items by model.items.collectAsState()
    var tab by remember { mutableStateOf("Overview") }
    var editor by remember { mutableStateOf<FinanceEditor?>(null) }
    var editing by remember { mutableStateOf<CachedRecord?>(null) }
    val eventNames = events.associate { it.entityId to financeJson(it).optString("title", "Scheduled event") }
    val expected = income.sumOf { financeJson(it).optLong("expected_cents") }
    val received = income.sumOf { financeJson(it).optLong("received_cents") }
    val spent = expenses.sumOf { financeJson(it).optLong("amount_cents") }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Finances", color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Track gig money, travel, agreements, and closeout from the same synchronized records as the web workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FinanceTabs(tab) { tab = it }
        when (tab) {
            "Overview" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FinanceMetric("Expected", expected, Modifier.weight(1f))
                    FinanceMetric("Received", received, Modifier.weight(1f))
                    FinanceMetric("Costs", spent, Modifier.weight(1f))
                }
                FinanceMetric("Recorded cash net", received - spent, Modifier.fillMaxWidth())
                val unfinished = events.filter { event ->
                    val row = financeJson(event)
                    val dueForCloseout = row.optString("event_status") in setOf("ended", "completed", "cancelled") ||
                        row.optString("event_date").let { it.isNotBlank() && it <= LocalDate.now().toString() }
                    dueForCloseout && closeouts.none { it.entityId == event.entityId && financeJson(it).optString("status") == "complete" }
                }
                Text("EVENT CLOSEOUT", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                unfinished.sortedByDescending { financeJson(it).optString("event_date") }.take(8).forEach { event ->
                    FinanceRecordCard(
                        financeJson(event).optString("title"),
                        financeJson(event).optString("event_date").ifBlank { "Date not set" },
                        "Needs review",
                        "Close Out",
                    ) { editing = event; editor = FinanceEditor.CLOSEOUT }
                }
            }
            "Income" -> FinanceRecordList(income, "Add Income", { editing = null; editor = FinanceEditor.INCOME }, { editing = it; editor = FinanceEditor.INCOME }) { record ->
                val row = financeJson(record)
                Triple(row.optString("description"), "${eventNames[row.optString("event_id")].orEmpty().ifBlank { "Standalone" }} · ${row.optString("status", "expected").replace('_', ' ')}", money(row.optLong("received_cents")) + " / " + money(row.optLong("expected_cents")))
            }
            "Expenses" -> FinanceRecordList(expenses, "Add Expense", { editing = null; editor = FinanceEditor.EXPENSE }, { editing = it; editor = FinanceEditor.EXPENSE }) { record ->
                val row = financeJson(record)
                Triple(row.optString("description"), "${eventNames[row.optString("event_id")].orEmpty().ifBlank { "Standalone" }} · ${row.optString("category", "other")}", money(row.optLong("amount_cents")))
            }
            "Mileage" -> FinanceRecordList(mileage, "Add Trip", { editing = null; editor = FinanceEditor.MILEAGE }, { editing = it; editor = FinanceEditor.MILEAGE }) { record ->
                val row = financeJson(record)
                Triple("${row.optDouble("distance_value")} ${row.optString("distance_unit", "mi")}", eventNames[row.optString("event_id")].orEmpty().ifBlank { "Standalone trip" }, row.optString("trip_date"))
            }
            "Contracts" -> FinanceRecordList(contracts, "Add Contract", { editing = null; editor = FinanceEditor.CONTRACT }, { editing = it; editor = FinanceEditor.CONTRACT }) { record ->
                val row = financeJson(record)
                Triple(row.optString("title"), "${row.optString("counterparty_name")} · ${row.optString("status", "draft")}", money(row.optLong("guarantee_cents")))
            }
            "Closeout" -> events.sortedByDescending { financeJson(it).optString("event_date") }.forEach { event ->
                val closeout = closeouts.firstOrNull { it.entityId == event.entityId }
                FinanceRecordCard(financeJson(event).optString("title"), financeJson(event).optString("event_date"), closeout?.let { financeJson(it).optString("status").replace('_', ' ') } ?: "Not started", "Review") {
                    editing = event; editor = FinanceEditor.CLOSEOUT
                }
            }
        }
    }

    editor?.let { mode ->
        val original = when (mode) {
            FinanceEditor.CLOSEOUT -> closeouts.firstOrNull { it.entityId == editing?.entityId }?.let(::financeJson) ?: JSONObject()
            else -> editing?.let(::financeJson) ?: JSONObject()
        }
        FinanceRecordDialog(mode, editing, original, events, templates, gearReturns, items, model) { editor = null; editing = null }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FinanceTabs(selected: String, choose: (String) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Overview", "Income", "Expenses", "Mileage", "Contracts", "Closeout").forEach { label ->
            Surface(
                color = if (selected == label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (selected == label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                onClick = { choose(label) },
            ) { Text(label, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = if (selected == label) Color(0xFF0A0D15) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun FinanceMetric(label: String, cents: Long, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(12.dp)) { Text(label.uppercase(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black); Text(money(cents), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun FinanceRecordList(
    records: List<CachedRecord>,
    addLabel: String,
    add: () -> Unit,
    edit: (CachedRecord) -> Unit,
    labels: (CachedRecord) -> Triple<String, String, String>,
) {
    TextButton(onClick = add, modifier = Modifier.fillMaxWidth()) { Text(addLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) }
    records.sortedByDescending { financeJson(it).optString("created_utc") }.forEach { record ->
        val (title, detail, value) = labels(record)
        FinanceRecordCard(title, detail, value, "Edit") { edit(record) }
    }
    if (records.isEmpty()) Text("Nothing has been recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FinanceRecordCard(title: String, detail: String, value: String, actionLabel: String, action: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) { Text(title.ifBlank { "Untitled" }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(value, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            TextButton(onClick = action) { Text(actionLabel) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FinanceRecordDialog(
    mode: FinanceEditor,
    record: CachedRecord?,
    original: JSONObject,
    events: List<CachedRecord>,
    templates: List<CachedRecord>,
    gearReturns: List<CachedRecord>,
    items: List<SupportingRecord>,
    model: StudioRackViewModel,
    close: () -> Unit,
) {
    val selectedEvent = if (mode == FinanceEditor.CLOSEOUT) record?.entityId.orEmpty() else original.optString("event_id")
    var eventId by remember(record, mode) { mutableStateOf(selectedEvent) }
    var description by remember(record, mode) { mutableStateOf(original.optString(if (mode == FinanceEditor.CONTRACT) "title" else "description")) }
    var amount by remember(record, mode) { mutableStateOf(when (mode) { FinanceEditor.INCOME -> original.optLong("expected_cents"); FinanceEditor.EXPENSE -> original.optLong("amount_cents"); FinanceEditor.CONTRACT -> original.optLong("guarantee_cents"); else -> 0L }.takeIf { it > 0 }?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var receivedAmount by remember(record, mode) { mutableStateOf(original.optLong("received_cents").takeIf { it > 0 }?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var secondary by remember(record, mode) { mutableStateOf(when (mode) { FinanceEditor.INCOME -> original.optString("payer_name"); FinanceEditor.EXPENSE -> original.optString("vendor"); FinanceEditor.MILEAGE -> original.optString("purpose"); FinanceEditor.CONTRACT -> original.optString("counterparty_name"); else -> original.optString("outcome_notes") }) }
    var extra by remember(record, mode) { mutableStateOf(when (mode) { FinanceEditor.MILEAGE -> original.optDouble("distance_value").takeIf { it > 0 }?.toString().orEmpty(); FinanceEditor.CONTRACT -> original.optString("issuer_name"); else -> original.optString("notes") }) }
    var category by remember(record, mode) { mutableStateOf(original.optString("category", "other")) }
    var recordDate by remember(record, mode) { mutableStateOf(original.optString("expense_date", LocalDate.now().toString())) }
    var startLocation by remember(record, mode) { mutableStateOf(original.optString("start_location")) }
    var endLocation by remember(record, mode) { mutableStateOf(original.optString("end_location")) }
    var distanceUnit by remember(record, mode) { mutableStateOf(original.optString("distance_unit", "mi")) }
    var parking by remember(record, mode) { mutableStateOf(original.optInt("parking_cents").takeIf { it > 0 }?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var tolls by remember(record, mode) { mutableStateOf(original.optInt("tolls_cents").takeIf { it > 0 }?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var body by remember(record, mode) { mutableStateOf(original.optString("body_text")) }
    var paymentTerms by remember(record, mode) { mutableStateOf(original.optString("payment_terms")) }
    var cancellationTerms by remember(record, mode) { mutableStateOf(original.optString("cancellation_terms")) }
    var contractType by remember(record, mode) { mutableStateOf(original.optString("contract_type", "performance")) }
    var issuerRole by remember(record, mode) { mutableStateOf(original.optString("issuer_role", "performer")) }
    var status by remember(record, mode) { mutableStateOf(original.optString("status", if (mode == FinanceEditor.CLOSEOUT) "in_progress" else if (mode == FinanceEditor.CONTRACT) "draft" else "expected")) }
    var eventMenu by remember { mutableStateOf(false) }
    val title = when (mode) { FinanceEditor.INCOME -> "Gig Income"; FinanceEditor.EXPENSE -> "Expense"; FinanceEditor.MILEAGE -> "Mileage"; FinanceEditor.CONTRACT -> "Contract"; FinanceEditor.CLOSEOUT -> "Event Closeout" }
    val closeoutHasPendingGear = mode == FinanceEditor.CLOSEOUT && gearReturns.any {
        val row = financeJson(it)
        row.optString("event_id") == record?.entityId && row.optString("return_status", "pending") == "pending"
    }
    val canSave = when (mode) {
        FinanceEditor.CONTRACT -> description.isNotBlank() && extra.isNotBlank() && secondary.isNotBlank() && body.isNotBlank()
        FinanceEditor.CLOSEOUT -> status != "complete" || !closeoutHasPendingGear
        else -> true
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mode != FinanceEditor.CLOSEOUT) EventPicker(events, eventId, eventMenu, { eventMenu = it }) { eventId = it }
            when (mode) {
                FinanceEditor.MILEAGE -> {
                    OutlinedTextField(description, { description = it.take(10) }, label = { Text("Date (YYYY-MM-DD)") }, placeholder = { Text(LocalDate.now().toString()) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(extra, { extra = decimalInput(it) }, label = { Text("Distance") }, modifier = Modifier.fillMaxWidth())
                    SimpleStatus(listOf("mi", "km"), distanceUnit) { distanceUnit = it }
                    OutlinedTextField(startLocation, { startLocation = it.take(300) }, label = { Text("From") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(endLocation, { endLocation = it.take(300) }, label = { Text("To") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(secondary, { secondary = it.take(500) }, label = { Text("Purpose") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(parking, { parking = decimalInput(it) }, label = { Text("Parking") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(tolls, { tolls = decimalInput(it) }, label = { Text("Tolls") }, modifier = Modifier.weight(1f))
                    }
                }
                FinanceEditor.CONTRACT -> {
                    val currentEvent = events.firstOrNull { it.entityId == eventId }?.let(::financeJson)
                    val activeTemplates = templates.filter { financeJson(it).optInt("active", 1) != 0 }
                    if (activeTemplates.isNotEmpty()) {
                        Text("START FROM A TEMPLATE", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            activeTemplates.forEach { templateRecord ->
                                val template = financeJson(templateRecord)
                                TextButton(onClick = {
                                    contractType = template.optString("contract_type", "performance")
                                    issuerRole = template.optString("issuer_role", "performer")
                                    description = contractTemplateText(template.optString("title_template"), currentEvent, extra, secondary)
                                    body = contractTemplateText(template.optString("body_template"), currentEvent, extra, secondary)
                                    paymentTerms = template.optString("default_payment_terms")
                                    cancellationTerms = template.optString("default_cancellation_terms")
                                }) { Text(template.optString("name", "Template")) }
                            }
                        }
                    }
                    OutlinedTextField(description, { description = it.take(300) }, label = { Text("Contract title") }, modifier = Modifier.fillMaxWidth())
                    Text("AGREEMENT TYPE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    SimpleStatus(listOf("performance", "touring", "festival", "rental", "production", "other"), contractType) { contractType = it }
                    Text("ISSUED AS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    SimpleStatus(listOf("performer", "touring_organization", "festival_organizer", "venue", "studio", "other"), issuerRole) { issuerRole = it }
                    OutlinedTextField(extra, { extra = it.take(250) }, label = { Text("Issuer") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(secondary, { secondary = it.take(250) }, label = { Text("Other party") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("Guarantee") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it.take(50000) }, label = { Text("Contract language") }, minLines = 7, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(paymentTerms, { paymentTerms = it.take(5000) }, label = { Text("Payment terms") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cancellationTerms, { cancellationTerms = it.take(5000) }, label = { Text("Cancellation terms") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    SimpleStatus(listOf("draft", "offered", "accepted", "signed", "declined", "void"), status) { status = it }
                }
                FinanceEditor.CLOSEOUT -> {
                    Text(financeJson(record!!).optString("title"), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    SimpleStatus(listOf("in_progress", "complete"), status) { status = it }
                    OutlinedTextField(secondary, { secondary = it.take(5000) }, label = { Text("Outcome and anything needing attention") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Text("GEAR RETURN", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    val eventReturns = gearReturns.filter { financeJson(it).optString("event_id") == record.entityId }
                    if (eventReturns.isEmpty()) {
                        Text("No assigned-kit items need reconciliation.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        eventReturns.forEach { gearReturn ->
                            GearReturnEditor(gearReturn, items, model)
                        }
                    }
                    Text("Complete requires every gear item to be reviewed along with money, travel, and documents.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                else -> {
                    OutlinedTextField(description, { description = it.take(300) }, label = { Text(if (mode == FinanceEditor.INCOME) "Income description" else "Expense description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text(if (mode == FinanceEditor.INCOME) "Expected" else "Amount") }, modifier = Modifier.fillMaxWidth())
                    if (mode == FinanceEditor.INCOME) {
                        OutlinedTextField(receivedAmount, { receivedAmount = decimalInput(it) }, label = { Text("Received") }, modifier = Modifier.fillMaxWidth())
                        SimpleStatus(listOf("expected", "requested", "partial", "received", "waived", "cancelled"), status) { status = it }
                    } else {
                        OutlinedTextField(recordDate, { recordDate = it.take(10) }, label = { Text("Expense date") }, modifier = Modifier.fillMaxWidth())
                        Text("CATEGORY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        SimpleStatus(listOf("travel", "parking", "tolls", "lodging", "meals", "rental", "personnel", "supplies", "production", "fees", "other"), category) { category = it }
                    }
                    OutlinedTextField(secondary, { secondary = it.take(200) }, label = { Text(if (mode == FinanceEditor.INCOME) "Payer" else "Vendor") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(extra, { extra = it.take(4000) }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            }
        } },
        confirmButton = { TextButton(enabled = canSave, onClick = {
            when (mode) {
                FinanceEditor.INCOME -> model.saveFinanceRecord("event_income", record?.entityId, JSONObject(original.toString()).put("event_id", nullableId(eventId)).put("description", description.trim()).put("payer_name", secondary.trim()).put("expected_cents", cents(amount)).put("received_cents", cents(receivedAmount)).put("status", status).put("notes", extra.trim()), close)
                FinanceEditor.EXPENSE -> model.saveFinanceRecord("event_expense", record?.entityId, JSONObject(original.toString()).put("event_id", nullableId(eventId)).put("description", description.trim()).put("vendor", secondary.trim()).put("category", category).put("expense_date", recordDate.ifBlank { LocalDate.now().toString() }).put("amount_cents", cents(amount)).put("reimbursable", original.optInt("reimbursable")).put("reimbursement_status", original.optString("reimbursement_status", "not_applicable")).put("notes", extra.trim()), close)
                FinanceEditor.MILEAGE -> model.saveMileageEntry(record?.entityId, JSONObject(original.toString()).put("event_id", nullableId(eventId)).put("trip_date", description.ifBlank { LocalDate.now().toString() }).put("start_location", startLocation.trim()).put("end_location", endLocation.trim()).put("distance_value", extra.toDoubleOrNull() ?: 0.0).put("distance_unit", distanceUnit).put("purpose", secondary.trim()).put("parking_cents", cents(parking)).put("tolls_cents", cents(tolls)).put("reimbursement_rate_micros", original.optLong("reimbursement_rate_micros")).put("reimbursement_cents", original.optInt("reimbursement_cents")).put("notes", original.optString("notes")), close)
                FinanceEditor.CONTRACT -> model.saveFinanceRecord("event_contract", record?.entityId, JSONObject(original.toString()).put("event_id", nullableId(eventId)).put("title", description.trim()).put("contract_type", contractType).put("issuer_role", issuerRole).put("issuer_name", extra.trim()).put("counterparty_type", original.optString("counterparty_type", "other")).put("counterparty_name", secondary.trim()).put("status", status).put("guarantee_cents", cents(amount)).put("deposit_cents", original.optLong("deposit_cents")).put("balance_due_cents", original.optLong("balance_due_cents")).put("payment_terms", paymentTerms.trim()).put("cancellation_terms", cancellationTerms.trim()).put("body_text", body.trim()), close)
                FinanceEditor.CLOSEOUT -> model.saveFinanceRecord("event_closeout", record?.entityId, JSONObject(original.toString()).put("event_id", record!!.entityId).put("status", status).put("outcome", original.optString("outcome", "completed")).put("outcome_notes", secondary.trim()).put("gear_confirmed", if (status == "complete") 1 else original.optInt("gear_confirmed")).put("finances_confirmed", if (status == "complete") 1 else original.optInt("finances_confirmed")).put("documents_confirmed", if (status == "complete") 1 else original.optInt("documents_confirmed")), close)
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = close) { Text("Cancel") } },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GearReturnEditor(record: CachedRecord, items: List<SupportingRecord>, model: StudioRackViewModel) {
    val original = financeJson(record)
    val itemName = items.firstOrNull { it.entityId == original.optString("item_id") }
        ?.let { runCatching { JSONObject(it.json).optString("display_name") }.getOrNull() }
        .orEmpty().ifBlank { "Equipment item" }
    val expected = original.optInt("expected_quantity", 1).coerceAtLeast(1)
    var status by remember(record.json) { mutableStateOf(original.optString("return_status", "pending")) }
    var quantity by remember(record.json) { mutableStateOf(original.optInt("returned_quantity").toString()) }
    var notes by remember(record.json) { mutableStateOf(original.optString("notes")) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(itemName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("Expected $expected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            SimpleStatus(listOf("returned_ready", "needs_maintenance", "damaged", "missing", "left_elsewhere", "awaiting_return"), status) {
                status = it
                quantity = if (it in setOf("returned_ready", "needs_maintenance", "damaged")) expected.toString() else "0"
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).take(4) }, label = { Text("Returned") }, modifier = Modifier.weight(0.34f))
                OutlinedTextField(notes, { notes = it.take(2000) }, label = { Text("Condition or location") }, modifier = Modifier.weight(0.66f))
            }
            TextButton(onClick = {
                model.saveFinanceRecord(
                    "event_gear_return",
                    record.entityId,
                    JSONObject(original.toString())
                        .put("returned_quantity", (quantity.toIntOrNull() ?: 0).coerceIn(0, expected))
                        .put("return_status", status)
                        .put("notes", notes.trim()),
                )
            }, modifier = Modifier.fillMaxWidth()) { Text("Save Gear Status", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EventPicker(events: List<CachedRecord>, selected: String, expanded: Boolean, setExpanded: (Boolean) -> Unit, choose: (String) -> Unit) {
    Column { TextButton(onClick = { setExpanded(true) }, modifier = Modifier.fillMaxWidth()) { Text(events.firstOrNull { it.entityId == selected }?.let { financeJson(it).optString("title") } ?: "No linked event") }; DropdownMenu(expanded, { setExpanded(false) }) { DropdownMenuItem({ Text("No linked event") }, onClick = { choose(""); setExpanded(false) }); events.sortedByDescending { financeJson(it).optString("event_date") }.forEach { event -> DropdownMenuItem({ Text("${financeJson(event).optString("event_date")} · ${financeJson(event).optString("title")}") }, onClick = { choose(event.entityId); setExpanded(false) }) } } }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SimpleStatus(options: List<String>, selected: String, choose: (String) -> Unit) { FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { options.forEach { value -> TextButton(onClick = { choose(value) }) { Text(value.replace('_', ' ').replaceFirstChar(Char::uppercase), color = if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected == value) FontWeight.Black else FontWeight.Normal) } } } }

private fun financeJson(record: CachedRecord): JSONObject = runCatching { JSONObject(record.json) }.getOrDefault(JSONObject()).put("id", record.entityId)
private fun money(cents: Long) = "$${"%.2f".format(cents / 100.0)}"
private fun cents(value: String) = ((value.toDoubleOrNull() ?: 0.0) * 100).toLong().coerceAtLeast(0)
private fun decimalInput(value: String) = value.filter { it.isDigit() || it == '.' }.take(12)
private fun nullableId(value: String): Any = value.ifBlank { JSONObject.NULL }

private fun contractTemplateText(template: String, event: JSONObject?, issuer: String, counterparty: String): String = template
    .replace("{{issuer_name}}", issuer)
    .replace("{{counterparty_name}}", counterparty)
    .replace("{{event_title}}", event?.optString("title").orEmpty())
    .replace("{{event_date}}", event?.optString("event_date").orEmpty())
    .replace("{{location}}", event?.optString("location").orEmpty())
