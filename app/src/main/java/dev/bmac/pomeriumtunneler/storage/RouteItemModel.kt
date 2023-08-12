package dev.bmac.pomeriumtunneler.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData

class RouteItemModel(
    private val routesRepository: RouteRepository
): ViewModel() {

    val routes = routesRepository.routes.asLiveData()

    fun deleteRoute(routeItem: RouteItem) = routesRepository.delete(routeItem)

    class RouteItemModelFactory(private val routesRepository: RouteRepository): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RouteItemModel::class.java)) {
                return RouteItemModel(routesRepository) as T
            }
            throw IllegalArgumentException("")
        }
    }
}