package com.example.demo.Utils;

import com.example.demo.Eception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Service
public class FileUtils {
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.max-size:10485760}")
    private long maxSize;

    /**
     * 保存文件，返回相对存储路径（相对于 uploadDir）
     */
    public String save(MultipartFile file) {
        validate(file);

        String dateDir  = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ext      = getExt(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir      = Paths.get(uploadDir, dateDir);
        Path filePath = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            file.transferTo(filePath.toFile());
            log.info("[FileUtil] saved: {}", filePath);
            return dateDir + "/" + filename;
        } catch (IOException e) {
            log.error("[FileUtil] save failed", e);
            throw new BusinessException(500, "文件保存失败，请稍后重试");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw BusinessException.badRequest("文件超过 10MB，请压缩后重新上传");
        }
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_TYPES.contains(mime)) {
            throw BusinessException.badRequest("不支持的文件类型，请上传图片、PDF 或 Office 文档");
        }
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
