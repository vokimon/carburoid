package net.canvoki.carburoid.product

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.R
import net.canvoki.carburoid.ui.settings.ThemeSettings

@Composable
fun CategorizedProductSelector() {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var productSelection = remember { ProductSelection(context = context) }
    var selectedProduct by productSelection.asState()
    var recentSelections = productSelection.recent()

    val productCategories =
        listOf(
            ProductCategory(
                stringResource(R.string.product_category_diesel),
                listOf(
                    "Gasoleo A",
                    "Gasoleo Premium",
                    "Gasoleo B",
                    "Diésel Renovable",
                ),
            ),
            ProductCategory(
                stringResource(R.string.product_category_gasoline),
                listOf(
                    "Gasolina 95 E10",
                    "Gasolina 95 E25",
                    "Gasolina 95 E5",
                    "Gasolina 95 E5 Premium",
                    "Gasolina 95 E85",
                    "Gasolina 98 E10",
                    "Gasolina 98 E5",
                    "Gasolina Renovable",
                ),
            ),
            ProductCategory(
                stringResource(R.string.product_category_natural_gas),
                listOf(
                    "Gas Natural Comprimido",
                    "Gas Natural Licuado",
                    "Biogas Natural Comprimido",
                    "Biogas Natural Licuado",
                ),
            ),
            ProductCategory(
                stringResource(R.string.product_category_biofuels),
                listOf(
                    "Biodiesel",
                    "Bioetanol",
                ),
            ),
            ProductCategory(
                stringResource(R.string.product_category_other_gases),
                listOf(
                    "Gases licuados del petróleo",
                    "Hidrogeno",
                    "Amoniaco",
                    "Metanol",
                ),
            ),
            ProductCategory(
                stringResource(R.string.product_category_additives),
                listOf(
                    "Adblue",
                ),
            ),
        )

    @Composable
    fun CategoryHeader(categoryName: String) {
        Text(
            categoryName,
            modifier = Modifier.padding(8.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        )
    }

    @Composable
    fun ProductItem(product: String) {
        DropdownMenuItem(
            text = { Text(product) },
            onClick = {
                selectedProduct = product
                expanded = false
            },
        )
    }

    @Composable
    fun CategoryGroup(
        categoryName: String,
        products: List<String>,
    ) {
        CategoryHeader(categoryName)
        products.forEach { product ->
            ProductItem(product)
        }
        HorizontalDivider()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = selectedProduct,
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(R.string.product_selector_hint)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.LocalGasStation,
                    contentDescription = stringResource(R.string.product_selector_icon_description),
                )
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (recentSelections.isNotEmpty()) {
                CategoryGroup(
                    categoryName = stringResource(R.string.product_category_recent),
                    products = recentSelections,
                )
            }

            productCategories.forEach { category ->
                CategoryGroup(
                    categoryName = category.name,
                    products = category.products,
                )
            }
        }
    }
}

data class ProductCategory(
    val name: String,
    val products: List<String>,
)

class CategorizedProductSelectorView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : FrameLayout(context, attrs, defStyle) {
        private val composeView = ComposeView(context)

        init {
            addView(composeView)

            composeView.setContent {
                CategorizedProductSelectorWrapper()
            }
        }
    }

@Composable
private fun CategorizedProductSelectorWrapper() {
    MaterialTheme(
        colorScheme = ThemeSettings.effectiveColorScheme(),
    ) {
        CategorizedProductSelector()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CategorizedProductSelector()
}
