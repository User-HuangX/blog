package com.example.my_blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.my_blog.domain.PostContent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostContentMapper extends BaseMapper<PostContent> {
}
