package dev.bmac.pomeriumtunneler.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import dev.bmac.pomeriumtunneler.storage.RouteItem
import java.net.ServerSocket

@Composable
fun EditRouteView(routeItem: RouteItem?, onSave: (route: RouteItem) -> Unit) {
    val mContext = LocalContext.current
    val route = remember {
        mutableStateOf(
            TextFieldValue(
                routeItem?.route?.substringAfter("https://") ?: ""
            )
        )
    }
    val port = remember {
        mutableStateOf(
            TextFieldValue(
                routeItem?.localPort?.toString() ?: ServerSocket(0).use { it.localPort }.toString()
            )
        )
    }
    Column() {
        Row(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            Column() {
                Row() {
                    TextField(
                        value = route.value,
                        onValueChange = { route.value = it },
                        prefix = { Text("https://") },
                        label = { Text("Route:") },
                        placeholder = { Text("route.example.com") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row() {
                    TextField(
                        value = port.value,
                        onValueChange = { port.value = it },
                        label = { Text("Port:") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

            }
        }

        Row() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row() {
                    Button(
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (route.value.text.isBlank()) {
                                Toast.makeText(
                                    mContext,
                                    "Route can not be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            onSave(
                                routeItem?.copy(
                                    route = "https://${route.value.text}",
                                    localPort = port.value.text.toInt()
                                ) ?: RouteItem(
                                    "https://${route.value.text}",
                                    port.value.text.toInt()
                                )
                            )
                        }) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }

}

@Composable
@Preview
private fun TestCreateRoute() {
    EditRouteView(routeItem = null, onSave = {})
}