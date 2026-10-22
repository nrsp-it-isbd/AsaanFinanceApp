package com.assan.finance.apis;

public class ApiManager {
    public static final String SERVER_IP = "http://103.172.140.103:999";
    public static final String BASE_URL = SERVER_IP + "/api/";
    public static final String IMAGE_BASE_URL = BASE_URL;

    public static String getFullImageUrl(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        String cleanPath = path.trim().replace("\\", "/");

        if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
            return cleanPath.replace("/api/uploads/", "/api/images/")
                            .replace("/uploads/", "/api/images/")
                            .replace("/api/images/api/images/", "/api/images/");
        }

        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        if (cleanPath.startsWith("api/")) {
            cleanPath = cleanPath.substring("api/".length());
        }

        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        if (cleanPath.startsWith("uploads/")) {
            cleanPath = cleanPath.substring("uploads/".length());
        } else if (cleanPath.startsWith("images/")) {
            cleanPath = cleanPath.substring("images/".length());
        }

        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        return SERVER_IP + "/api/images/" + cleanPath;
    }
}