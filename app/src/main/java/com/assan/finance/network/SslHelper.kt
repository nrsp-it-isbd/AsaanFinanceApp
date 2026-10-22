package com.assan.finance.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import android.util.Log

/**
 * Builds a fully configured OkHttpClient that trusts the bundled
 * server certificate (assets/certificate.crt) AND the system CAs.
 *
 * Strategy:
 *  1. Load the PEM cert from assets
 *  2. Build a KeyStore containing BOTH the custom cert AND system CAs
 *  3. Create a TrustManager from that KeyStore
 *  4. Configure OkHttp with the resulting SSLSocketFactory
 *
 * This allows the app to connect to the backend without
 * "javax.net.ssl.SSLHandshakeException: Certificate not trusted" errors.
 */
object SslHelper {

    private const val CERT_ASSET = "certificate.crt"

    /**
     * Create an OkHttpClient with the custom SSL trust store.
     * Falls back to default OkHttpClient if certificate loading fails.
     */
    fun buildOkHttpClient(context: Context, enableLogging: Boolean = false): OkHttpClient {
        return try {
            buildClientWithCustomSsl(context, enableLogging)
        } catch (e: Exception) {
            Log.e("SslHelper", "Custom SSL setup failed, using default client: ${e.message}")
            buildDefaultClient(enableLogging)
        }
    }

    private fun buildClientWithCustomSsl(context: Context, enableLogging: Boolean): OkHttpClient {
        // 1. Load our bundled certificate
        val certFactory = CertificateFactory.getInstance("X.509")
        val certStream: InputStream = context.assets.open(CERT_ASSET)
        val serverCert = certFactory.generateCertificate(certStream) as X509Certificate
        certStream.close()

        // 2. Build a KeyStore that contains BOTH system CAs and our custom cert
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)

            // Add system trusted CAs
            val systemTmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            systemTmf.init(null as KeyStore?)
            val systemTm = systemTmf.trustManagers
                .filterIsInstance<X509TrustManager>()
                .firstOrNull()

            systemTm?.acceptedIssuers?.forEachIndexed { i, cert ->
                setCertificateEntry("system_ca_$i", cert)
            }

            // Add our custom server certificate
            setCertificateEntry("nrsp_server_cert", serverCert)
        }

        // 3. Build TrustManager from the combined KeyStore
        val tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        ).apply { init(keyStore) }

        val trustManagers = tmf.trustManagers
        val x509TrustManager = trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()

        // 4. Configure SSLContext
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(x509TrustManager), null)
        }

        return buildClientBase(enableLogging)
            .sslSocketFactory(sslContext.socketFactory, x509TrustManager)
            .build()
    }

    private fun buildDefaultClient(enableLogging: Boolean): OkHttpClient {
        return buildClientBase(enableLogging).build()
    }

    private fun buildClientBase(enableLogging: Boolean): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .apply {
                if (enableLogging) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
    }
}
