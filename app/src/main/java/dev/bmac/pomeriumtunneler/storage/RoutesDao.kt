package dev.bmac.pomeriumtunneler.storage

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutesDao {

    @Query("SELECT * from routeitem")
    fun getAll(): Flow<List<RouteItem>>

    @Query("SELECT * from routeitem WHERE id=:id")
    fun get(id: Int): LiveData<RouteItem?>

    @Insert
    fun insert(routeItem: RouteItem)

    @Delete
    fun delete(routeItem: RouteItem)

    @Update
    fun update(routeItem: RouteItem)
}