package com.streamforge.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /*
     * Upload original file
     */
    public String uploadFile(MultipartFile file)
            throws IOException,
            ServerException,
            InsufficientDataException,
            ErrorResponseException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidResponseException,
            XmlParserException,
            InternalException {

        String fileName =
                System.currentTimeMillis()
                        + "_"
                        + file.getOriginalFilename();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(
                                file.getInputStream(),
                                file.getSize(),
                                -1
                        )
                        .contentType(file.getContentType())
                        .build()
        );

        return fileName;
    }

    /*
     * Generate secure URL
     */
    public String getVideoUrl(String objectKey)
            throws IOException,
            ServerException,
            InsufficientDataException,
            ErrorResponseException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidResponseException,
            XmlParserException,
            InternalException {

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .method(Method.GET)
                        .build()
        );
    }

    /*
     * Download file locally for FFmpeg processing
     */
    public void downloadFile(
            String objectKey,
            String outputPath
    ) throws Exception {

        minioClient.downloadObject(
                DownloadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .filename(outputPath)
                        .build()
        );
    }

    /*
     * Upload processed/transcoded files
     */
    public String uploadProcessedFile(
            String filePath,
            String objectName
    ) throws Exception {

        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .filename(filePath)
                        .build()
        );

        return objectName;
    }
}