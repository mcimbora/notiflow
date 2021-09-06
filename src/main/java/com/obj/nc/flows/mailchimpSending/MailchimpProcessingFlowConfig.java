package com.obj.nc.flows.mailchimpSending;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import com.obj.nc.functions.processors.endpointPersister.EndpointPersister;
import com.obj.nc.functions.processors.messagePersister.MessagePersister;
import com.obj.nc.functions.processors.senders.MailchimpSender;
import com.obj.nc.functions.sink.payloadLogger.PaylaodLoggerSinkConsumer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MailchimpProcessingFlowConfig {
    
    public final static String MAILCHIMP_PROCESSING_FLOW_ID = "MAILCHIMP_PROCESSING_FLOW_ID";
    public final static String MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID = MAILCHIMP_PROCESSING_FLOW_ID + "_INPUT";
    public final static String LOG_CONSUMER_HANDLER_ID = "LOG_CONSUMER";
    public final static String LOG_CONSUMER_HANDLER_METHOD_NAME = "accept";
    
    private final MailchimpSender mailchimpSender;
    private final PaylaodLoggerSinkConsumer logConsumer;
    private final MessagePersister messagePersister;
    private final EndpointPersister endpointPersister;
    
    @Bean(MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID)
    public MessageChannel mailchimpProcessingInputChangel() {
        return new PublishSubscribeChannel();
    }
    
    @Bean(MAILCHIMP_PROCESSING_FLOW_ID)
    public IntegrationFlow mailchimpProcessingFlowDefinition() {
        return IntegrationFlows
                .from(MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID)
                .handle(endpointPersister)
                .handle(messagePersister)
                .handle(mailchimpSender)
                .handle(logConsumer, LOG_CONSUMER_HANDLER_METHOD_NAME, c -> c.id(LOG_CONSUMER_HANDLER_ID))
                .get();
    }
    
}
