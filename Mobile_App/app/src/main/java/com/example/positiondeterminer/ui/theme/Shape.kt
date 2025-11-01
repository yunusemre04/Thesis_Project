package com.example.positiondeterminer.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Sleeker shape scale for a modern, soft UI
val Shapes = Shapes(
    // Extra Small - Chips, small buttons
    extraSmall = RoundedCornerShape(10.dp),

    // Small - Text fields, buttons
    small = RoundedCornerShape(14.dp),

    // Medium - Cards, dialogs
    medium = RoundedCornerShape(18.dp),

    // Large - Navigation drawers, modals
    large = RoundedCornerShape(24.dp),

    // Extra Large - Large surfaces and hero cards
    extraLarge = RoundedCornerShape(32.dp)
)

// Helper for elevated surfaces if needed elsewhere
fun elevatedShape(level: Int = 1): CornerBasedShape = when (level) {
    0 -> RoundedCornerShape(8.dp)
    1 -> RoundedCornerShape(16.dp)
    2 -> RoundedCornerShape(24.dp)
    else -> RoundedCornerShape(32.dp)
}
