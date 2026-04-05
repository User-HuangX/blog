package com.example.my_blog.controller;

import com.example.my_blog.dto.request.CreatePostRequest;
import com.example.my_blog.dto.response.PostDetailResponse;
import com.example.my_blog.dto.response.PostListItemResponse;
import com.example.my_blog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<PostListItemResponse> list() {
        return postService.listAll();
    }

    @GetMapping("/{id}")
    public PostDetailResponse getById(@PathVariable Long id) {
        return postService.getById(id);
    }

    @PostMapping("/author/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyAuthor(@RequestHeader(name = "X-Author-Password", required = false) String providedPassword) {
        postService.verifyAuthorPassword(providedPassword);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(
            @RequestBody @Valid CreatePostRequest request,
            @RequestHeader(name = "X-Author-Password", required = false) String providedPassword
    ) {
        return postService.create(request, providedPassword);
    }
}
