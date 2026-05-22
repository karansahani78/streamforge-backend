package com.streamforge.service;

import com.streamforge.dto.VideoProcessingMessage;
import com.streamforge.dto.VideoResponse;
import com.streamforge.entity.Video;
import com.streamforge.exception.VideoNotFoundException;
import com.streamforge.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.CacheManager;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final CacheManager cacheManager;
    private final VideoEventProducer videoEventProducer;

    private static final Path UPLOAD_PATH =
            Paths.get("uploads");

    @CacheEvict(value = "videos", allEntries = true)
    public VideoResponse uploadVideo(
            String title,
            MultipartFile file
    ) throws IOException {

        // Create uploads directory if not exists
        if (!Files.exists(UPLOAD_PATH)) {

            Files.createDirectories(
                    UPLOAD_PATH
            );
        }

        // Generate unique filename
        String fileName =
                UUID.randomUUID()
                        + "_"
                        + file.getOriginalFilename();

        // Final file path
        Path filePath =
                UPLOAD_PATH.resolve(fileName);

        // Copy file to uploads directory
        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        // Save metadata in DB
        Video video = Video.builder()
                .title(title)
                .fileName(fileName)
                .filePath(filePath.toString())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .processed(false)
                .processedPath(null)
                .thumbnailPath(null)
                .build();

        Video savedVideo =
                videoRepository.save(video);
        cacheManager.getCache("videos").clear();


        // Publish RabbitMQ event
        videoEventProducer.sendVideoProcessingEvent(
                VideoProcessingMessage.builder()
                        .videoId(savedVideo.getId())
                        .fileName(savedVideo.getFileName())
                        .filePath(savedVideo.getFilePath())
                        .build()
        );

        // Return response
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
                .build();
    }

    @Cacheable("videos")
    public List<Video> getAllVideos() {

        return videoRepository.findAll();
    }

    // Stream video
    public ResourceRegion streamVideo(
            Long videoId,
            String rangeHeader
    ) throws IOException {

        Video video =
                videoRepository.findById(videoId)
                        .orElseThrow(() ->
                                new VideoNotFoundException(
                                        "Video not found"
                                )
                        );

        Path path =
                Paths.get(video.getFilePath());

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