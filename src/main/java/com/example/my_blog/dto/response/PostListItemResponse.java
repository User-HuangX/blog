package com.example.my_blog.dto.response;

import com.example.my_blog.domain.PostMeta;

public record PostListItemResponse(
        Long id,
        String title
) {
    public static PostListItemResponse from(PostMeta postMeta) {
        return new PostListItemResponse(
                postMeta.getId(),
                postMeta.getTitle()
        );
    }
}
