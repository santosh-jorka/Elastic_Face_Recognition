package com.cc.cse546.project_2.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PredictionService {

    @Value("static/classification_face_images_1000.csv")
    private String resourcePath;

    private Map<String, String> lookupTable = new HashMap<>();

    @PostConstruct
    public void loadLookupTable() throws IOException {


        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                lookupTable.put(parts[0], parts[1]); // key: filename, value: prediction result
            }
        }
    }

    public String getPrediction(String filename) {
        return lookupTable.getOrDefault(filename, "Unknown");
    }
}
