package com.tbm.admin.service.telegram;

import com.tbm.admin.config.AesConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PasswordTests {

    @Autowired
    AesConfig aesConfig;

    @Test
    public void test() throws Exception {

        System.out.println(aesConfig.encryption("6#mv)9cu"));
        System.out.println(aesConfig.encryption("8)cf*6ai"));
        System.out.println(aesConfig.encryption("9$mn&1qc"));
        System.out.println(aesConfig.encryption("5^mu@7gp"));
        System.out.println(aesConfig.encryption("1!le@3jy"));
        System.out.println(aesConfig.encryption("9)oe@7wj"));
        System.out.println(aesConfig.encryption("9#am%0eo"));
        System.out.println(aesConfig.encryption("0)ik^6ai"));
        System.out.println(aesConfig.encryption("1(sh$1kx"));
        System.out.println(aesConfig.encryption("0$za(4yo"));
        System.out.println(aesConfig.encryption("5#vo%3um"));
        System.out.println(aesConfig.encryption("7#nb%6hl"));
        System.out.println(aesConfig.encryption("6%tg^1dt"));
        System.out.println(aesConfig.encryption("9$mn&1qc"));
        System.out.println(aesConfig.encryption("3&mu^1en"));
        System.out.println(aesConfig.encryption("0%cg@8xr"));
        System.out.println(aesConfig.encryption("8!uv&7yy"));
        System.out.println(aesConfig.encryption("8(lq#0eu"));
        System.out.println(aesConfig.encryption("0%eg&6zj"));
        System.out.println(aesConfig.encryption("9&cz@9wv"));
    }
}
