package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.google.common.io.BaseEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Utility {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(Utility.class);

    public static String getStringResponseFromUrl(CloseableHttpClient httpClient, String namespace, String serviceBusRootUri, String keyName, String key, String endpoint) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", getSASToken(namespace, serviceBusRootUri, keyName, key));

        final String url = "https://" + namespace + serviceBusRootUri + endpoint;
        String responseData = HttpClientUtils.getResponse(httpClient, url, new HttpClientUtils.ResponseConverter<String>() {
            public String convert(HttpEntity entity) {
                try {
                    String response = EntityUtils.toString(entity);
                    if (logger.isDebugEnabled()) {
                        logger.debug("The response of the url [{}]  is [{}]", url, response);
                    }
                    return response;
                } catch (IOException e) {
                    logger.error("Error while converting response of url [" + url + "] to string " + entity, e);
                    return null;
                }
            }
        }, headers);

        return responseData;
    }

    private static String getSASToken(String namespace, String serviceBusRootUri, String keyName, String key) {
        String resourceUri = "https://" + namespace + serviceBusRootUri;
        long epoch = System.currentTimeMillis() / 1000L;
        int week = 60 * 60 * 24 * 7;
        String expiry = Long.toString(epoch + week);

        String sasToken = null;
        try {
            String stringToSign = URLEncoder.encode(resourceUri, "UTF-8") + "\n" + expiry;
            String signature = getHMAC256(key, stringToSign);
            sasToken = "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, "UTF-8") + "&sig=" +
                    URLEncoder.encode(signature, "UTF-8") + "&se=" + expiry + "&skn=" + keyName;
        } catch (UnsupportedEncodingException e) {
            logger.error("Error trying to generate SAS token", e);
            throw new RuntimeException("Error trying to generate SAS token", e);
        }

        return sasToken;
    }


    private static String getHMAC256(String key, String input) {
        Mac sha256_HMAC = null;
        String hash = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            hash = new String(BaseEncoding.base64().encode(sha256_HMAC.doFinal(input.getBytes("UTF-8"))));

        } catch (InvalidKeyException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        } catch (IllegalStateException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        }

        return hash;
    }

}
