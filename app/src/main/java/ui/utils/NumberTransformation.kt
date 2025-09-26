package ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import kotlin.math.max

class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val longValue = originalText.toLongOrNull()
        if (longValue == null) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = DecimalFormat("#,###").format(longValue).replace(",", ".")

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val digitsToRight = originalText.length - offset
                val separatorsToRight = max(0, (digitsToRight - 1) / 3)
                val totalSeparators = max(0, (originalText.length - 1) / 3)
                val separatorsToLeft = totalSeparators - separatorsToRight
                return offset + separatorsToLeft
            }

            override fun transformedToOriginal(offset: Int): Int {
                val separatorsBefore = formattedText.take(offset).count { it == '.' }
                return offset - separatorsBefore
            }
        }

        return TransformedText(
            AnnotatedString(formattedText),
            offsetMapping
        )
    }
}

/**
 * Función de utilidad para formatear un número Double a una cadena de moneda en guaraníes.
 * ¡CORREGIDO! Ahora está fuera de la clase.
 */
fun formatCurrency(value: Double): String {
    return try {
        String.format("%,.0f", value).replace(",", ".")
    } catch (e: Exception) {
        "0"
    }
}