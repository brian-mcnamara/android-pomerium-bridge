package dev.bmac.pomeriumtunneler.storage

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class RouteRepository(private val routesDao: RoutesDao) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    val routes = routesDao.getAll()

    fun get(id: Int): LiveData<RouteItem?> {
        return routesDao.get(id)
    }


    fun insert(routeItem: RouteItem) {
        coroutineScope.launch(Dispatchers.IO) {
            routesDao.insert(routeItem)
        }
    }

    fun update(routeItem: RouteItem) {
        coroutineScope.launch(Dispatchers.IO) {
            routesDao.update(routeItem)
        }
    }

    fun delete(routeItem: RouteItem) {
        coroutineScope.launch(Dispatchers.IO) {
            routesDao.delete(routeItem)
        }
    }
}