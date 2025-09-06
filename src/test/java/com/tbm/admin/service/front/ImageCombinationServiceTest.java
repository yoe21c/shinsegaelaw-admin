package com.tbm.admin.service.front;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("local")
class ImageCombinationServiceTest {

    @Autowired
    private ImageCombinationService imageCombinationService;

    @Test
    void combineImages() throws IOException {
        File testImage = new File("./images/test.jpg");
        MockMultipartFile mockMultipartFile = new MockMultipartFile("test.jpg", new FileInputStream(testImage));

        File[] files = new File("./images/background").listFiles();
        assertNotNull(files);

        for (int i = 0; i < files.length; i++) {
            if(i == 3) break;
            imageCombinationService.mergeImage(files[i], mockMultipartFile, 80, 50, i);
        }
    }
}