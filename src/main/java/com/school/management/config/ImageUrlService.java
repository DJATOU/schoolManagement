package com.school.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlService {

    @Value("${server.url}")
    private String serverUrl;

    @Value("${images.path}")
    private String imagesPath;

    public String getPhotoUrl(String photoName) {
        if (photoName == null || photoName.isBlank()) {
            return null; // ou une URL par défaut
        }
        String photoUrl = serverUrl + "/" + imagesPath + "/" + photoName;
        System.out.println("Generated Photo URL: " + photoUrl); // Log the generated URL
        return photoUrl;
    }
}
