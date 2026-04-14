package com.example.my_blog.dto.response;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        Boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
