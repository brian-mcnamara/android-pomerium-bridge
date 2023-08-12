package dev.bmac.pomeriumtunneler.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import dev.bmac.pomeriumtunneler.storage.RouteItem
import dev.bmac.pomeriumtunneler.storage.RouteItemModel


@Composable
fun RouteCard(routeItem: RouteItem, navHostController: NavHostController, routeItemModel: RouteItemModel) {
    val itemHeight = remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current
    Card(modifier = Modifier.padding(5.dp).onSizeChanged {
        itemHeight.value = with(localDensity) {it.height.toDp() }
    }) {
        val isMenuVisible = remember { mutableStateOf(false) }
        val pressOffset = remember { mutableStateOf(DpOffset.Zero) }

        Box(modifier = Modifier.fillMaxWidth().pointerInput(true) {
            detectTapGestures(onTap = { navHostController.navigate("/edit/${routeItem.id}") },
                onLongPress = {
                    isMenuVisible.value = true
                    pressOffset.value = DpOffset(it.x.toDp(), it.y.toDp())
                })
        }) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row() {
                    Text(text = "Route:", fontWeight = FontWeight.Bold)
                    Text(text = routeItem.route, modifier = Modifier.padding(start = 5.dp))
                }
                Row() {
                    Text(text = "Local port:", fontWeight = FontWeight.Bold)
                    Text(text = routeItem.localPort.toString(), modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
        DropdownMenu(expanded = isMenuVisible.value,
            onDismissRequest = { isMenuVisible.value = false },
            offset = pressOffset.value.copy(
                y = pressOffset.value.y - itemHeight.value
            )) {
            DropdownMenuItem(text = { Text("Delete") }, onClick = {
                routeItemModel.deleteRoute(routeItem)
            })
        }
    }
}