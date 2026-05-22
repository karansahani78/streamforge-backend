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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoEventConsumer {

    private final VideoRepository videoRepository;

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

            // Fetch video from DB
            Video video =
                    videoRepository.findById(
                                    message.getVideoId()
                            )
                            .orElseThrow(() ->
                                    new VideoNotFoundException(
                                            "Video not found"
                                    )
                            );

            // Create processed directory
            Path processedPath =
                    Paths.get(PROCESSED_DIR);

            if (!Files.exists(processedPath)) {

                Files.createDirectories(
                        processedPath
                );
            }

            // Original uploaded file
            String inputPath =
                    message.getFilePath();

            // Processed output file
            String outputPath =
                    PROCESSED_DIR
                            + message.getVideoId()
                            + "_720p.mp4";

            // Thumbnail output
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
                            inputPath,
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
                            inputPath,
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
             * Update DB
             */

            video.setProcessed(true);

            video.setProcessedPath(
                    outputPath
            );

            video.setThumbnailPath(
                    thumbnailPath
            );

            videoRepository.save(video);

            log.info(
                    "Video processing completed successfully: {}",
                    message.getVideoId()
            );

        } catch (
                IOException
                | InterruptedException e
        ) {

            log.error(
                    "Error processing video: {}",
                    message.getVideoId(),
                    e
            );
        }
    }
}