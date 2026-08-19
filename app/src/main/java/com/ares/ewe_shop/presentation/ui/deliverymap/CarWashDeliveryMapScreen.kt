package com.ares.ewe_shop.presentation.ui.deliverymap

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_shop.R
import com.ares.ewe_shop.core.theme.DobbyShopColors
import com.ares.ewe_shop.presentation.ui.components.SixDigitCodeField
import com.ares.ewe_shop.presentation.viewmodel.deliverymap.CarWashDeliveryMapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MARKER_ICON_SIZE_DP = 39 // −10% extra sobre 43dp (parity Dobby)
private const val DELIVERY_MARKER_ICON_SIZE_DP = 39 // −5% extra sobre 41dp (repartidor/moto)
/** Carro negro: 25% más chico que [MARKER_ICON_SIZE_DP]. */
private const val CAR_MARKER_ICON_SIZE_DP = 29

private fun bitmapDescriptorFromRes(
    context: Context,
    resId: Int,
    sizeDp: Int = MARKER_ICON_SIZE_DP,
): BitmapDescriptor? {
    return try {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
        drawable.setBounds(0, 0, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.draw(Canvas(bitmap))
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (_: Exception) {
        // BitmapDescriptorFactory may not be ready until MapsInitializer / GoogleMap load.
        null
    }
}

private fun formatEtaDisplay(eta: String?): String {
    if (eta.isNullOrBlank()) return "--"
    return eta
        .replace(" mins", " min", ignoreCase = true)
        .replace(" minutos", " min", ignoreCase = true)
}

/**
 * Mapa de entrega para carwash: ruta del local al domicilio del cliente, "Ya llegué"
 * cuando el GPS está en el punto de entrega y PIN de 6 dígitos para cerrar la entrega.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarWashDeliveryMapScreen(
    onBack: () -> Unit,
    onDeliveredSuccess: () -> Unit,
    viewModel: CarWashDeliveryMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var houseIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var shopIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var carIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var deliveryIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    BackHandler(onBack = onBack)

    // La activity usa adjustNothing; aquí el teclado debe empujar el panel del PIN.
    val window = (context as? Activity)?.window
    @Suppress("DEPRECATION")
    val resizeSoftInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    DisposableEffect(window) {
        val previousMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(resizeSoftInputMode)
        onDispose {
            previousMode?.let { window.setSoftInputMode(it) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.startLocationTracking()
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.startLocationTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    fun loadMarkerIcons() {
        if (houseIcon == null) {
            houseIcon = bitmapDescriptorFromRes(context, R.drawable.ic_house)
        }
        if (shopIcon == null) {
            shopIcon = bitmapDescriptorFromRes(context, R.drawable.ic_car_wash)
        }
        if (carIcon == null) {
            carIcon = bitmapDescriptorFromRes(context, R.drawable.ic_car, CAR_MARKER_ICON_SIZE_DP)
        }
        if (deliveryIcon == null) {
            deliveryIcon = bitmapDescriptorFromRes(
                context,
                R.drawable.ic_delivery,
                DELIVERY_MARKER_ICON_SIZE_DP,
            )
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Otro dispositivo avanzó el pedido: salir del mapa (salvo rating post-entrega o preview).
    LaunchedEffect(uiState.orderStatus, uiState.showCustomerRating, uiState.isLoading, uiState.isRoutePreview) {
        val status = uiState.orderStatus ?: return@LaunchedEffect
        if (uiState.isLoading || uiState.showCustomerRating) return@LaunchedEffect
        if (uiState.isRoutePreview) return@LaunchedEffect
        if (status !in setOf("OUT_FOR_PICKUP", "PICKED_UP", "ON_DELIVERY", "DELIVERED")) {
            onDeliveredSuccess()
        }
    }

    val cameraPositionState = rememberCameraPositionState()
    val shop = uiState.shopLatLng
    val customer = uiState.customerLatLng
    val current = uiState.currentLocation
    var centeredOnShop by remember { mutableStateOf(false) }
    var centeredOnGps by remember { mutableStateOf(false) }

    // Al cargar: primero el local; cuando llega el GPS, centrar ahí (donde estoy).
    LaunchedEffect(shop, current) {
        delay(350)
        when {
            current != null && !centeredOnGps -> {
                runCatching {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(current, 15.5f))
                }.onFailure {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(current, 15.5f)
                }
                centeredOnGps = true
                centeredOnShop = true
            }
            shop != null && !centeredOnShop -> {
                runCatching {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(shop, 15.5f))
                }.onFailure {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(shop, 15.5f)
                }
                centeredOnShop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DobbyShopColors.Background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        topBar = {
            CarWashDeliveryTopBar(
                onBack = onBack,
                title = when {
                    uiState.isRoutePreview -> "Ruta al cliente"
                    uiState.isPickupTrip -> "En camino a recoger"
                    uiState.isReturnToShopTrip -> "Regreso al autolavado"
                    else -> "Entrega en camino"
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading && customer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DobbyShopColors.Purple)
                    Text(
                        text = "Cargando mapa...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DobbyShopColors.TextSecondary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            return@Scaffold
        }

        val imeVisible = WindowInsets.isImeVisible
        val focusLatLng: LatLng? = current ?: shop

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Solo IME: el sheet aplica navigationBarsPadding para anclarse al fondo.
                .windowInsetsPadding(WindowInsets.ime),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapType = MapType.NORMAL,
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                ),
                onMapLoaded = { loadMarkerIcons() },
            ) {
                if (uiState.routePoints.isNotEmpty()) {
                    Polyline(
                        points = uiState.routePoints,
                        color = DobbyShopColors.Purple,
                        width = 10f,
                    )
                }
                shop?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = uiState.shopName ?: "Tu local",
                        icon = shopIcon ?: bitmapDescriptorFromRes(context, R.drawable.ic_car_wash),
                    )
                }
                customer?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = "Domicilio del cliente",
                        snippet = uiState.deliveryAddress,
                        icon = houseIcon ?: bitmapDescriptorFromRes(context, R.drawable.ic_house),
                    )
                }
                // En preview solo carwash + casa; en viaje activo: GPS del repartidor.
                // En camino a recoger → moto (ic_delivery); con carro / entrega → auto.
                if (!uiState.isRoutePreview) {
                    uiState.currentLocation?.let { latLng ->
                        val useDeliveryScooter = uiState.isPickupTrip
                        val selfIcon = if (useDeliveryScooter) {
                            deliveryIcon
                                ?: bitmapDescriptorFromRes(
                                    context,
                                    R.drawable.ic_delivery,
                                    DELIVERY_MARKER_ICON_SIZE_DP,
                                )
                        } else {
                            carIcon ?: bitmapDescriptorFromRes(
                                context,
                                R.drawable.ic_car,
                                CAR_MARKER_ICON_SIZE_DP,
                            )
                        }
                        Marker(
                            state = MarkerState(position = latLng),
                            title = if (useDeliveryScooter) "Repartidor" else "Tu ubicación",
                            icon = selfIcon
                                ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        )
                    }
                }
            }

            DeliveryRouteInfoCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                etaText = formatEtaDisplay(uiState.etaText),
                distanceText = uiState.distanceText ?: "--",
                etaIsApproximate = uiState.etaIsApproximate,
            )

            if (customer == null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = DobbyShopColors.Surface,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = "Este pedido no tiene coordenadas de entrega en el mapa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobbyShopColors.TextSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Recenter pegado arriba del bottom sheet (como iOS / DobbyGo).
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                if (focusLatLng != null && !imeVisible) {
                    Surface(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(focusLatLng, 15.5f),
                                    )
                                }.onFailure {
                                    cameraPositionState.position =
                                        CameraPosition.fromLatLngZoom(focusLatLng, 15.5f)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 16.dp, bottom = 12.dp),
                        shape = CircleShape,
                        color = DobbyShopColors.Surface,
                        shadowElevation = 4.dp,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Centrar mapa",
                            tint = DobbyShopColors.Purple,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (customer != null &&
                    (uiState.isRoutePreview || !uiState.isDelivered || uiState.showCustomerRating)
                ) {
                    CarWashDeliveryBottomPanel(
                        modifier = Modifier.fillMaxWidth(),
                        deliveryAddress = uiState.deliveryAddress,
                        customerName = uiState.customerName,
                        hasMarkedArrived = uiState.hasMarkedArrived,
                        isNearCustomer = uiState.isNearCustomer,
                        isMarkingArrived = uiState.isMarkingArrived,
                        deliveryCodeInput = uiState.deliveryCodeInput,
                        deliveryCodeValid = uiState.deliveryCodeValid,
                        isVerifyingDeliveryCode = uiState.isVerifyingDeliveryCode,
                        isMarkingDelivered = uiState.isMarkingDelivered,
                        isPickupTrip = uiState.isPickupTrip,
                        isReturnToShopTrip = uiState.isReturnToShopTrip,
                        isRoutePreview = uiState.isRoutePreview,
                        isConfirmingPickup = uiState.isConfirmingPickup,
                        isStartingWash = uiState.isStartingWash,
                        showCustomerRating = uiState.showCustomerRating,
                        customerRatingStars = uiState.customerRatingStars,
                        customerPunctual = uiState.customerPunctual,
                        customerPaysWell = uiState.customerPaysWell,
                        customerTipped = uiState.customerTipped,
                        customerRecommended = uiState.customerRecommended,
                        isSubmittingCustomerRating = uiState.isSubmittingCustomerRating,
                        onDeliveryCodeChange = viewModel::onDeliveryCodeChange,
                        onMarkArrived = viewModel::markArrived,
                        onStartServiceAtShop = {
                            viewModel.startServiceAtShop(onSuccess = onDeliveredSuccess)
                        },
                        onMarkDelivered = { viewModel.markDelivered(onSuccess = onDeliveredSuccess) },
                        onStarsChange = viewModel::setCustomerRatingStars,
                        onTogglePunctual = viewModel::toggleCustomerPunctual,
                        onTogglePaysWell = viewModel::toggleCustomerPaysWell,
                        onToggleTipped = viewModel::toggleCustomerTipped,
                        onToggleRecommended = viewModel::toggleCustomerRecommended,
                        onSkipCustomerRating = viewModel::skipCustomerRating,
                        onSubmitCustomerRating = viewModel::submitCustomerRating,
                    )
                }
            }
        }
    }
}

@Composable
private fun CarWashDeliveryTopBar(onBack: () -> Unit, title: String) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DobbyShopColors.Surface)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = DobbyShopColors.TextPrimary,
                )
            }
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DobbyShopColors.TextPrimary,
            )
        }
        HorizontalDivider(color = DobbyShopColors.Border, thickness = 1.dp)
    }
}

@Composable
private fun DeliveryRouteInfoCard(
    etaText: String,
    distanceText: String,
    etaIsApproximate: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeliveryRouteStatRow(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccessTime,
                    label = "Llegada estimada",
                    value = etaText,
                    valueColor = DobbyShopColors.Purple,
                )
                DeliveryRouteStatRow(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Navigation,
                    label = "Distancia de la ruta",
                    value = distanceText,
                    valueColor = DobbyShopColors.TextPrimary,
                )
            }
            if (etaIsApproximate) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tiempo aproximado (sin tráfico)",
                    style = MaterialTheme.typography.labelSmall,
                    color = DobbyShopColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun DeliveryRouteStatRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DobbyShopColors.PurpleLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DobbyShopColors.Purple,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DobbyShopColors.TextSecondary,
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = valueColor,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarWashDeliveryBottomPanel(
    deliveryAddress: String?,
    customerName: String?,
    hasMarkedArrived: Boolean,
    isNearCustomer: Boolean,
    isMarkingArrived: Boolean,
    deliveryCodeInput: String,
    deliveryCodeValid: Boolean?,
    isVerifyingDeliveryCode: Boolean,
    isMarkingDelivered: Boolean,
    isPickupTrip: Boolean,
    isReturnToShopTrip: Boolean,
    isRoutePreview: Boolean,
    isConfirmingPickup: Boolean,
    isStartingWash: Boolean,
    showCustomerRating: Boolean,
    customerRatingStars: Int,
    customerPunctual: Boolean,
    customerPaysWell: Boolean,
    customerTipped: Boolean,
    customerRecommended: Boolean,
    isSubmittingCustomerRating: Boolean,
    onDeliveryCodeChange: (String) -> Unit,
    onMarkArrived: () -> Unit,
    onStartServiceAtShop: () -> Unit,
    onMarkDelivered: () -> Unit,
    onStarsChange: (Int) -> Unit,
    onTogglePunctual: () -> Unit,
    onTogglePaysWell: () -> Unit,
    onToggleTipped: () -> Unit,
    onToggleRecommended: () -> Unit,
    onSkipCustomerRating: () -> Unit,
    onSubmitCustomerRating: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val maxPanelHeight = (configuration.screenHeightDp * 0.72f).dp
    val scrollState = rememberScrollState()
    var sheetCollapsed by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showCustomerRating) {
        if (showCustomerRating) sheetCollapsed = false
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (sheetCollapsed) Modifier else Modifier.heightIn(max = maxPanelHeight),
            ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = DobbyShopColors.Surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .then(
                    if (sheetCollapsed) {
                        Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    } else {
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(sheetCollapsed) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                when {
                                    dragAccum > 48f -> sheetCollapsed = true
                                    dragAccum < -48f -> sheetCollapsed = false
                                }
                                dragAccum = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragAccum += dragAmount
                            },
                        )
                    }
                    .clickable { sheetCollapsed = !sheetCollapsed },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DobbyShopColors.Border),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (sheetCollapsed) "Mostrar detalles" else "Ocultar detalles",
                        style = MaterialTheme.typography.labelMedium,
                        color = DobbyShopColors.TextSecondary,
                    )
                    Icon(
                        imageVector = if (sheetCollapsed) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = DobbyShopColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (!sheetCollapsed) {
                Spacer(modifier = Modifier.height(14.dp))
                if (showCustomerRating) {
                    CustomerRatingSection(
                        stars = customerRatingStars,
                        punctual = customerPunctual,
                        paysWell = customerPaysWell,
                        tipped = customerTipped,
                        recommended = customerRecommended,
                        isSubmitting = isSubmittingCustomerRating,
                        onStarsChange = onStarsChange,
                        onTogglePunctual = onTogglePunctual,
                        onTogglePaysWell = onTogglePaysWell,
                        onToggleTipped = onToggleTipped,
                        onToggleRecommended = onToggleRecommended,
                        onSkip = onSkipCustomerRating,
                        onSubmit = onSubmitCustomerRating,
                    )
                } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DobbyShopColors.PurpleLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = DobbyShopColors.Purple,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isRoutePreview -> "Dirección del cliente"
                                isReturnToShopTrip -> "Dirección del autolavado"
                                isPickupTrip -> "Dirección del cliente"
                                else -> "Dirección de entrega"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DobbyShopColors.TextPrimary,
                        )
                        deliveryAddress?.takeIf { it.isNotBlank() }?.let { address ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DobbyShopColors.TextSecondary,
                                lineHeight = 20.sp,
                            )
                        }
                        customerName?.let { name ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cliente: $name",
                                style = MaterialTheme.typography.bodySmall,
                                color = DobbyShopColors.TextSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isRoutePreview) {
                    Text(
                        text = "Estimación del tiempo para ir a recoger el carro. La ruta usa tu ubicación actual si está disponible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobbyShopColors.TextSecondary,
                        lineHeight = 18.sp,
                    )
                } else if (isReturnToShopTrip) {
                    val canStartService = isNearCustomer && !isStartingWash && !isMarkingArrived
                    Button(
                        onClick = onStartServiceAtShop,
                        enabled = canStartService,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobbyShopColors.Purple,
                            disabledContainerColor = DobbyShopColors.Border,
                            contentColor = Color.White,
                            disabledContentColor = DobbyShopColors.TextSecondary,
                        ),
                    ) {
                        if (isStartingWash || isMarkingArrived) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Empezar a lavar",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                    if (!canStartService && !isStartingWash && !isMarkingArrived) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El botón se habilitará cuando estés en el autolavado.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DobbyShopColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else if (!hasMarkedArrived) {
                    val canMarkArrived = isNearCustomer && !isMarkingArrived
                    Button(
                        onClick = onMarkArrived,
                        enabled = canMarkArrived,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobbyShopColors.Purple,
                            disabledContainerColor = DobbyShopColors.Border,
                            contentColor = Color.White,
                            disabledContentColor = DobbyShopColors.TextSecondary,
                        ),
                    ) {
                        if (isMarkingArrived) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Ya llegué",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                    if (!canMarkArrived && !isMarkingArrived) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El botón se habilitará cuando estés en la ubicación del cliente.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DobbyShopColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Text(
                        text = "Código del cliente",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = DobbyShopColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SixDigitCodeField(
                        value = deliveryCodeInput,
                        onValueChange = onDeliveryCodeChange,
                    )
                    Text(
                        text = if (isPickupTrip) {
                            "Pide el código de 6 dígitos al cliente en la app Dobbi para recoger el carro."
                        } else {
                            "Pide el código de 6 dígitos al cliente en la app Dobbi."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DobbyShopColors.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    when {
                        deliveryCodeInput.length == 6 && isVerifyingDeliveryCode -> {
                            Text(
                                text = "Verificando código…",
                                style = MaterialTheme.typography.bodySmall,
                                color = DobbyShopColors.TextSecondary,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        deliveryCodeInput.length == 6 && deliveryCodeValid == false -> {
                            Text(
                                text = "Código incorrecto. Verifica con el cliente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DobbyShopColors.RedDark,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onMarkDelivered,
                        enabled = deliveryCodeValid == true &&
                            !isVerifyingDeliveryCode &&
                            !isMarkingDelivered &&
                            !isConfirmingPickup &&
                            !isStartingWash,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobbyShopColors.Purple,
                            disabledContainerColor = DobbyShopColors.Purple.copy(alpha = 0.5f),
                        ),
                    ) {
                        if (isMarkingDelivered || isConfirmingPickup) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (isPickupTrip) {
                                    "Carro recogido"
                                } else {
                                    "Confirmar entrega"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerRatingSection(
    stars: Int,
    punctual: Boolean,
    paysWell: Boolean,
    tipped: Boolean,
    recommended: Boolean,
    isSubmitting: Boolean,
    onStarsChange: (Int) -> Unit,
    onTogglePunctual: () -> Unit,
    onTogglePaysWell: () -> Unit,
    onToggleTipped: () -> Unit,
    onToggleRecommended: () -> Unit,
    onSkip: () -> Unit,
    onSubmit: () -> Unit,
) {
    Text(
        text = "¿Cómo estuvo el cliente?",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = DobbyShopColors.TextPrimary,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Opcional. Ayuda a mejorar la experiencia de entrega.",
        style = MaterialTheme.typography.bodySmall,
        color = DobbyShopColors.TextSecondary,
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..5).forEach { value ->
            val selected = stars >= value
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "$value estrellas",
                tint = if (selected) Color(0xFFF5A524) else DobbyShopColors.Border,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(enabled = !isSubmitting) { onStarsChange(value) }
                    .padding(4.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RatingTagChip("Usuario puntual", punctual, enabled = !isSubmitting, onClick = onTogglePunctual)
        RatingTagChip("Usuario paga bien", paysWell, enabled = !isSubmitting, onClick = onTogglePaysWell)
        RatingTagChip("Propina", tipped, enabled = !isSubmitting, onClick = onToggleTipped)
        RatingTagChip("Usuario recomendado", recommended, enabled = !isSubmitting, onClick = onToggleRecommended)
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onSubmit,
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DobbyShopColors.Purple,
            disabledContainerColor = DobbyShopColors.Purple.copy(alpha = 0.5f),
        ),
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "Enviar calificación",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White,
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onSkip,
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = "Omitir",
            fontWeight = FontWeight.SemiBold,
            color = DobbyShopColors.TextSecondary,
        )
    }
}

@Composable
private fun RatingTagChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
    )
}
