package com.zereimaisum.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Mapeia as chaves de ícone salvas no banco para ícones do Material. */
object ZmIcons {
    val boxChoices = listOf("shield", "flight", "home", "celebration", "star", "savings", "school", "car", "health")

    fun of(key: String): ImageVector = when (key) {
        "work" -> Icons.Filled.Work
        "card" -> Icons.Filled.CreditCard
        "percent" -> Icons.Filled.Percent
        "build" -> Icons.Filled.Build
        "add" -> Icons.Filled.Add
        "home" -> Icons.Filled.Home
        "water" -> Icons.Filled.WaterDrop
        "bolt" -> Icons.Filled.Bolt
        "wifi" -> Icons.Filled.Wifi
        "phone" -> Icons.Filled.Phone
        "cart" -> Icons.Filled.ShoppingCart
        "car" -> Icons.Filled.DirectionsCar
        "school" -> Icons.Filled.School
        "health" -> Icons.Filled.LocalHospital
        "celebration" -> Icons.Filled.Celebration
        "subscriptions" -> Icons.Filled.Subscriptions
        "delivery" -> Icons.Filled.DeliveryDining
        "debt" -> Icons.Filled.MoneyOff
        "shield" -> Icons.Filled.Shield
        "flight" -> Icons.Filled.Flight
        "star" -> Icons.Filled.Star
        "savings" -> Icons.Filled.Savings
        else -> Icons.Filled.MoreHoriz
    }
}
