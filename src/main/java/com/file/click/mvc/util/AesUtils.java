package com.file.click.mvc.util;

import com.file.click.mvc.config.MainConfig;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;

public class AesUtils {
    public static byte[] decode(String base64) throws Exception {
        return Base64.decode(base64);
    }

    public static String encode(byte[] bytes) throws Exception {
        return new String(Base64.encode(bytes));
    }

    public static Key toKey(byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, MainConfig.ALGORITHM);
        return secretKey;
    }
}
