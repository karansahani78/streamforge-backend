package com.streamforge.controller;

import com.streamforge.dto.VideoResponse;
import com.streamforge.entity.Video;
import com.streamforge.service.ObjectStorageService;
import com.streamforge.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    private final ObjectStorageService objectStorageService;

    /*
     * Upload Video
     */
    @PostMapping("/upload")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("title") String title,

            @RequestParam("file")
            MultipartFile file
    ) throws Exception {

        VideoResponse response =
                videoService.uploadVideo(
                        title,
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get All Videos
     */
    @GetMapping
    public ResponseEntity<List<Video>> getAllVideos() {

        return ResponseEntity.ok(
                videoService.getAllVideos()
        );
    }

    /*
     * Stream Video
     */
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<ResourceRegion> streamVideo(

            @PathVariable Long videoId,

            @RequestHeader(
                    value = "Range",
                    required = false
            )
            String rangeHeader

    ) throws Exception {

        ResourceRegion region =
                videoService.streamVideo(
                        videoId,
                        rangeHeader
                );

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(
                        MediaType.valueOf("video/mp4")
                )
                .body(region);
    }

    /*
     * Get Secure MinIO URL
     */
    @GetMapping("/{videoId}/url")
    public ResponseEntity<String> getVideoUrl(
            @PathVariable Long videoId
    ) throws Exception {

        Video video =
                videoService.getVideoById(videoId);

        String url =
                objectStorageService.getVideoUrl(
                        video.getObjectKey()
                );

        return ResponseEntity.ok(url);
    }
}