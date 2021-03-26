package com.obj.nc.flows.testmode.config;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.store.MessageGroup;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;

import com.obj.nc.flows.testmode.functions.processors.AggregateToSingleEmailTransformer;
import com.obj.nc.flows.testmode.functions.sources.GreenMailReceiverSourceSupplier;
import com.obj.nc.functions.processors.messageAggregator.MessageAggregator;
import com.obj.nc.functions.processors.messageAggregator.aggregations.MessageAggregationStrategy;
import com.obj.nc.functions.processors.messageAggregator.correlations.EventIdBasedCorrelationStrategy;
import com.obj.nc.functions.processors.messageTemplating.EmailTemplateFormatter;
import com.obj.nc.functions.processors.senders.EmailSender;
import com.obj.nc.functions.sink.payloadLogger.PaylaodLoggerSinkConsumer;

import lombok.extern.log4j.Log4j2;

@Configuration
@ConditionalOnProperty(value = "nc.flows.test-mode.enabled", havingValue = "true")
@Log4j2
public class TestModeFlowConfig {
	
	@Autowired private TestModeProperties testModeProps;
	@Autowired private GreenMailReceiverSourceSupplier greenMailMessageSource;
    
    @Qualifier(TestModeBeansConfig.TEST_MODE_EMAIL_SENDER_FUNCTION_BEAN_NAME)
    @Autowired private EmailSender sendEmailRealSmtp;
    @Autowired private PaylaodLoggerSinkConsumer logConsumer;
    @Autowired private MessageAggregationStrategy aggregationStrategy;
    @Autowired private EmailTemplateFormatter digestEmailFormatter;
    
	
	public final static String TEST_MODE_GREEN_MAIL_SOURCE_BEAN_NAME = "greenMailSource";

    @Bean
    public IntegrationFlow testModeSendMessage() {
        return IntegrationFlows.from(greenMailMessageSource,
                        config -> config.poller(testModeSourcePoller()).id(TEST_MODE_GREEN_MAIL_SOURCE_BEAN_NAME))
        		.split()
        		.channel(c -> c.executor(Executors.newCachedThreadPool()))
        		.aggregate(
        			aggSpec-> aggSpec
        				.correlationStrategy( testModeCorrelationStrategy() )
        				.releaseStrategy( testModeReleaseStrategy() )
        					.groupTimeout(2000) //wait max 2 sec for another message to arrive, if not, release
        					.sendPartialResultOnExpiry(true)
        					.expireGroupsUponCompletion(true)
        					.expireGroupsUponTimeout(true)
        				.outputProcessor( testModeMessageAggregator() )
        			)
        		.transform(aggregateToSingleEmailTransformer())
        		.transform(digestEmailFormatter)
                .transform(sendEmailRealSmtp)
                .handle(logConsumer).get();
    }
    
    @Bean
    public CorrelationStrategy testModeCorrelationStrategy() {
    	return new EventIdBasedCorrelationStrategy(); //pull all in one group
    }
    
    @Bean
    public MessageAggregator testModeMessageAggregator() {
    	return new MessageAggregator(aggregationStrategy);
    }
    
	@Bean
	public ReleaseStrategy testModeReleaseStrategy() {
	  	return new NeverReleaseStrategy(); //based on timeout, not release strategy
	} 
	
	public static class NeverReleaseStrategy implements ReleaseStrategy {

		@Override
		public boolean canRelease(MessageGroup group) {
			return false;
		}

	}

	
	public static class LoggingSimpleSequenceSizeReleaseStrategy implements ReleaseStrategy {

		@Override
		public boolean canRelease(MessageGroup group) {
			boolean releasing = group.getSequenceSize() == group.size();
			
			log.info("Having {} sequence size, have {} group size, releasing: {}",group.getSequenceSize(),group.size(),releasing);
			return releasing;
		}

	}


    @Bean
    public Trigger testModeSourceTrigger() {
        return new PeriodicTrigger(testModeProps.getPeriodInSeconds(), TimeUnit.SECONDS);
    }

    @Bean
    public PollerSpec testModeSourcePoller() {
        return Pollers.trigger(testModeSourceTrigger());
    }
    
    @Bean
    public AggregateToSingleEmailTransformer aggregateToSingleEmailTransformer() {
    	return new AggregateToSingleEmailTransformer();
    }

}
