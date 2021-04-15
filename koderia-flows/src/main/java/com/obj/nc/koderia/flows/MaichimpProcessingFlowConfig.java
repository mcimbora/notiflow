package com.obj.nc.koderia.flows;

import com.obj.nc.functions.sink.payloadLogger.PaylaodLoggerSinkConsumer;
import com.obj.nc.koderia.functions.processors.mailchimpSender.MailchimpSenderProcessorFunction;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

@Configuration
@AllArgsConstructor
public class MaichimpProcessingFlowConfig {
	
	public final static String MAILCHIMP_PROCESSING_FLOW_ID = "MAILCHIMP_PROCESSING_FLOW_ID";
	public final static String MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID = MAILCHIMP_PROCESSING_FLOW_ID + "_INPUT";
	public final static String LOG_CONSUMER_HANDLER_ID = "LOG_CONSUMER";
	public final static String LOG_CONSUMER_HANDLER_METHOD_NAME = "accept";
	
	private final MailchimpSenderProcessorFunction mailchimpSender;
	private final PaylaodLoggerSinkConsumer logConsumer;

	@Bean(MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID)
	public MessageChannel mailchimpProcessingInputChangel() {
		return new PublishSubscribeChannel();
	}
	
	@Bean(MAILCHIMP_PROCESSING_FLOW_ID)
	public IntegrationFlow mailchimpProcessingFlowDefinition() {
		return IntegrationFlows
				.from(MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID)
				.transform(mailchimpSender)
				.handle(logConsumer, LOG_CONSUMER_HANDLER_METHOD_NAME, c -> c.id(LOG_CONSUMER_HANDLER_ID)).get();
	}
	
}
