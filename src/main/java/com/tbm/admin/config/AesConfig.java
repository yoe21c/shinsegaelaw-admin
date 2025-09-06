package com.tbm.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AesConfig {

    @Value("${aes.secret-key}")
    private String secretKey;

    // 암호화
    public String encryption(String text) {
        try {
            Cipher cipher = Cipher.getInstance("AES");

            byte[] key = new byte[16];
            int i = 0;
            for(byte b : secretKey.getBytes()) {
                key[i++ % 16] ^= b;
            }

            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return new String(Hex.encodeHex(cipher.doFinal(text.getBytes("UTF-8")))).toUpperCase();

        } catch(Exception e) {
            throw new RuntimeException("AES 암호화 중 문제 발생");
        }
    }

    // 복호화
    public String decryption(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");

            byte[] key = new byte[16];
            int i = 0;
            for(byte b : secretKey.getBytes()) {
                key[i++ % 16] ^= b;
            }

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            return new String(cipher.doFinal(Hex.decodeHex(encryptedText.toCharArray())));

        } catch(Exception e) {
            throw new RuntimeException("AES 복호화 중 문제 발생");
        }
    }
}
