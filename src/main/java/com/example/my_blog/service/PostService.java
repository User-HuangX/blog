package com.example.my_blog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.my_blog.domain.PostContent;
import com.example.my_blog.domain.PostMeta;
import com.example.my_blog.dto.request.CreatePostRequest;
import com.example.my_blog.dto.response.PostDetailResponse;
import com.example.my_blog.dto.response.PostListItemResponse;
import com.example.my_blog.exception.NotFoundException;
import com.example.my_blog.mapper.PostContentMapper;
import com.example.my_blog.mapper.PostMetaMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private final PostMetaMapper postMetaMapper;
    private final PostContentMapper postContentMapper;
    private final String authorPassword;

    public PostService(
            PostMetaMapper postMetaMapper,
            PostContentMapper postContentMapper,
            @Value("${blog.author.password}") String authorPassword
    ) {
        this.postMetaMapper = postMetaMapper;
        this.postContentMapper = postContentMapper;
        this.authorPassword = authorPassword;
    }

    @Transactional(readOnly = true)
    public List<PostListItemResponse> listAll() {
        QueryWrapper<PostMeta> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_at");
        return postMetaMapper.selectList(queryWrapper).stream().map(PostListItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getById(Long id) {
        PostMeta postMeta = postMetaMapper.selectById(id);
        if (postMeta == null) {
            throw new NotFoundException("Post not found: " + id);
        }
        PostContent postContent = postContentMapper.selectById(id);
        if (postContent == null) {
            throw new NotFoundException("Post content not found: " + id);
        }

        return new PostDetailResponse(
                postMeta.getId(),
                postMeta.getTitle(),
                postContent.getContent(),
                postMeta.getCreatedAt(),
                postMeta.getUpdatedAt()
        );
    }

    @Transactional
    public PostDetailResponse create(CreatePostRequest request, String providedPassword) {
        verifyAuthorPassword(providedPassword);

        PostMeta postMeta = new PostMeta();
        postMeta.setTitle(request.title());
        postMeta.setAuthorName("博主");
        LocalDateTime now = LocalDateTime.now();
        postMeta.setCreatedAt(now);
        postMeta.setUpdatedAt(now);
        postMetaMapper.insert(postMeta);

        PostContent postContent = new PostContent();
        postContent.setPostId(postMeta.getId());
        postContent.setSummary(null);
        postContent.setContent(request.content());
        postContentMapper.insert(postContent);

        return new PostDetailResponse(
                postMeta.getId(),
                postMeta.getTitle(),
                postContent.getContent(),
                postMeta.getCreatedAt(),
                postMeta.getUpdatedAt()
        );
    }

    public void verifyAuthorPassword(String providedPassword) {
        if (providedPassword == null || !providedPassword.equals(authorPassword)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Author password is invalid.");
        }
    }
}
