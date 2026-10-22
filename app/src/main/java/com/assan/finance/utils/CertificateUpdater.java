package com.assan.finance.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.assan.finance.apis.ApiManager;

public class CertificateUpdater {

    private static final String SERVER_CERT_URL = ApiManager.BASE_URL + "certificates/assancard.crt";
    private static final String LOCAL_CERT_FILENAME = "assancard.crt";

    public static void checkAndDownloadCertificate(Context context) {
        new DownloadCertificateTask(context).execute(SERVER_CERT_URL);
    }

    private static class DownloadCertificateTask extends AsyncTask<String, Void, Boolean> {
        private Context context;

        public DownloadCertificateTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String certUrl = params[0];
            try {
                URL url = new URL(certUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e("CertificateUpdater", "Server returned HTTP " + connection.getResponseCode());
                    return false;
                }

                // Save the certificate in app storage
                InputStream inputStream = connection.getInputStream();
                File certFile = new File(context.getFilesDir(), LOCAL_CERT_FILENAME);
                FileOutputStream outputStream = new FileOutputStream(certFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                Log.d("CertificateUpdater", "Certificate downloaded and updated successfully.");
                return true;
            } catch (Exception e) {
                Log.e("CertificateUpdater", "Error downloading certificate", e);
                return false;
            }
        }
    }
}
