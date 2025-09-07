package org.marensovich.bot.bot.Yandex.Storage;

import io.awspring.cloud.s3.S3Template;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class YandexStorageService {
    private final S3Client s3Client;

    public YandexStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadVoiceMessage(String bucket, String objectKey, InputStream inputStream) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType("audio/ogg")
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));
            log.info("File uploaded successfully to {}/{}", bucket, objectKey);
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("Upload failed", e);
        }
    }

    public void deleteVoiceMessage(String bucket, String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getVoiceMessage(String bucket, String objectKey) {
        try {   
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path targetDir = projectRoot.resolve("Bot/src/main/resources/voicedata");
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(Paths.get(objectKey).getFileName());

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            s3Client.getObject(request, targetFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download voice message to " + objectKey, e);
        }
    }

    @PostConstruct
    public void testConnection() {
        try {
            log.info(s3Client.listBuckets().buckets().get(0).name());
            log.info("Successfully connected to Yandex Object Storage");
        } catch (Exception e) {
            log.error("Connection test failed", e);
        }
    }
}

