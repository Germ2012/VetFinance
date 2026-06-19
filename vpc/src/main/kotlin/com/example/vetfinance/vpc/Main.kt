package com.example.vetfinance.vpc

import com.example.vetfinance.vpc.data.VpcDatabase
import com.example.vetfinance.vpc.ui.VetFinanceDesktopApp
import com.formdev.flatlaf.FlatLightLaf
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    FlatLightLaf.setup()
    UIManager.put("Button.arc", 8)
    UIManager.put("Component.arc", 8)
    UIManager.put("TextComponent.arc", 8)

    SwingUtilities.invokeLater {
        val database = VpcDatabase.default()
        VetFinanceDesktopApp(database).isVisible = true
    }
}
