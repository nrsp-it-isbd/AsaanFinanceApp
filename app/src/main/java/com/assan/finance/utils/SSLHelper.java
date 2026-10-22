package com.assan.finance.utils;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLHelper {

    public static SSLConfig getSSLConfig(Context context) {
        try {File certFile = new File(context.getFilesDir(), "assancard.crt");
            InputStream certInputStream = new FileInputStream(certFile);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca = cf.generateCertificate(certInputStream);
            certInputStream.close();

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Extract the X509TrustManager
            X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];

            return new SSLConfig(sslContext, trustManager);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class SSLConfig {
        public final SSLContext sslContext;
        public final X509TrustManager trustManager;

        public SSLConfig(SSLContext sslContext, X509TrustManager trustManager) {
            this.sslContext = sslContext;
            this.trustManager = trustManager;
        }
    }
}
