package com.tbm.admin.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "images")
public class ImagesPathProps {

    private String background;

    private String result;

    private String defaultThumbnail;
}