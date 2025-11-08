package com.ahmedgamal.aquamemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandlePricesScreen(
    onBackClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val candlePrices by viewModel.candlePrices.collectAsState(emptyList())
    val selectedCurrency by viewModel.selectedCurrency.collectAsState() // 🔽 استخدم من ViewModel

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.candle_prices_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // اختيار العملة
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.currency),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        listOf("USD", "EUR", "EGP", "SAR").forEach { currency ->
                            FilterChip(
                                selected = selectedCurrency == currency,
                                onClick = {
                                    // 🔽 التصحيح: استخدم الدالة الجديدة
                                    viewModel.updateSelectedCurrency(currency)
                                },
                                label = { Text(currency) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // قائمة الأسعار
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 🔽 **التصحيح: استخدام items بشكل صحيح**
                items(candlePrices, key = { it.candleNumber }) { price ->
                    CandlePriceItem(
                        candlePrice = price,
                        currency = selectedCurrency,
                        onPriceChange = { newPrice ->
                            viewModel.updateCandlePrice(price.candleNumber, newPrice)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CandlePriceItem(
    candlePrice: CandlePrice,
    currency: String,
    onPriceChange: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(candlePrice.price.toString()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getCandleName(candlePrice.candleNumber), // 🔽 **التصحيح: استخدام candleNumber**
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = {
                    priceText = it
                    it.toDoubleOrNull()?.let { price ->
                        onPriceChange(price)
                    }
                },
                label = { Text(currency) },
                modifier = Modifier.width(120.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}