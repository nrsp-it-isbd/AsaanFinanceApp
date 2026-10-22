package com.assan.finance.network.assan

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.assan.finance.BuildConfig
import com.assan.finance.network.SslHelper
import android.content.Context

object AssanRetrofitClient {
    private val BASE_URL = com.assan.finance.apis.ApiManager.BASE_URL

    @Volatile
    private var mainRetrofit: Retrofit? = null
    private lateinit var appContext: Context

    /** Builds the client now, on the calling thread. */
    fun init(context: Context) {
        appContext = context.applicationContext
        retrofit()
    }

    /**
     * Builds the client on a background thread (it loads the SSL certificate, ~0.4 s), so app start
     * and the splash are not held up. A call that needs it earlier simply builds it then.
     */
    fun initAsync(context: Context) {
        appContext = context.applicationContext
        Thread({ retrofit() }, "retrofit-init").start()
    }

    private fun retrofit(): Retrofit = mainRetrofit ?: synchronized(this) {
        mainRetrofit ?: Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(SslHelper.buildOkHttpClient(context = appContext, enableLogging = BuildConfig.DEBUG))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .also { mainRetrofit = it }
    }

    val apiService: AssanApiService
        get() {
            check(::appContext.isInitialized) { "AssanRetrofitClient not initialized" }
            return retrofit().create(AssanApiService::class.java)
        }
}
