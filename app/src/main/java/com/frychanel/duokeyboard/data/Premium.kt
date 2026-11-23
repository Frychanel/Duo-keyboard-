package com.frychanel.duokeyboard.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Compra única "DuoKeyboard Premium" con Google Play Billing.
 * El estado se guarda en local para que el teclado lo sepa al instante.
 *
 * En Play Console crea un producto integrado (compra única) con el ID [PRODUCTO].
 */
object Premium {
    const val PRODUCTO = "duokeyboard_premium"

    private const val PREFS = "duo_premium"
    private const val KEY_COMPRADO = "comprado"
    private const val KEY_PRUEBA = "modo_prueba"

    private val _activo = MutableStateFlow(false)
    val activo: StateFlow<Boolean> = _activo.asStateFlow()

    private val _precio = MutableStateFlow<String?>(null)
    val precio: StateFlow<String?> = _precio.asStateFlow()

    private val _modoPrueba = MutableStateFlow(false)
    val modoPrueba: StateFlow<Boolean> = _modoPrueba.asStateFlow()

    private var prefs: SharedPreferences? = null
    private var cliente: BillingClient? = null
    private var detalles: ProductDetails? = null
    private var comprado = false

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        comprado = p.getBoolean(KEY_COMPRADO, false)
        _modoPrueba.value = p.getBoolean(KEY_PRUEBA, false)
        recalcular()

        cliente = BillingClient.newBuilder(context.applicationContext)
            .setListener { resultado, compras ->
                if (resultado.responseCode == BillingClient.BillingResponseCode.OK && compras != null) {
                    procesar(compras, completo = false)
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        conectar()
    }

    private fun conectar() {
        cliente?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(resultado: BillingResult) {
                if (resultado.responseCode == BillingClient.BillingResponseCode.OK) {
                    consultarCompras()
                    consultarProducto()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun consultarCompras() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        cliente?.queryPurchasesAsync(params) { resultado, compras ->
            if (resultado.responseCode == BillingClient.BillingResponseCode.OK) procesar(compras, completo = true)
        }
    }

    private fun consultarProducto() {
        val producto = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCTO)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(producto)).build()
        cliente?.queryProductDetailsAsync(params) { resultado, lista ->
            if (resultado.responseCode == BillingClient.BillingResponseCode.OK) {
                detalles = lista.firstOrNull()
                _precio.value = detalles?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    /** @return false si la tienda todavía no está lista (producto sin dar de alta, sin conexión…). */
    fun comprar(activity: Activity): Boolean {
        val d = detalles ?: run { conectar(); return false }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        return cliente?.launchBillingFlow(activity, params)?.responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Solo para pruebas en versiones debug. */
    fun activarModoPrueba(activo: Boolean) {
        _modoPrueba.value = activo
        prefs?.edit()?.putBoolean(KEY_PRUEBA, activo)?.apply()
        recalcular()
    }

    private fun procesar(compras: List<Purchase>, completo: Boolean) {
        val nuestras = compras.filter { PRODUCTO in it.products }
        val pagadas = nuestras.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        pagadas.filter { !it.isAcknowledged }.forEach { compra ->
            val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(compra.purchaseToken).build()
            cliente?.acknowledgePurchase(ack) { }
        }
        // En la consulta completa, si no aparece la compra (p. ej. reembolso) se quita Premium.
        if (pagadas.isNotEmpty()) guardarComprado(true) else if (completo) guardarComprado(false)
    }

    private fun guardarComprado(valor: Boolean) {
        comprado = valor
        prefs?.edit()?.putBoolean(KEY_COMPRADO, valor)?.apply()
        recalcular()
    }

    private fun recalcular() {
        _activo.value = comprado || _modoPrueba.value
    }
}
