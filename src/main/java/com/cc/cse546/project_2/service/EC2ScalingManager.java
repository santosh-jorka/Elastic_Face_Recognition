package com.cc.cse546.project_2.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
public class EC2ScalingManager {

    private final EC2Service ec2Service;
    private final SqsService sqsService;
    private final String queueUrl = "https://sqs.us-east-1.amazonaws.com/905418405896/1225462862-req-queue";
    private final String amiId = "ami-0a6d0641e633d96bc";
    private final int maxInstances = 20;  // Max number of EC2 instances allowed

    public EC2ScalingManager(EC2Service ec2Service, SqsService sqsService) {
        this.ec2Service = ec2Service;
        this.sqsService = sqsService;
    }

    // Scheduled task to check queue and manage scaling every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void monitorAndScale() {
        // Long polling for messages (waits for up to 10 seconds)
        /*ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(10)  // Long polling
                .build();

        ReceiveMessageResponse response = sqsService.sqsClient().receiveMessage(receiveMessageRequest);
       */ int messageCount = sqsService.getApproximateNumberOfMessages(queueUrl);

        System.out.println("Current number of messages in the queue: " + messageCount);

        // Scale logic based on the number of messages
        scaleInstancesBasedOnQueue(messageCount);
    }

    private void scaleInstancesBasedOnQueue(int messageCount) {
        int requiredInstances = calculateRequiredInstances(messageCount);
        int runningInstances = ec2Service.getRunningInstanceCount();

        System.out.println("Required instances: " + requiredInstances + ", Running instances: " + runningInstances);

        // Scale up if required instances are greater than running instances
        if (requiredInstances > runningInstances) {
            while (runningInstances < requiredInstances && ec2Service.getRunningInstanceCount() < maxInstances) {
                ec2Service.createInstance(amiId);
                runningInstances++;
                System.out.println("Creating new instance, running instances: " + runningInstances);
            }
        }
        // Scale down if running instances are more than required
        else if (requiredInstances < runningInstances) {
            while (runningInstances > requiredInstances && runningInstances > 0) {
                ec2Service.terminateInstance();
                runningInstances--;
                System.out.println("Terminating instance, running instances: " + runningInstances);
            }
        }
    }

    // Calculate the required number of instances based on the message count
    private int calculateRequiredInstances(int messageCount) {
        if (messageCount == 0) {
            return 0;  // No instances needed if there are no messages
        }
        // For 1-3 messages, return 1 instance. For each additional 3 messages, add 1 more instance
        return Math.min((int) Math.ceil(messageCount / 3.0), maxInstances);
    }
}
