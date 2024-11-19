package com.cc.cse546.project_2.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName = "1225462862-in-bucket";  // Replace with your S3 bucket name

    public S3Service() {
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)  // Specify the correct AWS region
                .credentialsProvider(InstanceProfileCredentialsProvider.create())  // Adjust credentials as needed
                .build();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        // Save the file to a temp directory first
        Path tempFile = Files.createTempFile(fileName, "");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        try {
            // Upload the file to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.putObject(putObjectRequest, tempFile);

            // Generate the file URL after upload
            return getFileUrl(fileName);

        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        } finally {
            Files.delete(tempFile);  // Clean up temp file after upload
        }
    }

    private String getFileUrl(String fileName) {
        // Generate the file URL in the S3 bucket
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
    }
}
