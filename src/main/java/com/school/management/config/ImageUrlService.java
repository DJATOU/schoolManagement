package com.school.management.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlService {

    @Value("${images.path}")
    private String imagesPath;  // Le répertoire des images (ex. "personne")

    // Méthode pour générer l'URL HTTP complète de l'image pour le frontend
    public String getPhotoUrl(String photoName, HttpServletRequest request) {
        if (photoName == null || photoName.isBlank()) {
            return null;  // Retourne null si le nom est vide
        }

        // Récupère l'URL du serveur (par exemple http://localhost:8080)
        String serverUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

        // Crée l'URL complète de l'image via HTTP (http://localhost:8080/personne/photoName.png)
        return serverUrl + "/" + imagesPath + "/" + photoName;
    }
}
