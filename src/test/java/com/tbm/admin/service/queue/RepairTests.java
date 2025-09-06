package com.tbm.admin.service.queue;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RepairTests {

    private static final String SECRET_KEY = "your-16-char-key"; // 16자 비밀키

    @Test
    public void test() throws Exception {
        String code = "print(f'hello world !!!!!!!!!!!!!!!!!!!!!!')";
        String encrypted = encryptCode(code);
        System.out.println("Encrypted: " + encrypted);

        String decrypted = decryptCode(encrypted);
        System.out.println("Decrypted: " + decrypted);
    }

    private String encryptCode(String code) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptCode(String encrypted) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
