package com.school.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String getPhotoUrl(String photoName) {
        if (photoName == null || photoName.isBlank()) {
            return null; // ou une URL par défaut
        }
        return "/personne/" + photoName;
    }
}
