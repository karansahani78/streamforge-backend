package com.streamforge.service;

import com.streamforge.dto.VideoResponse;
import com.streamforge.entity.Video;
import com.streamforge.exception.VideoNotFoundException;
import com.streamforge.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

    private static final Path UPLOAD_PATH =
            Paths.get("uploads");

    public VideoResponse uploadVideo(
            String title,
            MultipartFile file
    ) throws IOException {

        // Create uploads directory if not exists
        if (!Files.exists(UPLOAD_PATH)) {
            Files.createDirectories(UPLOAD_PATH);
        }

        // Generate unique filename
        String fileName =
                UUID.randomUUID()
                        + "_"
                        + file.getOriginalFilename();

        // Final file path
        Path filePath = UPLOAD_PATH.resolve(fileName);

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
                .build();

        Video savedVideo = videoRepository.save(video);

        // Return response
        return VideoResponse.builder()
                .id(savedVideo.getId())
                .title(savedVideo.getTitle())
                .fileName(savedVideo.getFileName())
                .fileSize(savedVideo.getFileSize())
                .contentType(savedVideo.getContentType())
                .uploadedAt(savedVideo.getUploadedAt())
                .build();
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }
    // stream video
    public ResourceRegion streamVideo(Long videoId,String rangeHeader) throws IOException{
        Video video = videoRepository.findById(videoId).orElseThrow(()-> new VideoNotFoundException("Video not found"));

        Path path = Paths.get(video.getFilePath());
        UrlResource resource = new UrlResource(path.toUri());

        long contentLength = resource.contentLength();
        long chunkSize = 1024 * 1024;

        long start =0;
        long end = chunkSize -1;
        if(rangeHeader !=null){
            String [] ranges = rangeHeader.replace("bytes=", "")
                    .split("-");
            start = Long.parseLong(ranges[0]);
            if(ranges.length > 1 && ranges[1].isEmpty()){
                end = Long.parseLong(ranges[1]);
            }else {
                end = Math.min(start + chunkSize -1, contentLength -1);
            }
        }
        end = Math.min(end,contentLength -1);
        long regionLength = end -start + 1;

        return  new ResourceRegion(
                resource,
                start,
                regionLength
        );



    }
}