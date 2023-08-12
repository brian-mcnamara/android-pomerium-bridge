package dev.bmac.pomeriumtunneler.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.bmac.pomeriumtunneler.storage.RouteItem
import dev.bmac.pomeriumtunneler.storage.RouteItemModel
import dev.bmac.pomeriumtunneler.storage.RouteRepository
import dev.bmac.pomeriumtunneler.storage.RoutesDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Composable
fun RouteListScreen(routeModel: RouteItemModel, modifier: Modifier, navHostController: NavHostController) {
    val routes by routeModel.routes.observeAsState(initial = emptyList())
    Column(modifier) {
        for ( routeItem in routes) {
            RouteCard(routeItem = routeItem, navHostController, routeModel)
        }
    }

}

//@Composable
//@Preview
//private fun testListScreen() {
//    RouteListScreen(routeModel = RouteItemModel(RouteRepository(object : RoutesDao {
//        override fun getAll(): Flow<List<RouteItem>> {
//            return listOf(listOf(RouteItem("test", 8080, 1))).asFlow()
//        }
//
//        override fun insert(routeItem: RouteItem) {
//            TODO("Not yet implemented")
//        }
//
//        override fun delete(routeItem: RouteItem) {
//            TODO("Not yet implemented")
//        }
//
//        override fun update(routeItem: RouteItem) {
//            TODO("Not yet implemented")
//        }
//    })), Modifier.padding(1.dp), onClick = {})
//}