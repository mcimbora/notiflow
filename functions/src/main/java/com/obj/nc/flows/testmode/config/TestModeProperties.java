package com.obj.nc.flows.testmode.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "nc.flows.test-mode")
public class TestModeProperties {

    //This two should be here
    private List<String> recipients;

    private int pollMockSourcesPeriodInSeconds=5;
    
    private boolean enabled = false;

}
