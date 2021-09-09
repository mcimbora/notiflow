package com.obj.nc.flows.errorHandling;

import static com.obj.nc.flows.inputEventRouting.config.InputEventRoutingFlowConfig.GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.obj.nc.config.SpringIntegration;
import com.obj.nc.controllers.ErrorHandlingRestController;
import com.obj.nc.flows.errorHandling.ErrorHandlingTests.TestModeTestConfiguration.TestFlow1;
import com.obj.nc.flows.errorHandling.domain.FailedPayload;
import com.obj.nc.repositories.FailedPayloadRepository;
import com.obj.nc.testUtils.BaseIntegrationTest;
import com.obj.nc.testUtils.SystemPropertyActiveProfileResolver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ActiveProfiles(value = "test", resolver = SystemPropertyActiveProfileResolver.class)
@SpringIntegrationTest(noAutoStartup = GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME)
@SpringBootTest(properties = {
		"test-flow-gateway=true", //this is strange, if I don't make TestFlow1 conditional, some unrelated test fail because they don't see testInputChannel1
		"spring.integration.channels.error.requireSubscribers=false" // https://docs.spring.io/spring-integration/reference/html/error-handling.html
}) 
public class ErrorHandlingTests {

	@Autowired FailedPayloadRepository failedPayloadRepo;
	@Autowired TestFlow1 testFlow1;
	@Autowired ErrorHandlingRestController errorHandlingController;
	@Autowired @Qualifier(SpringIntegration.OBJECT_MAPPER_FOR_SPRING_MESSAGES_BEAN_NAME) ObjectMapper jsonConverterForMessages;
	
	private static boolean processingFinished;
	
    @BeforeEach
    void setUp(@Autowired JdbcTemplate jdbcTemplate) {
    	BaseIntegrationTest.purgeNotifTables(jdbcTemplate);
    	processingFinished = false;
    }
	
	@Test
	@SuppressWarnings("unchecked")
	public void testPayloadWithErrorProduced() throws InterruptedException, ExecutionException, TimeoutException, JsonProcessingException {
        //WHEN
		TestPayload payload = TestPayload.builder().str("ss").build();
        Future<TestPayload> result = testFlow1.execute(payload);
 
        //THEN fail documented
		Awaitility.await().atMost(1000, TimeUnit.SECONDS).until(() -> 
			failedPayloadRepo.count() > 0);
		
		FailedPayload failed = failedPayloadRepo.findAll().iterator().next();
		
		//WHEN problem fixed and corrected message saved
		Message<TestPayload> failedMsg = (Message<TestPayload>)jsonConverterForMessages.treeToValue(failed.getMessageJson(), Message.class);
		failedMsg.getPayload().setStr("1");
		
		JsonNode jsonTree = jsonConverterForMessages.valueToTree(failedMsg);
		failed.setMessageJson(jsonTree);
		failedPayloadRepo.save(failed);
		
		//TRY resurrect
		errorHandlingController.resurrect(failed.getId().toString());
		
        //THEN processing finished successfully
		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> processingFinished == true);
	}
	
	
    @TestConfiguration	
    @EnableIntegrationManagement
    public static class TestModeTestConfiguration {

        @Bean
        public IntegrationFlow errorProducingFlow1() {
            return 
            	IntegrationFlows.from(testInputChannel1())
    				.handle((p,h)-> {
    					//this can fail with NumberFormatException
    					return ((TestPayload)p).convertStrToNum();
    					} 
    				)
    				.handle(m ->
    					processingFinished = true
    				)
            		.get();
        }
        
        @Bean(name = "testInputChannel1")
        public MessageChannel testInputChannel1() {
        	return new PublishSubscribeChannel();
        }
        
        @MessagingGateway(name = "TestFlow1", errorChannel = ErrorHandlingFlowConfig.ERROR_CHANNEL_NAME)
        @ConditionalOnProperty(value = "test-flow-gateway", havingValue = "true")
        public static interface TestFlow1 {
        	
        	@Gateway(requestChannel = "testInputChannel1")
        	Future<TestPayload> execute(TestPayload payload);
        	
        }

    }
    
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonTypeInfo(include = As.PROPERTY, use = Id.CLASS)
    public static class TestPayload {
    	
    	private Integer num;
    	private String str;
    	
    	public TestPayload convertStrToNum() {
    		num = Integer.parseInt(str);
    		return this;
    	}
    }
}
