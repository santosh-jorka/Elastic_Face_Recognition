    package com.cc.cse546.project_2.service;

    import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cc.cse546.project_2.entities.ResponseEntity;
import com.cc.cse546.project_2.repository.ResponseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

    @Service
    public class SqsService {


        public final SqsClient sqsClient;
        @Autowired
        public ResponseRepository responseRepository;
        private final String requestQueueUrl = "1225462862-req-queue";


        @Autowired
        public SqsService(SqsClient sqsClient) {
            this.sqsClient = sqsClient;

        }


        public String waitForResponse(String fileName, long timeout, TimeUnit unit) {
            long endTime = System.currentTimeMillis() + unit.toMillis(timeout);

            while (System.currentTimeMillis() < endTime) {
                // Poll the file or database for the response associated with this requestId
                Optional<ResponseEntity> response = responseRepository.findByFileName(fileName); // Query the DB
                //System.out.println("waitForResponse");
                if (response.isPresent()) {
                    System.out.println(" Response Enter: " + response.get().getResponseData());
                    // Return the response once found
                    String result = response.get().getResponseData();
                    //deleteResponse(response.get());
                    responseRepository.delete(response.get());
                    return result;
                }

                // Sleep for a short while before polling again
                try {
                    TimeUnit.SECONDS.sleep(2); // Adjust this delay as needed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return null; // Return null if no response is found within the timeout period
        }

        

        public void deleteResponse(ResponseEntity response) {
        	System.out.println("Response Entity deleted: "+response.getFileName());
            responseRepository.delete(response);
        }
        public void sendMultipartFileToSqs(MultipartFile file) {
            try {
                // Get the byte array from MultipartFile
                byte[] fileBytes = file.getBytes();

                // Convert byte array to Base64 string
                String base64EncodedData = Base64.getEncoder().encodeToString(fileBytes);

                // Create a JSON object with the file name and Base64-encoded data
                Map<String, String> messagePayload = new HashMap<>();
                messagePayload.put("fileName", file.getOriginalFilename());  // Add the file name
                messagePayload.put("fileData", base64EncodedData);           // Add the file data

                // Convert the map to a JSON string
                ObjectMapper objectMapper = new ObjectMapper();
                String messageBody = objectMapper.writeValueAsString(messagePayload);

                // Create SQS message request
                SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                        .queueUrl(requestQueueUrl)
                        .messageBody(messageBody) // Send the JSON message
                        .build();

                // Send the message
                sqsClient.sendMessage(sendMsgRequest);
                System.out.println("Multipart file and file name sent to SQS successfully.");

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error sending multipart file to SQS: " + e.getMessage());
            }
        }


        public int getApproximateNumberOfMessages(String queueUrl) {
            GetQueueAttributesRequest attributesRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build();

            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(attributesRequest);
            return Integer.parseInt(response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));
        }
    }
