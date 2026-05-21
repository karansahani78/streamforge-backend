package com.streamforge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class VideoResponse {
    private Long id;

    private String title;

    private String fileName;

    private Long fileSize;

    private String contentType;

    private LocalDateTime uploadedAt;
}
