package com.cc.cse546.project_2.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
@Service
public class EC2Service {

    private final Ec2Client ec2Client;

    private static final int MAX_INSTANCES = 20;
    private List<String> runningInstances = new ArrayList<>();
    private static final String pythonFilePath = "face_recognition_project/manage.py";

    public EC2Service() {
        this.ec2Client = Ec2Client.create();
    }

    public String createInstance(String amiId) {// User Data script to install Python and run the Python script\

        String userDataScript = "#!/bin/bash\n" +
                "sudo apt-get update -y\n" +
                "/home/ubuntu/myenv/bin/python /home/ubuntu/face_recognition_project/manage.py listen_queue"; // Run the Python script

        // Encode user data to base64
        String userDataBase64 = Base64.getEncoder().encodeToString(userDataScript.getBytes());

        RunInstancesRequest runInstancesRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(InstanceType.T2_MICRO)
                .minCount(1)
                .maxCount(1)
                .userData(userDataBase64)  // Inject user data script
                .iamInstanceProfile(IamInstanceProfileSpecification.builder()
                        .name("S3BucketAccess")  // Make sure the role has permissions (if needed)
                        .build())
                .tagSpecifications(TagSpecification.builder()
                        .resourceType(ResourceType.INSTANCE)
                        .tags(Tag.builder()
                                .key("Name")
                                .value("app-tier-instance")  // Set the instance name here
                                .build(),
                                Tag.builder()
                                        .key("App-Tier")
                                        .value("true")  // Set the App-Tier tag here
                                        .build())
                        .build())
                .keyName("project-2-part-1")
                .build();

        RunInstancesResponse response = ec2Client.runInstances(runInstancesRequest);
        runningInstances.add(response.instances().get(0).instanceId());
        return response.instances().get(0).instanceId();}

    public void terminateInstance() {
        if (!runningInstances.isEmpty()) {
            String instanceId = runningInstances.remove(runningInstances.size() - 1);
            TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            TerminateInstancesResponse response = ec2Client.terminateInstances(terminateRequest);
            System.out.println("Terminated instance with ID: " + instanceId);
        } else {
            System.out.println("No instances to terminate");
        }
    }

    public int getRunningInstanceCount() {

        String namePrefix = "app-tier-instance";
        Filter runningFilter = Filter.builder()
            .name("instance-state-name")
            .values(InstanceStateName.RUNNING.toString())
            .build();

        // Create a filter for instances with a 'Name' tag that starts with the given prefix
        Filter nameFilter = Filter.builder()
                .name("tag:Name")
                .values(namePrefix + "*")  // Wildcard for prefix matching
                .build();

        // Describe instances request with the above filters
        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .filters(runningFilter, nameFilter)
                .build();

        // Get the response from AWS
        DescribeInstancesResponse response = ec2Client.describeInstances(request);
       // System.out.print(response.reservations());
        // Count the running instances with the matching name
        int runningInstanceCount = 0;
        for (Reservation reservation : response.reservations()) {
            for (Instance instance : reservation.instances()) {
                if (instance.state().nameAsString().equals("running")) {
                    runningInstanceCount++;
                }
            }
        }

        return runningInstanceCount;
    }
}

