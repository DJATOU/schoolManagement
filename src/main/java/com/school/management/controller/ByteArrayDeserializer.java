package com.school.management.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class ByteArrayDeserializer extends JsonDeserializer<byte[]> {
    @Override
    public byte[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String value = jp.getValueAsString();

        // Vérifier si value est null ou vide
        if (value == null || value.isEmpty()) {
            // Retourner un tableau vide ou null, selon ce qui est attendu par votre logique métier
            return new byte[0]; // Ou retourner null
        }

        // Enlever les crochets au début et à la fin si présents
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }

        // Découper la chaîne sur les virgules
        String[] parts = value.split(",");
        byte[] result = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Byte.parseByte(parts[i].trim()); // Utiliser trim() pour enlever les espaces blancs éventuels
            } catch (NumberFormatException e) {
                // Gérer l'exception si le parsing échoue, par exemple en loguant une erreur
                // et en retournant un tableau partiellement rempli ou en lançant une exception
                throw new IOException("Erreur lors de la conversion de la chaîne en tableau de bytes", e);
            }
        }
        return result;
    }
}
