package com.example.my_blog.service;

import com.example.my_blog.domain.PostImage;
import com.example.my_blog.exception.NotFoundException;
import com.example.my_blog.mapper.PostImageMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private final PostImageMapper postImageMapper;

    public FileStorageService(PostImageMapper postImageMapper) {
        this.postImageMapper = postImageMapper;
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required.");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not an image.");
        }

        PostImage postImage = new PostImage();
        postImage.setFileName(resolveFileName(file.getOriginalFilename(), extension));
        postImage.setContentType(contentType);
        postImage.setSizeBytes(file.getSize());
        postImage.setCreatedAt(LocalDateTime.now());
        try {
            postImage.setData(file.getBytes());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image.");
        }
        postImageMapper.insert(postImage);

        return "/api/uploads/images/" + postImage.getId();
    }

    public StoredImage loadImage(Long id) {
        PostImage postImage = postImageMapper.selectById(id);
        if (postImage == null) {
            throw new NotFoundException("Image not found: " + id);
        }
        return new StoredImage(postImage.getData(), postImage.getContentType());
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveFileName(String originalFilename, String extension) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image." + extension;
        }
        return originalFilename;
    }

    public record StoredImage(byte[] data, String contentType) {
    }
}
