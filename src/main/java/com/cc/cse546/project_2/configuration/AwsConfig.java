package com.cc.cse546.project_2.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    /*@Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.US_EAST_1) // Set your desired AWS region
                .credentialsProvider(InstanceProfileCredentialsProvider.create()) // Use IAM role credentials
                .build();
    }*/

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.US_EAST_1) // Set your desired AWS region
                .credentialsProvider(DefaultCredentialsProvider.create()) // Use IAM role credentials
                .build();
    }
}
