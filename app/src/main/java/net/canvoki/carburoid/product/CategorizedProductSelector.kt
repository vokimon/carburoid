package net.canvoki.carburoid.product

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
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

    val productCategories = ProductCatalog.categories

    @Composable
    fun CategoryHeader(
        @StringRes name: Int,
    ) {
        Text(
            stringResource(name),
            modifier = Modifier.padding(8.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        )
    }

    @Composable
    fun ProductItem(apiName: String) {
        DropdownMenuItem(
            text = { Text(translateProductName(apiName)) },
            onClick = {
                selectedProduct = apiName
                expanded = false
            },
            modifier = Modifier.padding(start = 16.dp),
        )
    }

    @Composable
    fun CategoryGroup(category: ProductCatalog.ProductCategory) {
        CategoryHeader(category.name)
        category.products.forEach { product ->
            ProductItem(product.apiName)
        }
        HorizontalDivider()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = translateProductName(selectedProduct),
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(R.string.product_selector_hint)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_local_gas_station),
                    contentDescription = stringResource(R.string.product_selector_icon_description),
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (recentSelections.isNotEmpty()) {
                CategoryHeader(R.string.product_category_recent)
                recentSelections.forEach { apiName ->
                    ProductItem(apiName)
                }
                HorizontalDivider()
            }

            productCategories.forEach { category ->
                CategoryGroup(category)
            }
        }
    }
}

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
