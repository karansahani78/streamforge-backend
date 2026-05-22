package com.streamforge.service;

import com.streamforge.dto.VideoProcessingMessage;
import com.streamforge.entity.Video;
import com.streamforge.exception.VideoNotFoundException;
import com.streamforge.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoEventConsumer {

    private final VideoRepository videoRepository;

    private final ObjectStorageService objectStorageService;

    private static final String TEMP_DIR =
            "temp/";

    private static final String PROCESSED_DIR =
            "processed/";

    @RabbitListener(
            queues = "video.processing.queue"
    )
    public void processVideo(
            VideoProcessingMessage message
    ) {

        log.info(
                "Started processing video: {}",
                message.getVideoId()
        );

        try {

            /*
             * Fetch video from DB
             */
            Video video =
                    videoRepository.findById(
                                    message.getVideoId()
                            )
                            .orElseThrow(() ->
                                    new VideoNotFoundException(
                                            "Video not found"
                                    )
                            );

            /*
             * Create temp and processed dirs
             */
            Files.createDirectories(
                    Paths.get(TEMP_DIR)
            );

            Files.createDirectories(
                    Paths.get(PROCESSED_DIR)
            );

            /*
             * Download original video from MinIO
             */
            String tempInputPath =
                    TEMP_DIR
                            + message.getFileName();

            objectStorageService.downloadFile(
                    message.getObjectKey(),
                    tempInputPath
            );

            /*
             * Processed output paths
             */
            String outputPath =
                    PROCESSED_DIR
                            + message.getVideoId()
                            + "_720p.mp4";

            String thumbnailPath =
                    PROCESSED_DIR
                            + message.getVideoId()
                            + "_thumbnail.jpg";

            /*
             * Video Transcoding
             */
            ProcessBuilder transcodingProcessBuilder =
                    new ProcessBuilder(
                            "ffmpeg",
                            "-i",
                            tempInputPath,
                            "-vf",
                            "scale=-1:720",
                            outputPath
                    );

            transcodingProcessBuilder
                    .redirectErrorStream(true);

            Process transcodingProcess =
                    transcodingProcessBuilder.start();

            int transcodingExitCode =
                    transcodingProcess.waitFor();

            if (transcodingExitCode != 0) {

                log.error(
                        "Video transcoding failed for video: {}",
                        message.getVideoId()
                );

                return;
            }

            log.info(
                    "720p transcoding completed for video: {}",
                    message.getVideoId()
            );

            /*
             * Thumbnail Generation
             */
            ProcessBuilder thumbnailProcessBuilder =
                    new ProcessBuilder(
                            "ffmpeg",
                            "-i",
                            tempInputPath,
                            "-ss",
                            "00:00:05",
                            "-vframes",
                            "1",
                            thumbnailPath
                    );

            thumbnailProcessBuilder
                    .redirectErrorStream(true);

            Process thumbnailProcess =
                    thumbnailProcessBuilder.start();

            int thumbnailExitCode =
                    thumbnailProcess.waitFor();

            if (thumbnailExitCode != 0) {

                log.error(
                        "Thumbnail generation failed for video: {}",
                        message.getVideoId()
                );

                return;
            }

            log.info(
                    "Thumbnail generated for video: {}",
                    message.getVideoId()
            );

            /*
             * Upload processed video back to MinIO
             */
            String processedObjectKey =
                    objectStorageService
                            .uploadProcessedFile(
                                    outputPath,
                                    "processed/"
                                            + message.getVideoId()
                                            + "_720p.mp4"
                            );

            /*
             * Upload thumbnail back to MinIO
             */
            String thumbnailObjectKey =
                    objectStorageService
                            .uploadProcessedFile(
                                    thumbnailPath,
                                    "thumbnails/"
                                            + message.getVideoId()
                                            + "_thumbnail.jpg"
                            );

            /*
             * Update DB
             */
            video.setProcessed(true);

            video.setProcessedPath(
                    processedObjectKey
            );

            video.setThumbnailPath(
                    thumbnailObjectKey
            );

            videoRepository.save(video);

            /*
             * Cleanup temp files
             */
            Files.deleteIfExists(
                    Paths.get(tempInputPath)
            );

            Files.deleteIfExists(
                    Paths.get(outputPath)
            );

            Files.deleteIfExists(
                    Paths.get(thumbnailPath)
            );

            log.info(
                    "Video processing completed successfully: {}",
                    message.getVideoId()
            );

        } catch (Exception e) {

            log.error(
                    "Error processing video: {}",
                    message.getVideoId(),
                    e
            );
        }
    }
}