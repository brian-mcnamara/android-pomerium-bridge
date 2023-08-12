package dev.bmac.pomeriumtunneler.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["localPort"], unique = true)])
data class RouteItem(
    val route: String,
    val localPort: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)
