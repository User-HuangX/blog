package com.example.my_blog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.my_blog.domain.PostContent;
import com.example.my_blog.domain.PostMeta;
import com.example.my_blog.dto.request.CreatePostRequest;
import com.example.my_blog.dto.request.SaveDraftRequest;
import com.example.my_blog.dto.request.UpdatePostRequest;
import com.example.my_blog.dto.response.PostDetailResponse;
import com.example.my_blog.dto.response.PostListItemResponse;
import com.example.my_blog.exception.NotFoundException;
import com.example.my_blog.mapper.PostContentMapper;
import com.example.my_blog.mapper.PostMetaMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

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
        this.authorPassword = authorPassword == null ? "" : authorPassword.trim();
    }

    @PostConstruct
    void logAuthorPasswordConfigured() {
        if (authorPassword.isEmpty()) {
            log.warn(
                    "Author password is not configured (empty). Set blog.author.password or BLOG_AUTHOR_PASSWORD; author login will fail."
            );
            return;
        }
        log.info(
                "Author password loaded from configuration (length={} chars, value not logged). If login fails, align client password or env BLOG_AUTHOR_PASSWORD.",
                authorPassword.length()
        );
    }

    @Transactional(readOnly = true)
    public List<PostListItemResponse> listAll() {
        QueryWrapper<PostMeta> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_published", true);
        queryWrapper.orderByDesc("created_at");
        return postMetaMapper.selectList(queryWrapper).stream().map(PostListItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getById(Long id) {
        PostMeta postMeta = postMetaMapper.selectById(id);
        if (postMeta == null) {
            throw new NotFoundException("Post not found: " + id);
        }
        if (!Boolean.TRUE.equals(postMeta.getIsPublished())) {
            throw new NotFoundException("Post not found: " + id);
        }
        PostContent postContent = postContentMapper.selectById(id);
        if (postContent == null) {
            throw new NotFoundException("Post content not found: " + id);
        }

        return toDetail(postMeta, postContent);
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getByIdForAuthor(Long id) {
        PostMeta postMeta = postMetaMapper.selectById(id);
        if (postMeta == null) {
            throw new NotFoundException("Post not found: " + id);
        }
        PostContent postContent = postContentMapper.selectById(id);
        if (postContent == null) {
            throw new NotFoundException("Post content not found: " + id);
        }
        return toDetail(postMeta, postContent);
    }

    @Transactional
    public PostDetailResponse updatePost(Long id, UpdatePostRequest request) {
        PostMeta postMeta = postMetaMapper.selectById(id);
        if (postMeta == null) {
            throw new NotFoundException("Post not found: " + id);
        }
        PostContent postContent = postContentMapper.selectById(id);
        if (postContent == null) {
            throw new NotFoundException("Post content not found: " + id);
        }

        postMeta.setTitle(request.title());
        postMeta.setUpdatedAt(LocalDateTime.now());
        postMetaMapper.updateById(postMeta);

        postContent.setContent(request.content());
        postContentMapper.updateById(postContent);

        return toDetail(postMeta, postContent);
    }

    @Transactional
    public PostDetailResponse create(CreatePostRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PostMeta postMeta = new PostMeta();
        postMeta.setTitle(request.title());
        postMeta.setAuthorName("博主");
        postMeta.setIsPublished(true);
        postMeta.setCreatedAt(now);
        postMeta.setUpdatedAt(now);
        postMetaMapper.insert(postMeta);

        PostContent postContent = new PostContent();
        postContent.setPostId(postMeta.getId());
        postContent.setSummary(null);
        postContent.setContent(request.content());
        postContentMapper.insert(postContent);

        return toDetail(postMeta, postContent);
    }

    @Transactional
    public PostDetailResponse saveDraft(SaveDraftRequest request) {
        if (request.id() == null) {
            return createDraft(request.title(), request.content());
        }
        return updateDraft(request.id(), request.title(), request.content());
    }

    @Transactional
    public PostDetailResponse publishDraft(Long id) {
        PostMeta postMeta = postMetaMapper.selectById(id);
        if (postMeta == null) {
            throw new NotFoundException("Draft not found: " + id);
        }
        if (Boolean.TRUE.equals(postMeta.getIsPublished())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该文章已发布。");
        }
        postMeta.setIsPublished(true);
        postMeta.setUpdatedAt(LocalDateTime.now());
        postMetaMapper.updateById(postMeta);

        PostContent postContent = postContentMapper.selectById(id);
        if (postContent == null) {
            throw new NotFoundException("Post content not found: " + id);
        }
        return toDetail(postMeta, postContent);
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getLatestDraft() {
        QueryWrapper<PostMeta> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_published", false);
        queryWrapper.orderByDesc("updated_at");
        queryWrapper.last("LIMIT 1");
        PostMeta postMeta = postMetaMapper.selectOne(queryWrapper);
        if (postMeta == null) {
            throw new NotFoundException("Draft not found.");
        }
        PostContent postContent = postContentMapper.selectById(postMeta.getId());
        if (postContent == null) {
            throw new NotFoundException("Draft content not found.");
        }
        return toDetail(postMeta, postContent);
    }

    @Transactional
    public void deleteById(Long id) {
        PostMeta postMeta = postMetaMapper.selectById(id);
        if (postMeta == null) {
            throw new NotFoundException("Post not found: " + id);
        }
        postMetaMapper.deleteById(id);
    }

    public boolean matchesAuthorPassword(String providedPassword) {
        if (providedPassword == null) {
            return false;
        }
        return authorPassword.equals(providedPassword.trim());
    }

    private PostDetailResponse createDraft(String title, String content) {
        LocalDateTime now = LocalDateTime.now();

        PostMeta postMeta = new PostMeta();
        postMeta.setTitle(title);
        postMeta.setAuthorName("博主");
        postMeta.setIsPublished(false);
        postMeta.setCreatedAt(now);
        postMeta.setUpdatedAt(now);
        postMetaMapper.insert(postMeta);

        PostContent postContent = new PostContent();
        postContent.setPostId(postMeta.getId());
        postContent.setSummary(null);
        postContent.setContent(content);
        postContentMapper.insert(postContent);

        return toDetail(postMeta, postContent);
    }

    private PostDetailResponse updateDraft(Long id, String title, String content) {
        PostMeta postMeta = postMetaMapper.selectById(id);
        if (postMeta == null) {
            throw new NotFoundException("Draft not found: " + id);
        }
        if (Boolean.TRUE.equals(postMeta.getIsPublished())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已发布文章不能作为草稿保存。");
        }

        postMeta.setTitle(title);
        postMeta.setUpdatedAt(LocalDateTime.now());
        postMetaMapper.updateById(postMeta);

        PostContent postContent = postContentMapper.selectById(id);
        if (postContent == null) {
            throw new NotFoundException("Draft content not found: " + id);
        }
        postContent.setContent(content);
        postContentMapper.updateById(postContent);

        return toDetail(postMeta, postContent);
    }

    private PostDetailResponse toDetail(PostMeta postMeta, PostContent postContent) {
        return new PostDetailResponse(
                postMeta.getId(),
                postMeta.getTitle(),
                postContent.getContent(),
                Boolean.TRUE.equals(postMeta.getIsPublished()),
                postMeta.getCreatedAt(),
                postMeta.getUpdatedAt()
        );
    }
}
