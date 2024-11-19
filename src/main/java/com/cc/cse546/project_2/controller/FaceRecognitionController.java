package com.cc.cse546.project_2.controller;


import com.cc.cse546.project_2.service.PredictionService;
import com.cc.cse546.project_2.service.SqsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
public class FaceRecognitionController {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private SqsService sqsService;

    @PostMapping("/")
    public ResponseEntity<String> handleFileUpload(@RequestParam("inputFile") MultipartFile file) throws IOException {
        // Get the original filename, remove the extension

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".jpg")) {
            return ResponseEntity.badRequest().body("Only .jpg files are accepted");
        }
        sqsService.sendMultipartFileToSqs(file);
        // Now, wait for the response asynchronously
        CompletableFuture<String> responseFuture = CompletableFuture.supplyAsync(() -> {
            return sqsService.waitForResponse(filename, 600, TimeUnit.SECONDS); // Polling logic
        });

        try {
            // Wait for the response or timeout after 30 seconds
            String response = responseFuture.get(600, TimeUnit.SECONDS);

            if (response != null) {
                // Write response to a local file or DB, or process the response as needed
                return ResponseEntity.ok( response);
            } else {
                // Timeout if no response was received
                return ResponseEntity.status(504).body("Timeout waiting for response");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing request");
        }
    }

}