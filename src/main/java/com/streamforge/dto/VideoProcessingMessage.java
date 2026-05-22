package com.streamforge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoProcessingMessage implements Serializable {
    private Long videoId;
    private String objectKey;
    private String fileName;
}
