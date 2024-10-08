package com.buildazan.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService{

    
    public String saveImage(String directory, MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        try {
            if (!Files.exists(Path.of(directory))) {
                Files.createDirectory(Path.of(directory));
            }
            Files.copy(file.getInputStream(), Path.of(directory).resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }
    
}
