package com.shinsegaelaw.admin.model.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws.credentials")
public class AwsCredentialsProps {
    private String accessKeyId;
    private String secretKey;
}
