package com.gal101.foodexpirytracker.ui.screens.addproduct

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.* // Importează tot din layout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Importează toate iconițele filled
import androidx.compose.material3.* // Importează tot din material3
import androidx.compose.runtime.* // Importează tot din runtime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gal101.foodexpirytracker.productDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalLayoutApi::class
) // Adaugă ExperimentalLayoutApi pentru FlowRow
@Composable
fun AddDetailsScreen(
        // Parametrii pentru a reține starea anterioară
        name: String,
        unit: String,
        count: Int,
        // Callback-ul pasează toate informațiile selectate
        onNext: (name: String, unit: String, quantity: Double) -> Unit,
        onBack: () -> Unit = {}
) {
        val context = LocalContext.current
        val viewModel: ProductNameViewModel = viewModel()

        // Inițializează DataStore
        LaunchedEffect(Unit) { viewModel.initDataStore(context.productDataStore) }

        // Observă template-urile din ViewModel
        val templates by viewModel.templates.collectAsState()

        // --- Inițializarea Stării ---
        // Încearcă să găsești template-ul care corespunde parametrilor primiți
        // Match by name, unit, and location. Quantity might vary, but for "Edit" flow we might want
        // to
        // find exact match?
        // For now, flexible matching.
        val initialTemplate =
                remember(templates, name, unit) {
                        templates.find { it.name == name && it.unit == unit }
                }
        // Starea pentru template-ul selectat
        var selectedTemplate by remember { mutableStateOf(initialTemplate) }
        // --- Sfârșitul Inițializării Stării ---

        val scope = rememberCoroutineScope()
        var showAddDialog by remember { mutableStateOf(false) }

        // --- Logica Selecției Implicite ---
        LaunchedEffect(templates, initialTemplate) {
                if (templates.isNotEmpty() && initialTemplate == null && selectedTemplate == null) {
                        selectedTemplate = templates[0]
                } else if (templates.isEmpty()) {
                        selectedTemplate = null
                }
        }

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = { Text("Select Product") },
                                navigationIcon = {
                                        IconButton(onClick = onBack) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                },
                                actions = {
                                        IconButton(onClick = { showAddDialog = true }) {
                                                Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "Add New Template"
                                                )
                                        }
                                }
                        )
                },
                bottomBar = {}, // Bara de jos goală (cantitatea e acum în dialog/template)
                floatingActionButton = {
                        FloatingActionButton(
                                onClick = {
                                        selectedTemplate?.let { template ->
                                                onNext(
                                                        template.name,
                                                        template.unit,
                                                        template.quantity
                                                )
                                        }
                                },
                                containerColor =
                                        if (selectedTemplate != null)
                                                MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor =
                                        if (selectedTemplate != null)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        ) { Icon(Icons.Default.ArrowRight, contentDescription = "Next") }
                }
        ) { padding ->

                // Dialog pentru adăugarea unui nou template
                if (showAddDialog) {
                        AddProductTemplateDialog(
                                onConfirm = { newName, newUnit, newQuantity ->
                                        viewModel.addTemplate(
                                                newName,
                                                newUnit,
                                                "", // location handled at instance level
                                                newQuantity
                                        )
                                        Toast.makeText(
                                                        context,
                                                        "Added: $newName",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                },
                                onDismiss = { showAddDialog = false }
                        )
                }

                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        // Lista template-urilor existente
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                items(
                                        templates,
                                        key = {
                                                "${it.name}|${it.unit}|${it.location}|${it.quantity}"
                                        }
                                ) { template ->
                                        var visible by remember { mutableStateOf(true) }

                                        AnimatedVisibility(
                                                visible = visible,
                                                exit = fadeOut() + shrinkVertically()
                                        ) {
                                                Button(
                                                        onClick = { selectedTemplate = template },
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(horizontal = 16.dp)
                                                                        .height(60.dp),
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                if (selectedTemplate ==
                                                                                                template
                                                                                )
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surface
                                                                ),
                                                        shape = RoundedCornerShape(12.dp)
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween,
                                                                modifier = Modifier.fillMaxSize()
                                                        ) {
                                                                // Afișează Nume (Cantitate Unit) -
                                                                // Locație
                                                                Text(
                                                                        text =
                                                                                "${template.name} (${template.quantity} ${template.unit})",
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        start =
                                                                                                16.dp
                                                                                ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleMedium,
                                                                        color =
                                                                                if (selectedTemplate ==
                                                                                                template
                                                                                )
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onPrimary
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurface
                                                                )

                                                                IconButton(
                                                                        onClick = {
                                                                                visible = false
                                                                                scope.launch {
                                                                                        delay(250)
                                                                                        viewModel
                                                                                                .removeTemplate(
                                                                                                        template
                                                                                                )
                                                                                }
                                                                        },
                                                                        modifier =
                                                                                Modifier.size(40.dp)
                                                                ) {
                                                                        Icon(
                                                                                Icons.Default
                                                                                        .Delete,
                                                                                contentDescription =
                                                                                        "Delete",
                                                                                tint =
                                                                                        if (selectedTemplate ==
                                                                                                        template
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onPrimary
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.7f
                                                                                                        )
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.7f
                                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }

                                if (templates.isEmpty()) {
                                        item {
                                                Text(
                                                        "No product templates saved. Add one using the '+' button.",
                                                        modifier = Modifier.padding(16.dp),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        }
                }
        }
}

// Dialog pentru adăugarea unui nou template
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddProductTemplateDialog(
        onDismiss: () -> Unit,
        onConfirm: (name: String, unit: String, quantity: Double) -> Unit
) {
        var text by remember { mutableStateOf("") }
        var selectedUnit by remember { mutableStateOf("Unit") }
        var quantityText by remember { mutableStateOf("1.0") }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Add Product Template") },
                text = {
                        Column {
                                Text("Enter product name:")
                                Spacer(Modifier.height(8.dp))
                                TextField(
                                        value = text,
                                        onValueChange = { text = it },
                                        singleLine = true,
                                        placeholder = { Text("Ex: Milk") }
                                )

                                Spacer(Modifier.height(16.dp))
                                Text("Enter quantity per item:")
                                Spacer(Modifier.height(8.dp))
                                TextField(
                                        value = quantityText,
                                        onValueChange = {
                                                if (it.isEmpty() ||
                                                                it.count { c -> c == '.' } <= 1 &&
                                                                        it.all { c ->
                                                                                c.isDigit() ||
                                                                                        c == '.'
                                                                        }
                                                ) {
                                                        quantityText = it
                                                }
                                        },
                                        singleLine = true,
                                        placeholder = { Text("1.0") },
                                        keyboardOptions =
                                                KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                Spacer(Modifier.height(16.dp))
                                Text("Select unit:")
                                Spacer(Modifier.height(8.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        listOf("Unit", "L", "kg").forEach { unit ->
                                                Button(
                                                        onClick = { selectedUnit = unit },
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                if (selectedUnit ==
                                                                                                unit
                                                                                )
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surfaceVariant
                                                                ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(48.dp)
                                                ) {
                                                        Text(
                                                                text = unit,
                                                                color =
                                                                        if (selectedUnit == unit)
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimary
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                        )
                                                }
                                        }
                                }
                        }
                },
                confirmButton = {
                        TextButton(
                                onClick = {
                                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                                        onConfirm(text.trim(), selectedUnit, qty)
                                        onDismiss()
                                },
                                enabled =
                                        text.isNotBlank() &&
                                                !text.contains(NAME_UNIT_SEPARATOR) &&
                                                !text.contains('/')
                        ) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
}
