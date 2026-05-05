package com.example.my_blog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@TableName("post_image")
public class PostImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String contentType;

    private Long sizeBytes;

    private byte[] data;

    private LocalDateTime createdAt;
}
