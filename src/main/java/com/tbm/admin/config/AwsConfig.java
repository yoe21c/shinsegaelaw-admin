package com.tbm.admin.config;

import com.tbm.admin.model.props.AwsCredentialsProps;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Configuration
@RequiredArgsConstructor
public class AwsConfig {

    private final AwsCredentialsProps awsCredentialsProps;

    private AwsCredentialsProvider awsCredentialsProvider() {
        return () -> AwsBasicCredentials.create(awsCredentialsProps.getAccessKeyId(), awsCredentialsProps.getSecretKey());
    }

    @Bean
    Ec2Client ec2Client() {
        return Ec2Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(awsCredentialsProvider())
                .build();
    }

    @Bean
    CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(awsCredentialsProvider())
                .build();
    }
}
