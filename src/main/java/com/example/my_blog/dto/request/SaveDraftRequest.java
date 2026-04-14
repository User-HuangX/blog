package com.example.my_blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveDraftRequest(
        Long id,
        @NotBlank @Size(max = 200) String title,
        String content
) {
}
