package app.plein.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/** Кривые M3 emphasized из ДНК §4. easeInOut для фирменных появлений не берём. */
val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

fun <T> emphasized(durationMillis: Int = 500) = tween<T>(durationMillis, easing = Emphasized)
fun <T> entering(durationMillis: Int = 400) = tween<T>(durationMillis, easing = EmphasizedDecelerate)
fun <T> exiting(durationMillis: Int = 200) = tween<T>(durationMillis, easing = EmphasizedAccelerate)
