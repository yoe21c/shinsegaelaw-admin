package com.tbm.admin.model.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws.ec2")
public class AwsEc2Props {
    private String amiId;
    private String keyName;
    private String securityGroup;
}
