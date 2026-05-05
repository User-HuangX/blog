package com.example.my_blog.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@TableName("post_content")
public class PostContent {

    @TableId
    private Long postId;

    private String summary;

    private String content;
}
