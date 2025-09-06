package com.tbm.admin.service.thirdparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * S3 내 객체 목록 조회
     */
    public List<String> listObjects(String prefix) {
        ListObjectsV2Request request;
        if(prefix != null) {
            request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
        } else {
            request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
        }

        ListObjectsV2Response result = s3Client.listObjectsV2(request);
        return result.contents().stream()
                .sorted((o1, o2) -> o2.lastModified().compareTo(o1.lastModified())) // 최신순 정렬
                .map(S3Object::key)
                .limit(20) // 20개로 제한
                .collect(Collectors.toList());
    }

    /**
     * 파일 업로드
     */
    public void uploadFile(InputStream inputStream, long contentLength, String path, String contentType) throws IOException {
        String key = path; // 이미 파일명을 포함한 전체 경로를 전달받음
        // log
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build(),
                RequestBody.fromInputStream(inputStream, contentLength)
        );

        log.info("[S3업로드] 경로: " + "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key);
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String fileName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    public Resource getFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

        // InputStream을 Resource로 변환하여 반환
        return new InputStreamResource(s3Object);
    }
}