package com.example.my_blog.service;

import com.example.my_blog.dto.response.UploadImageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {

    private final FileStorageService fileStorageService;
    private final AuthorSessionService authorSessionService;

    public ImageService(FileStorageService fileStorageService, AuthorSessionService authorSessionService) {
        this.fileStorageService = fileStorageService;
        this.authorSessionService = authorSessionService;
    }

    public UploadImageResponse upload(MultipartFile image, HttpServletRequest request) {
        authorSessionService.requireAuthor(request);
        return new UploadImageResponse(fileStorageService.storeImage(image));
    }

    public ResponseEntity<byte[]> serve(Long id) {
        FileStorageService.StoredImage img = fileStorageService.loadImage(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(img.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
                .body(img.data());
    }
}