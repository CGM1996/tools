package com.cgm.tools.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 附加配置
 *
 * @author cgm
 */
@Data
@Configuration
@ConfigurationProperties("extra")
public class ExtraConfig {
    private String defaultAvatar;

    private String baiduAppKey;

    private String baiduSecretKey;
}
