package com.example.my_blog.controller;

import com.example.my_blog.dto.response.UploadImageResponse;
import com.example.my_blog.service.AuthorSessionService;
import com.example.my_blog.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;
    private final AuthorSessionService authorSessionService;

    public UploadController(FileStorageService fileStorageService, AuthorSessionService authorSessionService) {
        this.fileStorageService = fileStorageService;
        this.authorSessionService = authorSessionService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadImageResponse uploadImage(
            @RequestParam("image") MultipartFile image,
            HttpServletRequest request
    ) {
        authorSessionService.requireAuthor(request);
        String url = fileStorageService.storeImage(image);
        return new UploadImageResponse(url);
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        FileStorageService.StoredImage storedImage = fileStorageService.loadImage(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(storedImage.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
                .body(storedImage.data());
    }
}
