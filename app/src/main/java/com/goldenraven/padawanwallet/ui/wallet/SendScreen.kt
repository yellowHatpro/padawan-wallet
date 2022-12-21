/*
 * Copyright 2020-2022 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package com.goldenraven.padawanwallet.ui.wallet

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.goldenraven.padawanwallet.R
import com.goldenraven.padawanwallet.data.Wallet
import com.goldenraven.padawanwallet.theme.*
import com.goldenraven.padawanwallet.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bitcoindevkit.PartiallySignedTransaction
import org.bitcoindevkit.TxBuilderResult
import androidx.compose.material.SnackbarHostState as SnackbarHostStateM2
import androidx.compose.material.SnackbarDuration as SnackbarDurationM2

private const val TAG = "SendScreen"

// BottomSheetScaffold is not available in Material 3, so this screen is all Material 2

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun SendScreen(navController: NavHostController, walletViewModel: WalletViewModel) {
    val (showDialog, setShowDialog) = rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val balance by walletViewModel.balance.observeAsState()
    val recipientAddress: MutableState<String> = rememberSaveable { mutableStateOf("") }
    val amount: MutableState<String> = rememberSaveable { mutableStateOf("") }
    val feeRate: MutableState<String> = rememberSaveable { mutableStateOf("") }
    val txBuilderResult: MutableState<TxBuilderResult?> = rememberSaveable { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val showMenu: MutableState<Boolean> = remember { mutableStateOf(false) }
    var dropDownMenuExpanded by remember { mutableStateOf(false) }
    val currencyList = listOf(CurrencyType.SATS, CurrencyType.BTC)
    var selectedCurrency by remember { mutableStateOf(0) }

    val qrCodeScanner =
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("BTC_Address")
            ?.observeAsState()
    qrCodeScanner?.value.let {
        if (it != null)
            recipientAddress.value = it

        navController.currentBackStackEntry?.savedStateHandle?.remove<String>("BTC_Address")
    }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
    )

    BottomSheetScaffold(
        sheetContent = { TransactionConfirmation(txBuilderResult, recipientAddress, feeRate, bottomSheetScaffoldState, scope, navController) },
        scaffoldState = bottomSheetScaffoldState,
        sheetBackgroundColor = Color.White,
        sheetElevation = 12.dp,
        sheetPeekHeight = 0.dp,
        backgroundColor = padawan_theme_background
    ) {
        val focusManager = LocalFocusManager.current
        PadawanAppBar(navController = navController, title = "Send bitcoin")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .standardBackground()
        ) {
            Row(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                Text(
                    text = "Amount",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .weight(weight = 0.5f)
                        .align(Alignment.Bottom)
                )
                Text(
                    text = "Balance: ${balance.toString()} sats",
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(weight = 0.5f)
                        .align(Alignment.Bottom)
                )
            }
            TextField(
                modifier = Modifier
                    .wideTextField()
                    .height(IntrinsicSize.Min),
                shape = RoundedCornerShape(20.dp),
                value = amount.value,
                onValueChange = { value -> amount.value = value.filter { it.isDigit() } },
                singleLine = true,
                placeholder = { Text("Enter amount (sats)") },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = padawan_theme_background_secondary,
                    cursorColor = padawan_theme_onPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                enabled = (true),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
            )

            Text(
                text = "Address",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
            TextField(
                modifier = Modifier
                    .wideTextField()
                    .height(IntrinsicSize.Min),
                shape = RoundedCornerShape(20.dp),
                value = recipientAddress.value,
                onValueChange = { recipientAddress.value = it },
                singleLine = true,
                placeholder = { Text(text = "Enter a bitcoin testnet address") },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = padawan_theme_background_secondary,
                    cursorColor = padawan_theme_onPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    Row {
                        VerticalTextFieldDivider()
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.QRScanScreen.route) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_camera),
                                contentDescription = "Scan QR Icon",
                            )
                        }
                    }
                }
            )

            Text(
                text = "Fees",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
            TextField(
                modifier = Modifier
                    .wideTextField()
                    .height(IntrinsicSize.Min),
                shape = RoundedCornerShape(20.dp),
                value = feeRate.value,
                onValueChange = { value -> feeRate.value = value.filter { it.isDigit() } },
                singleLine = true,
                placeholder = { Text(text = "Edit fees") },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = padawan_theme_background_secondary,
                    cursorColor = padawan_theme_onPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
            )

            Button(
                onClick = {
                    val inputsAreValid = verifyInputs(recipientAddress.value, amount.value, feeRate.value, scope, bottomSheetScaffoldState.snackbarHostState)
                    try {
                        if (inputsAreValid) {
                            val txBR = Wallet.createTransaction(recipientAddress.value, amount.value.toULong(), feeRate.value.toFloat())
                            txBuilderResult.value = txBR
                            coroutineScope.launch {
                                if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                                    bottomSheetScaffoldState.bottomSheetState.expand()
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        scope.launch {
                            bottomSheetScaffoldState.snackbarHostState.showSnackbar(message = "$exception", duration = SnackbarDurationM2.Short)
                        }
                    }

                },
                colors = ButtonDefaults.buttonColors(containerColor = padawan_theme_button_primary),
                shape = RoundedCornerShape(20.dp),
                border = standardBorder,
                modifier = Modifier
                    .padding(top = 32.dp, start = 4.dp, end = 4.dp, bottom = 24.dp)
                    .standardShadow(20.dp)
                    .height(70.dp)
                    .width(240.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Verify transaction",
                        color = Color(0xff000000)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionConfirmation(
    txBuilderResult: MutableState<TxBuilderResult?>,
    recipientAddress: MutableState<String>,
    feeRate: MutableState<String>,
    snackbarHostState: BottomSheetScaffoldState,
    scope: CoroutineScope,
    navController: NavHostController,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Confirm Transaction",
                fontSize = 24.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Amount",
                fontSize = 20.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${txBuilderResult.value?.transactionDetails?.sent ?: 0}",
                fontSize = 16.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Address",
                fontSize = 20.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$recipientAddress",
                fontSize = 16.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Fee",
                fontSize = 20.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$feeRate",
                fontSize = 16.sp,
            )
        }

        Button(
            onClick = {
                val psbt = txBuilderResult.value?.psbt ?: throw Exception()
                broadcastTransaction(psbt, snackbarHostState.snackbarHostState, scope)
                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = padawan_theme_button_primary),
            shape = RoundedCornerShape(20.dp),
            border = standardBorder,
            modifier = Modifier
                .padding(top = 32.dp, start = 4.dp, end = 4.dp, bottom = 24.dp)
                .standardShadow(20.dp)
                .height(70.dp)
                .width(240.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Confirm and broadcast",
                    color = Color(0xff000000)
                )
            }
        }
    }
}

// @Composable
// fun Dialog(
//     recipientAddress: String,
//     amount: String,
//     feeRate: String,
//     showDialog: Boolean,
//     setShowDialog: (Boolean) -> Unit,
//     snackbarHostState: SnackbarHostState,
//     scope: CoroutineScope,
// ) {
//     if (showDialog) {
//         AlertDialog(
//             containerColor = md_theme_dark_background2,
//             onDismissRequest = {},
//             title = {
//                 Text(
//                     text = "Confirm transaction",
//                 )
//             },
//             text = {
//                 Text(
//                     text = "Send: $amount satoshis \nto: $recipientAddress\nFee rate: $feeRate"
//                 )
//             },
//             confirmButton = {
//                 TextButton(
//                     onClick = {
//                         broadcastTransaction(
//                             recipientAddress = recipientAddress,
//                             amount = amount,
//                             feeRate = feeRate,
//                             snackbarHostState = snackbarHostState,
//                             scope = scope,
//                         )
//                         setShowDialog(false)
//                     },
//                 ) {
//                     Text(
//                         text = "Confirm",
//                     )
//                 }
//             },
//             dismissButton = {
//                 TextButton(
//                     onClick = {
//                         setShowDialog(false)
//                     },
//                 ) {
//                     Text(
//                         text = "Cancel",
//                     )
//                 }
//             },
//         )
//     }
// }

fun verifyInputs(
    recipientAddress: String,
    amount: String,
    feeRate: String,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostStateM2,
): Boolean {
    if (amount.isBlank()) {
        scope.launch {
            snackbarHostState.showSnackbar(message = "Amount is missing!", duration = SnackbarDurationM2.Short)
        }
        return false
    }
    if (recipientAddress.isBlank()) {
        scope.launch {
            Log.i(TAG, "Showing snackbar for missing address")
            snackbarHostState.showSnackbar(message = "Address is missing!", duration = SnackbarDurationM2.Short)
        }
        return false
    }
    if (feeRate.isBlank()) {
        scope.launch {
            snackbarHostState.showSnackbar(message = "Fee Rate is missing!", duration = SnackbarDurationM2.Short)
        }
        return false
    }
    if (feeRate.toInt() < 1 || feeRate.toInt() > 200) {
        scope.launch {
            snackbarHostState.showSnackbar(message = "Please input a fee rate between 1 and 200", duration = SnackbarDurationM2.Short)
        }
        return false
    }
    return true
}

private fun broadcastTransaction(
    psbt: PartiallySignedTransaction,
    snackbarHostState: SnackbarHostStateM2,
    scope: CoroutineScope,
) {
    val snackbarMsg: String = try {
        Wallet.sign(psbt)
        Wallet.broadcast(psbt)
        "Transaction was broadcast successfully"
    } catch (e: Throwable) {
        Log.i(TAG, "Broadcast error: ${e.message}")
        "Error: ${e.message}"
    }
    scope.launch {
        snackbarHostState.showSnackbar(message = snackbarMsg, duration = SnackbarDurationM2.Short)
    }
}
