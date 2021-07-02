package com.obj.nc.flows.messageProcessing;

import static com.obj.nc.flows.deliveryInfo.DeliveryInfoFlowConfig.DELIVERY_INFO_PROCESSING_FLOW_INPUT_CHANNEL_ID;
import static com.obj.nc.flows.emailFormattingAndSending.EmailProcessingFlowConfig.EMAIL_FORMAT_AND_SEND_ROUTING_FLOW_INPUT_CHANNEL_ID;
import static com.obj.nc.flows.emailFormattingAndSending.EmailProcessingFlowConfig.EMAIL_SEND_ROUTING_FLOW_INPUT_CHANNEL_ID;
import static com.obj.nc.flows.mailchimpSending.MailchimpProcessingFlowConfig.MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID;
import static com.obj.nc.flows.smsFormattingAndSending.SmsProcessingFlowConfig.SMS_PROCESSING_FLOW_INPUT_CHANNEL_ID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

import com.obj.nc.domain.message.EmailMessage;
import com.obj.nc.domain.message.EmailMessageTemplated;
import com.obj.nc.domain.message.MailChimpMessage;
import com.obj.nc.domain.message.SmsMessageTemplated;
import com.obj.nc.functions.processors.messageBuilder.MessageByRecipientTokenizer;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class MessageProcessingFlowConfig {
		
	@Autowired MessageByRecipientTokenizer<?> messageByRecipientTokenizer;
	
	public final static String MESSAGE_PROCESSING_FLOW_ID = "MESSAGE_PROCESSING_FLOW_ID";
	public final static String MESSAGE_PROCESSING_FLOW_INPUT_CHANNEL_ID = MESSAGE_PROCESSING_FLOW_ID + "_INPUT";
	
	@Bean(MESSAGE_PROCESSING_FLOW_INPUT_CHANNEL_ID)
	public PublishSubscribeChannel messageProcessingInputChannel() {
		return new PublishSubscribeChannel();
	}
	
	@Bean(MESSAGE_PROCESSING_FLOW_ID)
	public IntegrationFlow messageProcessingFlowDefinition() {
		return IntegrationFlows
				.from(messageProcessingInputChannel())
				.transform(messageByRecipientTokenizer)
				.split()
				.wireTap( flowConfig -> 
					flowConfig.channel(DELIVERY_INFO_PROCESSING_FLOW_INPUT_CHANNEL_ID)
				)
				.routeToRecipients(spec -> spec.
						recipient(EMAIL_SEND_ROUTING_FLOW_INPUT_CHANNEL_ID, m-> m instanceof EmailMessage).
						recipient(EMAIL_FORMAT_AND_SEND_ROUTING_FLOW_INPUT_CHANNEL_ID, m-> m instanceof EmailMessageTemplated).
						recipient(SMS_PROCESSING_FLOW_INPUT_CHANNEL_ID, m-> m instanceof SmsMessageTemplated).
						recipient(MAILCHIMP_PROCESSING_FLOW_INPUT_CHANNEL_ID, m-> m instanceof MailChimpMessage).
						defaultOutputToParentFlow()
				)
				.get();
	}
	
}
