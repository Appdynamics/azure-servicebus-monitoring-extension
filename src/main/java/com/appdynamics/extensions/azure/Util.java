package com.appdynamics.extensions.azure;

import com.appdynamics.extensions.azure.Metrics.MetricCollectorUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Util {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricCollectorUtils.class);
    static String convertToString(final Object field,final String defaultStr){
        if(field == null){
            return defaultStr;
        }
        return field.toString();
    }

    protected String getSASToken(String resourceUri, String keyName, String key) {
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

            hash = BaseEncoding.base64().encode(sha256_HMAC.doFinal(input.getBytes("UTF-8")));

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