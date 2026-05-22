package com.streamforge.service;

import com.streamforge.dto.VideoProcessingMessage;
import com.streamforge.dto.VideoResponse;
import com.streamforge.entity.Video;
import com.streamforge.exception.VideoNotFoundException;
import com.streamforge.repository.VideoRepository;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;

    private final CacheManager cacheManager;

    private final ObjectStorageService objectStorageService;

    private final VideoEventProducer videoEventProducer;

    private static final String TEMP_STREAM_DIR =
            "temp-stream/";

    @CacheEvict(value = "videos", allEntries = true)
    public VideoResponse uploadVideo(
            String title,
            MultipartFile file
    ) throws ServerException,
            InsufficientDataException,
            ErrorResponseException,
            IOException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidResponseException,
            XmlParserException,
            InternalException {

        /*
         * Upload original file to MinIO
         */
        String objectKey =
                objectStorageService.uploadFile(file);

        /*
         * Save metadata in PostgreSQL
         */
        Video video = Video.builder()
                .title(title)
                .fileName(file.getOriginalFilename())
                .objectKey(objectKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .processed(false)
                .processedPath(null)
                .thumbnailPath(null)
                .build();

        Video savedVideo =
                videoRepository.save(video);

        /*
         * Clear Redis cache
         */
        if (cacheManager.getCache("videos") != null) {

            cacheManager
                    .getCache("videos")
                    .clear();
        }

        /*
         * Publish RabbitMQ event
         */
        videoEventProducer.sendVideoProcessingEvent(
                VideoProcessingMessage.builder()
                        .videoId(savedVideo.getId())
                        .fileName(savedVideo.getFileName())
                        .objectKey(savedVideo.getObjectKey())
                        .build()
        );

        log.info(
                "Video uploaded successfully with ID: {}",
                savedVideo.getId()
        );

        /*
         * Generate secure MinIO URL
         */
        String videoUrl =
                objectStorageService.getVideoUrl(
                        savedVideo.getObjectKey()
                );

        /*
         * Return API response
         */
        return VideoResponse.builder()
                .id(savedVideo.getId())
                .title(savedVideo.getTitle())
                .fileName(savedVideo.getFileName())
                .fileSize(savedVideo.getFileSize())
                .contentType(savedVideo.getContentType())
                .uploadedAt(savedVideo.getUploadedAt())
                .processed(savedVideo.getProcessed())
                .processedPath(savedVideo.getProcessedPath())
                .thumbnailPath(savedVideo.getThumbnailPath())
                .videoUrl(videoUrl)
                .build();
    }

    @Cacheable("videos")
    public List<Video> getAllVideos() {

        log.info(
                "Fetching videos from database"
        );

        return videoRepository.findAll();
    }

    public Video getVideoById(Long videoId) {

        return videoRepository.findById(videoId)
                .orElseThrow(() ->
                        new VideoNotFoundException(
                                "Video not found with id: "
                                        + videoId
                        )
                );
    }

    /*
     * Video Streaming
     */
    public ResourceRegion streamVideo(
            Long videoId,
            String rangeHeader
    ) throws Exception {

        Video video =
                videoRepository.findById(videoId)
                        .orElseThrow(() ->
                                new VideoNotFoundException(
                                        "Video not found"
                                )
                        );

        /*
         * Create temp streaming directory
         */
        Files.createDirectories(
                Paths.get(TEMP_STREAM_DIR)
        );

        /*
         * Temp local file path
         */
        String tempFilePath =
                TEMP_STREAM_DIR
                        + video.getFileName();

        /*
         * Download file from MinIO
         */
        objectStorageService.downloadFile(
                video.getObjectKey(),
                tempFilePath
        );

        Path path =
                Paths.get(tempFilePath);

        UrlResource resource =
                new UrlResource(path.toUri());

        long contentLength =
                resource.contentLength();

        long chunkSize =
                1024 * 1024;

        long start = 0;
        long end = chunkSize - 1;

        if (rangeHeader != null) {

            String[] ranges =
                    rangeHeader
                            .replace("bytes=", "")
                            .split("-");

            start =
                    Long.parseLong(ranges[0]);

            if (
                    ranges.length > 1
                            && !ranges[1].isEmpty()
            ) {

                end =
                        Long.parseLong(ranges[1]);

            } else {

                end =
                        Math.min(
                                start + chunkSize - 1,
                                contentLength - 1
                        );
            }
        }

        end =
                Math.min(
                        end,
                        contentLength - 1
                );

        long regionLength =
                end - start + 1;

        return new ResourceRegion(
                resource,
                start,
                regionLength
        );
    }
}