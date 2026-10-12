package com.sheyinglai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumReq(
        @NotBlank @Size(max = 64) String title,
        @Size(max = 500) String description,
        @Size(max = 32) String style,
        @Size(max = 100) String location,
        Boolean visible,
        Integer sort) {
}
