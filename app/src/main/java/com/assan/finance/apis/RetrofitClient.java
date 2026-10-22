package com.assan.finance.apis;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import com.assan.finance.utils.SSLHelper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient<T> {
    private final T apiService;

    public RetrofitClient(Class<T> serviceClass, Context context) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        // Load SSL certificate and TrustManager
        SSLHelper.SSLConfig sslConfig = SSLHelper.getSSLConfig(context);
        if (sslConfig != null) {
            httpClient.sslSocketFactory(sslConfig.sslContext.getSocketFactory(), sslConfig.trustManager);
        }

        // FIX: loan submission uploads 3 Base64 images; the 10s OkHttp default timed out on slow
        // rural networks (user retries → possible duplicate application). Allow more time.
        httpClient.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
        httpClient.writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS);
        httpClient.readTimeout(60, java.util.concurrent.TimeUnit.SECONDS);

        // FIX: BODY logging was always on, printing CNIC images, CNIC numbers, phone and bank
        // account numbers to logcat in release builds. Debug builds only now.
        if (com.assan.finance.BuildConfig.DEBUG) {
            // HEADERS only: BODY printed ~250 KB of base64 images per loan submit, which exceeded
            // the device log quota so the server's reply was dropped from logcat.
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            httpClient.addInterceptor(logging);

            // Log the response body (and failures) separately, under the API_RESPONSE tag
            httpClient.addInterceptor(chain -> {
                okhttp3.Request request = chain.request();
                try {
                    okhttp3.Response response = chain.proceed(request);
                    String body = response.peekBody(64 * 1024).string();
                    android.util.Log.d("API_RESPONSE", response.code() + " " + request.url() + " -> " + body);
                    return response;
                } catch (IOException e) {
                    android.util.Log.e("API_RESPONSE", "FAILED " + request.url() + " -> " + e);
                    throw e;
                }
            });
        }

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiManager.BASE_URL)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(serviceClass);
    }

    public T getApiService() {
        return apiService;
    }

    public <U> U executeRequest(Call<U> call) throws IOException {
        return call.execute().body();
    }

    public <U> void enqueueRequest(Call<U> call, retrofit2.Callback<U> callback) {
        call.enqueue(callback);
    }
}
