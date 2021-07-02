package com.obj.nc.flows.inputEventRouting.config;

import static com.obj.nc.flows.intenToMessageToSender.NotificationIntentProcessingFlowConfig.INTENT_PROCESSING_FLOW_INPUT_CHANNEL_ID;
import static com.obj.nc.flows.messageProcessing.MessageProcessingFlowConfig.MESSAGE_PROCESSING_FLOW_INPUT_CHANNEL_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.obj.nc.Get;
import com.obj.nc.domain.IsNotification;
import com.obj.nc.domain.notifIntent.NotificationIntent;
import com.obj.nc.flows.inputEventRouting.extensions.InputEventConverterExtension;
import com.obj.nc.functions.processors.event2Message.ExtensionsBasedEventConvertor;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class InputEventExtensionConvertingFlowConfig {
		
	public final static String EVENT_CONVERTING_EXTENSION_FLOW_ID = "EVENT_CONVERTING_EXTENSION_FLOW_ID";
	public final static String EVENT_CONVERTING_EXTENSION_FLOW_ID_INPUT_CHANNEL_ID = EVENT_CONVERTING_EXTENSION_FLOW_ID + "_INPUT";
				
    public static final String GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME = "genericEventSupplierFlowId"; 
    
	@Bean(EVENT_CONVERTING_EXTENSION_FLOW_ID_INPUT_CHANNEL_ID)
	public PublishSubscribeChannel messageProcessingInputChannel() {
		return new PublishSubscribeChannel();
	}
	
    @Bean
    public IntegrationFlow extensionBasedRoutingFlow(List<InputEventConverterExtension<? extends IsNotification>> eventProcessors) {
    	return IntegrationFlows
			.from(messageProcessingInputChannel())
			.handle(extensionsBasedEventConvertor(eventProcessors))
			.split()
			.route(messageOrIntentRouter())
			.get();
    }
    
    @Bean
    public ExtensionsBasedEventConvertor extensionsBasedEventConvertor(
    		List<InputEventConverterExtension<? extends IsNotification>> eventProcessors) {
    	return new ExtensionsBasedEventConvertor(eventProcessors);
    }
    
    @Bean
    public AbstractMessageRouter messageOrIntentRouter() {
        return new AbstractMessageRouter() {

            @Override
            protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
            	if (message.getPayload() instanceof com.obj.nc.domain.message.Message<?>) {
            		MessageChannel destChannel = Get.getBean(MESSAGE_PROCESSING_FLOW_INPUT_CHANNEL_ID, MessageChannel.class);
        			return Arrays.asList(destChannel);
            	}
            	
            	if (message.getPayload() instanceof NotificationIntent) {
            		MessageChannel destChannel = Get.getBean(INTENT_PROCESSING_FLOW_INPUT_CHANNEL_ID, MessageChannel.class);
        			return Arrays.asList(destChannel);
            	}
            	
            	throw new RuntimeException("Cannot route any other type than Message or Intent in InputEventExtensionConvertingFlowConfig");
            }
        };    
   }

}
