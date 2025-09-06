package com.tbm.admin.service.thirdparty;

import com.tbm.admin.config.security.Authed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class S3ServiceTest {

    @Autowired
    S3Service s3Service;

    @Test
    public void listObjects() {

        List<String> strings = s3Service.listObjects(null);
        System.out.println("strings = " + strings);

    }
}