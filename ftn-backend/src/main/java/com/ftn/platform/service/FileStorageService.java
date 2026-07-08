package com.ftn.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadPath) {
        this.uploadDir = Paths.get(uploadPath, "contracts").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String store(MultipartFile file, Long contractId) {
        try {
            String filename = "contract_" + contractId + "_" + System.currentTimeMillis() + ".pdf";
            Path targetPath = this.uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "contracts/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public byte[] download(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir.getParent().toString(), relativePath).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + relativePath, e);
        }
    }

    public void delete(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir.getParent().toString(), relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }

    public Path getUploadDir() {
        return uploadDir.getParent();
    }
}
