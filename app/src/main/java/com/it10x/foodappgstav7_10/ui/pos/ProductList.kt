package com.it10x.foodappgstav7_10.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.it10x.foodappgstav7_10.data.pos.entities.PosCartEntity
import com.it10x.foodappgstav7_10.data.pos.entities.ProductEntity
import com.it10x.foodappgstav7_10.ui.cart.CartViewModel
import com.it10x.foodappgstav7_10.ui.theme.*
import com.it10x.foodappgstav7_10.viewmodel.PosTableViewModel

@Composable
fun ProductList(
    filteredProducts: List<ProductEntity>,
   // variants: List<ProductEntity>,
    cartViewModel: CartViewModel,
    tableViewModel: PosTableViewModel,
    tableNo: String,
    posSessionViewModel: PosSessionViewModel,
    onProductAdded: () -> Unit
) {


    val sessionId by posSessionViewModel.sessionId.collectAsState()
    val sortedProducts = remember(filteredProducts) {
        filteredProducts
            .filter { it.type == "parent" } // ✅ only parent products
            .sortedBy { it.sortOrder }
    }
//    val sortedProducts = remember(filteredProducts) {
//        filteredProducts.sortedBy { it.sortOrder }
//    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(
            count = sortedProducts.size,
            key = { index -> sortedProducts[index].id }
        ) { index ->

            val product = sortedProducts[index]

            ParentProductCard(
                product = product,
                filteredProducts = filteredProducts,
                cartViewModel = cartViewModel,
                tableViewModel = tableViewModel,
                tableNo = tableNo,
                sessionId = sessionId,
                onProductAdded = onProductAdded
            )
        }

    }
}

@Composable
private fun ParentProductCard(
    product: ProductEntity,
    filteredProducts:List<ProductEntity>,
    cartViewModel: CartViewModel,
    tableViewModel: PosTableViewModel,
    tableNo: String,
    sessionId: String,
    onProductAdded: () -> Unit
) {

    var showVariantDialog by remember { mutableStateOf(false) }
    val cartItems by cartViewModel.cart.collectAsState()
    val currentQty = cartItems
        .filter { it.tableId == tableNo && it.productId == product.id }
        .sumOf { it.quantity }

    val variants = remember(filteredProducts, product.id) {
        filteredProducts.filter { it.parentId == product.id }
    }

    val productBg = MaterialTheme.colorScheme.background//MaterialTheme.colorScheme.surface
    val productText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)

    val addBg = PosTheme.accent.cartAddBg
    val addText = PosTheme.accent.cartAddText

    val removeBorder = PosTheme.accent.cartRemoveBorder
    val removeText = PosTheme.accent.cartRemoveText


    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.10f) // light, subtle border
                )
            ),
        color = productBg,
        shape = RectangleShape
    ) {

        Column(
            modifier = Modifier
                .padding(11.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            val price = when {
                product.discountPrice == null || product.discountPrice == 0.0 -> product.price
                else -> product.discountPrice
            }

            val code = product.searchCode?.trim()
            val numericCode = code?.takeIf { it.all { ch -> ch.isDigit() } }

            val displayName = numericCode?.let {
                "${product.name} $it"
            } ?: product.name

            Text(
                text = toTitleCase(displayName),
                minLines = 2,
                maxLines = 2,
                lineHeight = 18.sp,
                color = productText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    "₹$price",
                    color = productText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 🔒 LEFT SIDE: fixed container so layout never shifts
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ➖ Remove slot (always reserves space)
                    Box(
                        modifier = Modifier.size(width = 48.dp, height = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentQty > 0) {
                            IconButton(
                                onClick = { cartViewModel.decrease(product.id, tableNo) },
                                modifier = Modifier
                                    .size(width = 38.dp, height = 30.dp)
                                    .background(
                                        color = Color(0xFF64748B), // darker slate (not white)
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(
                                    "−",
                                    color = Color(0xFF1E293B), // dark slate text
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }


                        }
                    }

                    // 🔢 Qty slot (fixed width)
                    Box(
                        modifier = Modifier.size(width = 32.dp, height = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentQty > 0) {
                            Text(
                                currentQty.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = productText
                            )
                        }
                    }
                }

                // ➕ Add (unchanged behavior)

                IconButton(
                    onClick = {

                        if (product.hasVariants == true) {
                            // 👉 STEP 1: just trigger placeholder action
                            showVariantDialog = true   // you can use this to open box later
                        } else {
                            // 👉 Normal product
                            cartViewModel.addProductToCart(
                                product = product,
                                price = price
                            )
                            onProductAdded()
                        }
                    },
                    modifier = Modifier
                        .size(width = 38.dp, height = 30.dp)
                        .background(
                            color = addBg.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp) // 👈 perfect POS feel
                        )
                ) {
                    Text(
                        "+",
                        color = addText,
                        fontSize = 18.sp
                    )
                }

            }

        }
    }

    if (showVariantDialog) {

        var search by remember { mutableStateOf("") }

        val filteredVariants = variants.filter {
            it.name.contains(search, ignoreCase = true) ||
                    (it.searchCode?.contains(search, ignoreCase = true) == true)
        }
        val textPrimary = MaterialTheme.colorScheme.onSurface
        val textSecondary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        AlertDialog(
            onDismissRequest = { showVariantDialog = false },

            // ✅ ADD THIS BUTTON
            confirmButton = {
                Button(
                    onClick = {
                        showVariantDialog = false
                        onProductAdded()
                    }
                ) {
                    Text("OK")
                }
            },

            title = {
                Text("Select")
            },

            text = {
                Column {

                    // 🔍 SEARCH BOX
//                    OutlinedTextField(
//                        value = search,
//                        onValueChange = { search = it },
//                        placeholder = { Text("Search variant...") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(10.dp))

                    // 📦 VARIANT LIST
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {

                        filteredVariants.forEach { variant ->

                            val variantPrice =
                                if (variant.discountPrice == null || variant.discountPrice == 0.0)
                                    variant.price
                                else variant.discountPrice

                            val cartItems by cartViewModel.cart.collectAsState()

                            val qty = cartItems
                                .filter { it.productId == variant.id && it.tableId == tableNo }
                                .sumOf { it.quantity }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = variant.name,
                                        color = Color.White, // ✅ WHITE TEXT
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "₹$variantPrice",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    // ➖ MINUS
                                    IconButton(
                                        onClick = {
                                            if (qty > 0) {
                                                cartViewModel.decrease(variant.id, tableNo)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                PosTheme.accent.cartRemoveBorder, // ✅ FROM THEME
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Text(
                                            "−", // ✅ real minus symbol (NOT underscore)
                                            color = PosTheme.accent.cartRemoveText,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = qty.toString(),
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        color = Color.White, // ✅ WHITE
                                        fontWeight = FontWeight.Bold
                                    )

                                    // ➕ PLUS
                                    IconButton(
                                        onClick = {
                                            cartViewModel.addProductToCart(
                                                product = variant,
                                                price = variantPrice
                                            )
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                PosTheme.accent.cartAddBg, // ✅ FROM THEME
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Text(
                                            "+",
                                            color = PosTheme.accent.cartAddText, // ✅ FROM THEME
                                            fontSize = 18.sp
                                        )
                                    }
                                }

                            }
                            Divider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        )
    }
}
