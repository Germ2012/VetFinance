package ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import kotlin.math.max

class NumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text.replace(".", "").replace(",", "")
        val formattedText = if (originalText.isNotEmpty()) {
            val number = originalText.toLongOrNull()
            if (number != null) {
                val formatter = DecimalFormat("#,###")
                formatter.format(number)
            } else {
                originalText
            }
        } else {
            ""
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val transformedOffset = offset + formattedText.count { it == '.' || it == ',' }
                return transformedOffset.coerceIn(0, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val originalOffset = offset - formattedText.substring(0, offset).count { it == '.' || it == ',' }
                return originalOffset.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(
            AnnotatedString(formattedText),
            offsetMapping
        )
    }
}

fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,###.##")
    return formatter.format(value)
}

val ThousandsSeparatorTransformation = NumberTransformation()