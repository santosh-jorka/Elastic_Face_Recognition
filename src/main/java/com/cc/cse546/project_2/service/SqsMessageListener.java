package com.cc.cse546.project_2.service;

import java.util.List;
import java.util.Optional;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cc.cse546.project_2.entities.ResponseEntity;
import com.cc.cse546.project_2.repository.ResponseRepository;

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.LocalDateTime;

@Component
public class SqsMessageListener {
    @Autowired
    public ResponseRepository responseRepository;

    @SqsListener(value = "1225462862-resp-queue" )// Optional: Send a response to another queue
    public void listenToQueue(String message) {


            System.out.println("Received message from SQS: " + message);
        String[] parts = message.split(":");

        // Store the values in fileName and result variables
        String fileName = parts[0]; // First part before the colon
        String result = parts[1];    // Second part after the colon


        Optional<ResponseEntity> existingResponse = responseRepository.findByFileName(fileName);

        if (existingResponse.isPresent()) {
            // Update the existing record
            ResponseEntity response = existingResponse.get();
            response.setResponseData(message);
             responseRepository.save(response); // Save the updated entity
        } else {
            // Create a new record
            ResponseEntity responseEntity = new ResponseEntity();
            responseEntity.setFileName(fileName);
            responseEntity.setResponseData(message);
            responseEntity.setCreatedAt(LocalDateTime.now());

            responseRepository.save(responseEntity);
        }



    }
}

