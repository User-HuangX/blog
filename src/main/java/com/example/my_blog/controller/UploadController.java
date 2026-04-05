package com.example.my_blog.controller;

import com.example.my_blog.dto.response.UploadImageResponse;
import com.example.my_blog.service.FileStorageService;
import com.example.my_blog.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;
    private final PostService postService;

    public UploadController(FileStorageService fileStorageService, PostService postService) {
        this.fileStorageService = fileStorageService;
        this.postService = postService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadImageResponse uploadImage(
            @RequestParam("image") MultipartFile image,
            @RequestHeader(name = "X-Author-Password", required = false) String providedPassword
    ) {
        postService.verifyAuthorPassword(providedPassword);
        String url = fileStorageService.storeImage(image);
        return new UploadImageResponse(url);
    }
}
