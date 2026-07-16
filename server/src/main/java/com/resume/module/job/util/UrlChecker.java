package com.resume.module.job.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class UrlChecker {

    private static final Logger logger = LoggerFactory.getLogger(UrlChecker.class);

    private static final int DEFAULT_TIMEOUT = 5000;

    public boolean isUrlAccessible(String urlStr) {
        return isUrlAccessible(urlStr, DEFAULT_TIMEOUT);
    }

    public boolean isUrlAccessible(String urlStr, int timeoutMs) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            return false;
        }

        try {
            URL url = new URL(urlStr);
            String protocol = url.getProtocol().toLowerCase();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        boolean result = checkUrlWithMethod(urlStr, timeoutMs, "HEAD");
        if (!result) {
            result = checkUrlWithMethod(urlStr, timeoutMs, "GET");
        }
        return result;
    }

    private boolean checkUrlWithMethod(String urlStr, int timeoutMs, String method) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode == 405) {
                return false;
            }
            return responseCode >= 200 && responseCode < 400;
        } catch (IOException e) {
            logger.debug("URL check failed for {} with method {}: {}", urlStr, method, e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}