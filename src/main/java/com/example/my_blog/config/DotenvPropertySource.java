package com.example.my_blog.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class DotenvPropertySource implements EnvironmentPostProcessor {

    private static final String DOTENV_NAME = ".env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path path = findDotenv();
        if (path == null) {
            return;
        }
        try {
            Map<String, Object> props = parseDotenv(path);
            PropertySource<?> source = new MapPropertySource("dotenv", props);
            environment.getPropertySources().addLast(source);
        } catch (IOException e) {
            // 解析失败不阻塞启动，系统属性 / IDE 配置仍可作为回退
        }
    }

    private Path findDotenv() {
        Path cwd = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 5; i++) {
            Path candidate = cwd.resolve(DOTENV_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            Path parent = cwd.getParent();
            if (parent == null || parent.equals(cwd)) break;
            cwd = parent;
        }
        return null;
    }

    private Map<String, Object> parseDotenv(Path path) throws IOException {
        Properties props = PropertiesLoaderUtils.loadProperties(new FileSystemResource(path));
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }
        return result;
    }
}