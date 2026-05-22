package com.streamforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    private String fileName;

    private String filePath;

    private String contentType;

    private Long fileSize;

    private LocalDateTime uploadedAt;
    /*
     * Processing Status
     */

    @Column(nullable = false)
    private Boolean processed = false;

    /*
     * Processed 720p video path
     */

    private String processedPath;

    /*
     * Thumbnail image path
     */

    private String thumbnailPath;
}
