package com.ares.ewe_shop.presentation.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ares.ewe_shop.core.DevServerStore

@Stable
class DevServerDialogState {
    var show by mutableStateOf(false)
    var draft by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var showRestartHint by mutableStateOf(false)

    fun open() {
        draft = DevServerStore.savedRaw().ifBlank { DevServerStore.displayHost() }
        error = null
        show = true
    }
}

@Composable
fun rememberDevServerDialogState(): DevServerDialogState = remember { DevServerDialogState() }

@Composable
fun DevServerConnectionDialogs(state: DevServerDialogState) {
    if (state.show) {
        AlertDialog(
            onDismissRequest = { state.show = false },
            title = { Text("Server connection") },
            text = {
                Column {
                    Text(
                        text = "Escribe la IP de tu Mac. Cierra y vuelve a abrir la app para conectarte.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = {
                            state.draft = it
                            state.error = null
                        },
                        singleLine = true,
                        placeholder = { Text("192.168.1.10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        isError = state.error != null,
                        supportingText = state.error?.let { message -> { Text(message) } },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (DevServerStore.save(state.draft)) {
                            state.show = false
                            state.showRestartHint = true
                        } else {
                            state.error = "Usa una IP como 192.168.1.10, o la URL completa con puerto."
                        }
                    },
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Column {
                    if (DevServerStore.savedRaw().isNotEmpty()) {
                        TextButton(
                            onClick = {
                                DevServerStore.clear()
                                state.show = false
                                state.showRestartHint = true
                            },
                        ) {
                            Text("Usar la del build")
                        }
                    }
                    TextButton(onClick = { state.show = false }) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }

    if (state.showRestartHint) {
        AlertDialog(
            onDismissRequest = { state.showRestartHint = false },
            title = { Text("Reinicia la app") },
            text = { Text("Cierra DobbiShop y ábrela de nuevo. Ahí se conecta a la IP que guardaste.") },
            confirmButton = {
                TextButton(onClick = { state.showRestartHint = false }) {
                    Text("Entendido")
                }
            },
        )
    }
}
