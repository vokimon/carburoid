package net.canvoki.carburoid.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorizedProductSelector() {
    var expanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf("Select Product") }
    var recentSelections by remember { mutableStateOf(emptyList<String>()) }

    val productCategories = listOf(
        ProductCategory("Gasoils", listOf(
            "Gasoleo A", "Gasoleo Premium", "Gasoleo B", "Diésel Renovable"
        )),
        ProductCategory("Gasolinas", listOf(
            "Gasolina 95 E10", "Gasolina 95 E25", "Gasolina 95 E5", "Gasolina 95 E5 Premium",
            "Gasolina 95 E85", "Gasolina 98 E10", "Gasolina 98 E5", "Gasolina Renovable"
        )),
        ProductCategory("Gas Naturales & Biogases", listOf(
            "Gas Natural Comprimido", "Gas Natural Licuado", "Biogas Natural Comprimido",
            "Biogas Natural Licuado"
        )),
        ProductCategory("Biocombustibles", listOf(
            "Biodiesel", "Bioetanol"
        )),
        ProductCategory("Otros Gases", listOf(
            "Gases licuados del petróleo", "Hidrogeno", "Amoniaco", "Metanol"
        )),
        ProductCategory("Aditivos", listOf(
            "Adblue"
        ))
    )

    Column(modifier = Modifier.padding(16.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = selectedProduct,
                onValueChange = { },
                readOnly = true,
                label = { Text("Select Product") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.LocalGasStation,
                        contentDescription = "Gas Station Icon"
                    )
                },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (recentSelections.isNotEmpty()) {
                    Text(
                        "Recent Selections",
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    recentSelections.forEach { product ->
                        DropdownMenuItem(onClick = {
                            selectedProduct = product
                            expanded = false
                            if (!recentSelections.contains(product)) {
                                recentSelections = listOf(product) + recentSelections.take(4)
                            }
                        }, text = { Text(product) })
                    }
                    HorizontalDivider()
                }

                productCategories.forEach { category ->
                    Text(
                        category.name,
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    category.products.forEach { product ->
                        DropdownMenuItem(onClick = {
                            selectedProduct = product
                            expanded = false
                            if (!recentSelections.contains(product)) {
                                recentSelections = listOf(product) + recentSelections.take(4)
                            }
                        }, text = { Text(product) })
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

data class ProductCategory(val name: String, val products: List<String>)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CategorizedProductSelector()
}
