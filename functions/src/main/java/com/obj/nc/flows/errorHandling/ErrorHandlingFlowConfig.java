package com.obj.nc.flows.errorHandling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

import com.obj.nc.functions.processors.errorHandling.SpringMessageToFailedPaylodFunction;
import com.obj.nc.functions.sink.failedPaylodPersister.FailedPayloadPersister;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class ErrorHandlingFlowConfig {
	

	//Default channel for errorMessages used by spring
	@Qualifier("errorChannel")
	@Autowired private PublishSubscribeChannel errorChannel;
	@Autowired private SpringMessageToFailedPaylodFunction failedPaylodTranformer;
	@Autowired private FailedPayloadPersister failedPaylodPersister;
	
	
    @Bean
    public IntegrationFlow errorPayloadRecievedFlowConfig() {
        return 
        	IntegrationFlows.from(errorChannel)
				.handle(failedPaylodTranformer)
				.split()
				.handle(failedPaylodPersister)
        		.get();
    }

}
