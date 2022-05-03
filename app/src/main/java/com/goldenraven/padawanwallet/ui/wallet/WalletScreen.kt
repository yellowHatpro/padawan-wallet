package com.goldenraven.padawanwallet.ui.wallet

import com.goldenraven.padawanwallet.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.goldenraven.padawanwallet.data.Wallet
import com.goldenraven.padawanwallet.theme.*
import com.goldenraven.padawanwallet.ui.Screen
import com.goldenraven.padawanwallet.utils.formatInBtc
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// internal class RefreshViewModel() : ViewModel() {
//     private val _isRefreshing = MutableStateFlow(false)
//
//     val isRefreshing: StateFlow<Boolean>
//         get() = _isRefreshing.asStateFlow()
//
//     fun refresh() {
//         // This doesn't handle multiple 'refreshing' tasks, don't use this
//         viewModelScope.launch {
//             // A fake 2 second 'refresh'
//             _isRefreshing.emit(true)
//
//             delay(2000)
//             _isRefreshing.emit(false)
//         }
//     }
// }

@Composable
internal fun WalletScreen(
    walletViewModel: WalletViewModel = viewModel(),
    navController: NavHostController
) {
    val balance by walletViewModel.balance.observeAsState()
    // walletViewModel.updateBalance()

    // val refreshViewModel: RefreshViewModel = viewModel()
    val isRefreshing by walletViewModel.isRefreshing.collectAsState()

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { walletViewModel.refresh() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(md_theme_dark_background)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.padding(24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(color = md_theme_dark_background2)
                    .height(110.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sat),
                    contentDescription = "Bitcoin testnet logo",
                    Modifier
                        .align(Alignment.CenterVertically)
                        .size(60.dp)
                )
                Text(
                    balance.toString(),
                    fontFamily = shareTechMono,
                    fontSize = 32.sp,
                )
            }
            // Spacer(Modifier.padding(24.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.SendScreen.route) },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(80.dp)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "Send",
                        fontFamily = jost,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                    )
                }
                Button(
                    onClick = { navController.navigate(Screen.ReceiveScreen.route) },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(80.dp)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "Receive",
                        fontFamily = jost,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

internal class WalletViewModel() : ViewModel() {

    private var _balance: MutableLiveData<ULong> = MutableLiveData(0u)
    val balance: LiveData<ULong>
        get() = _balance

    private fun updateBalance() {
        Wallet.sync()
        _balance.value = Wallet.getBalance()
    }

    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

    fun refresh() {
        // This doesn't handle multiple 'refreshing' tasks, don't use this
        viewModelScope.launch {
            // A fake 2 second 'refresh'
            _isRefreshing.emit(true)
            updateBalance()
            delay(300)
            _isRefreshing.emit(false)
        }
    }
}

