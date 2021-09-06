package com.obj.nc.flows.deliveryInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.obj.nc.functions.processors.deliveryInfo.DeliveryInfoFailedGenerator;
import com.obj.nc.functions.processors.deliveryInfo.DeliveryInfoPersister;
import com.obj.nc.functions.processors.deliveryInfo.DeliveryInfoProcessingGenerator;
import com.obj.nc.functions.processors.deliveryInfo.DeliveryInfoReadGenerator;
import com.obj.nc.functions.processors.deliveryInfo.DeliveryInfoSendGenerator;
import com.obj.nc.functions.processors.deliveryInfo.DeliveryInfoSendTransformer;

@Configuration
public class DeliveryInfoFlowConfig {
	
	public final static String DELIVERY_INFO_SEND_FLOW_ID = "DELIVERY_INFO_SEND_FLOW_ID";
	public final static String DELIVERY_INFO_SEND_FLOW_INPUT_CHANNEL_ID = DELIVERY_INFO_SEND_FLOW_ID + "_INPUT";
	
	public final static String DELIVERY_INFO_PROCESSING_FLOW_ID = "DELIVERY_INFO_PROCESSING_FLOW_ID";
	public final static String DELIVERY_INFO_PROCESSING_FLOW_INPUT_CHANNEL_ID = DELIVERY_INFO_PROCESSING_FLOW_ID + "_INPUT";
	
	public final static String DELIVERY_INFO_FAILED_FLOW_ID = "DELIVERY_INFO_FAILED_FLOW_ID";
	public final static String DELIVERY_INFO_FAILED_FLOW_INPUT_CHANNEL_ID = DELIVERY_INFO_FAILED_FLOW_ID + "_INPUT";
	
	public final static String DELIVERY_INFO_READ_FLOW_ID = "DELIVERY_INFO_READ_FLOW_ID";
	public final static String DELIVERY_INFO_READ_FLOW_INPUT_CHANNEL_ID = DELIVERY_INFO_READ_FLOW_ID + "_INPUT";
	
	public final static String DELIVERY_INFO_FLOW_OUTPUT_CHANNEL_ID = "DELIVERY_INFO_FLOW_OUTPUT_CHANNEL_ID";

	@Autowired private DeliveryInfoSendTransformer deliveryTransformer;
	@Autowired private DeliveryInfoPersister deliveryInfoPersister;
	@Autowired private DeliveryInfoSendGenerator deliveryInfoSendGenerator;
	@Autowired private DeliveryInfoReadGenerator deliveryInfoReadGenerator;
	@Autowired private DeliveryInfoFailedGenerator deliveryInfoFailedGenerator;
	@Autowired private DeliveryInfoProcessingGenerator deliveryInfoProcessingGenerator;
	@Autowired private ThreadPoolTaskScheduler executor;

	//Default channel for errorMessages used by spring
	@Autowired
	@Qualifier("errorChannel")
	private PublishSubscribeChannel errorChannel;

    @Bean
    public IntegrationFlow deliveryInfoFailedFlow() {
        return 
        	IntegrationFlows.from(deliveryInfoFailedInputChannel())
				.handle(deliveryInfoFailedGenerator)
//				.split()
				.handle(deliveryInfoPersister)
				.channel(DELIVERY_INFO_FLOW_OUTPUT_CHANNEL_ID)
        		.get();
    }
    
    @Bean
    public IntegrationFlow deliveryInfoSendFlow() {
        return 
        	IntegrationFlows.from(deliveryInfoSendInputChannel())
				.handle(deliveryInfoSendGenerator)
				.split()
				.handle(deliveryTransformer)
//				.split()
				.handle(deliveryInfoPersister)
				.channel(DELIVERY_INFO_FLOW_OUTPUT_CHANNEL_ID)
        		.get();
    }
    
    @Bean
    public IntegrationFlow deliveryInfoProcessingFlow() {
        return 
        	IntegrationFlows.from(deliveryInfoProcessingInputChannel())
				.handle(deliveryInfoProcessingGenerator)
				.split()
				.handle(deliveryTransformer)
//				.split()
				.handle(deliveryInfoPersister)
				.channel(DELIVERY_INFO_FLOW_OUTPUT_CHANNEL_ID)
        		.get();
    }
	
	@Bean
	public IntegrationFlow deliveryInfoReadFlow() {
		return
			IntegrationFlows.from(deliveryInfoReadInputChannel())
				.handle(deliveryInfoReadGenerator)
				.split()
				.handle(deliveryTransformer)
//				.split()
				.handle(deliveryInfoPersister)
				.channel(DELIVERY_INFO_FLOW_OUTPUT_CHANNEL_ID)
				.get();
	}
    
	@Bean(DELIVERY_INFO_SEND_FLOW_INPUT_CHANNEL_ID)
	public MessageChannel deliveryInfoSendInputChannel() {
		return new PublishSubscribeChannel(executor);
	}
	
	@Bean(DELIVERY_INFO_PROCESSING_FLOW_INPUT_CHANNEL_ID)
	public MessageChannel deliveryInfoProcessingInputChannel() {
		return new PublishSubscribeChannel(executor);
	}
	
	@Bean(DELIVERY_INFO_FAILED_FLOW_INPUT_CHANNEL_ID)
	public MessageChannel deliveryInfoFailedInputChannel() {
		return new PublishSubscribeChannel(executor);
	}
	
	@Bean(DELIVERY_INFO_READ_FLOW_INPUT_CHANNEL_ID)
	public MessageChannel deliveryInfoReadInputChannel() {
		return new PublishSubscribeChannel(executor);
	}
	
	@Bean(DELIVERY_INFO_FLOW_OUTPUT_CHANNEL_ID)
	public MessageChannel deliveryInfoOutputChannel() {
		return new PublishSubscribeChannel(executor);
	}

}
