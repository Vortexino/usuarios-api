package com.uni.api.infrastructure.adapter.out.storage;

import com.uni.api.application.port.out.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Component
@Profile("lambda")
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3;
    private final String bucketName;
    private final String region;

    public S3FileStorageAdapter(S3Client s3,
                                @Value("${app.s3.bucket-name}") String bucketName,
                                @Value("${app.s3.region}") String region) {
        this.s3 = s3;
        this.bucketName = bucketName;
        this.region = region;
    }

    @Override
    public String guardar(String nombreOriginal, byte[] contenido) {
        String extension = "";
        int punto = nombreOriginal.lastIndexOf('.');
        if (punto >= 0) extension = nombreOriginal.substring(punto);

        String key = "uploads/" + UUID.randomUUID() + extension;

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                RequestBody.fromBytes(contenido)
        );

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
}