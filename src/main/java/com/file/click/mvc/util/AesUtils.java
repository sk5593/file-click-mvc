package com.file.click.mvc.util;

import com.file.click.mvc.config.MainConfig;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
/**
 * 对称加密AES工具类
 */
public class AesUtils {
    public static Key toKey(byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, MainConfig.ALGORITHM);
        return secretKey;
    }
}
