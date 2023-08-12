package dev.bmac.pomeriumtunneler

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.bmac.pomeriumtunneler.service.TunnelService
import dev.bmac.pomeriumtunneler.storage.AppDatabase
import dev.bmac.pomeriumtunneler.storage.RouteItemModel
import dev.bmac.pomeriumtunneler.storage.RouteRepository
import dev.bmac.pomeriumtunneler.ui.EditRouteView
import dev.bmac.pomeriumtunneler.ui.RouteListScreen
import dev.bmac.pomeriumtunneler.ui.theme.PomeriumTunnelerTheme

class MainActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val repository by lazy { RouteRepository(db.routesDao()) }
    private val routeModel: RouteItemModel by viewModels {
        RouteItemModel.RouteItemModelFactory(repository)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForegroundService(Intent(applicationContext, TunnelService::class.java))
        setContent {
            val navController = rememberNavController()
            PomeriumTunnelerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(startDestination = "/", navController = navController) {
                        composable("/") {
//                            Surface(modifier = Modifier.fillMaxSize()) {
//
//                            }
                            Scaffold(floatingActionButtonPosition = FabPosition.End,
                                floatingActionButton = {
                                    FloatingActionButton(onClick = {
                                        navController.navigate("/newRoute")
                                    }) {
                                        Icon(Icons.Filled.Add, "")
                                    }
                                }, content = { contentPadding ->
                                    RouteListScreen(routeModel = routeModel, Modifier.padding(contentPadding), navController)
                                })

                        }
                        composable("/newRoute") {
                            EditRouteView(routeItem = null, onSave = {
                                navController.popBackStack()
                                repository.insert(it)
                            })
                        }
                        composable("/edit/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })) {

                            val liveData = repository.get(it.arguments?.getInt("id")!!)
                            val routeItem = liveData.observeAsState().value
                            if (routeItem != null) {
                                EditRouteView(routeItem = routeItem, onSave = {
                                    navController.popBackStack()
                                    repository.update(it)
                                } )
                            }
                        }
                    }
                }
            }
        }
    }
}