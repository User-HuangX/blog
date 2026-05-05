package com.example.my_blog.controller;

import com.example.my_blog.dto.request.AuthorVerifyRequest;
import com.example.my_blog.dto.request.CreatePostRequest;
import com.example.my_blog.dto.request.SaveDraftRequest;
import com.example.my_blog.dto.request.UpdatePostRequest;
import com.example.my_blog.dto.response.PostDetailResponse;
import com.example.my_blog.dto.response.PostListItemResponse;
import com.example.my_blog.service.AuthorSessionService;
import com.example.my_blog.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final AuthorSessionService authorSessionService;

    public PostController(PostService postService, AuthorSessionService authorSessionService) {
        this.postService = postService;
        this.authorSessionService = authorSessionService;
    }

    private static String resolveAuthorPassword(AuthorVerifyRequest body, String headerPassword) {
        if (body != null && body.password() != null && !body.password().isBlank()) {
            return body.password().trim();
        }
        if (headerPassword != null && !headerPassword.isBlank()) {
            return headerPassword.trim();
        }
        return null;
    }

    @GetMapping
    public List<PostListItemResponse> list() {
        return postService.listAll();
    }

    @GetMapping("/{id}")
    public PostDetailResponse getById(@PathVariable Long id) {
        return postService.getById(id);
    }

    @GetMapping("/author/{id}")
    public PostDetailResponse getByIdForAuthor(@PathVariable Long id, HttpServletRequest request) {
        authorSessionService.requireAuthor(request);
        return postService.getByIdForAuthor(id);
    }

    @PutMapping("/{id}")
    public PostDetailResponse update(@PathVariable Long id, @RequestBody @Valid UpdatePostRequest body, HttpServletRequest request) {
        authorSessionService.requireAuthor(request);
        return postService.updatePost(id, body);
    }

    @PostMapping("/author/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyAuthor(@RequestBody(required = false) AuthorVerifyRequest body, @RequestHeader(name = "X-Author-Password", required = false) String headerPassword, HttpServletRequest request) {
        String providedPassword = resolveAuthorPassword(body, headerPassword);
        authorSessionService.verifyAndLogin(providedPassword, request);
    }

    @GetMapping("/author/status")
    public Map<String, Boolean> authorStatus(HttpServletRequest request) {
        return Map.of("authenticated", authorSessionService.isAuthenticated(request));
    }

    @PostMapping("/author/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAuthor(HttpServletRequest request) {
        authorSessionService.logout(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(@RequestBody @Valid CreatePostRequest request, HttpServletRequest httpRequest) {
        authorSessionService.requireAuthor(httpRequest);
        return postService.create(request);
    }

    @PostMapping("/drafts")
    public PostDetailResponse saveDraft(@RequestBody @Valid SaveDraftRequest request, HttpServletRequest httpRequest) {
        authorSessionService.requireAuthor(httpRequest);
        return postService.saveDraft(request);
    }

    @PostMapping("/{id}/publish")
    public PostDetailResponse publishDraft(@PathVariable Long id, HttpServletRequest request) {
        authorSessionService.requireAuthor(request);
        return postService.publishDraft(id);
    }

    @GetMapping("/drafts/latest")
    public PostDetailResponse getLatestDraft(HttpServletRequest request) {
        authorSessionService.requireAuthor(request);
        return postService.getLatestDraft();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id, HttpServletRequest request) {
        authorSessionService.requireAuthor(request);
        postService.deleteById(id);
    }
}
