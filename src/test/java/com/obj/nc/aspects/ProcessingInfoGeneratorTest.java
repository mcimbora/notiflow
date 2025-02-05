/*
 *   Copyright (C) 2021 the original author or authors.
 *
 *   This file is part of Notiflow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.obj.nc.aspects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.JsonNode;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.obj.nc.domain.event.GenericEvent;
import com.obj.nc.domain.headers.Header;
import com.obj.nc.domain.headers.ProcessingInfo;
import com.obj.nc.domain.message.EmailMessage;
import com.obj.nc.domain.message.Message;
import com.obj.nc.domain.notifIntent.NotificationIntent;
import com.obj.nc.domain.notifIntent.content.IntentContent;
import com.obj.nc.functions.processors.messageBuilder.MessagesFromIntentGenerator;
import com.obj.nc.functions.processors.senders.EmailSender;
import com.obj.nc.functions.sources.genericEvents.GenericEventsSupplier;
import com.obj.nc.repositories.GenericEventRepository;
import com.obj.nc.repositories.GenericEventRepositoryTest;
import com.obj.nc.repositories.ProcessingInfoRepository;
import com.obj.nc.testUtils.BaseIntegrationTest;
import com.obj.nc.testUtils.SystemPropertyActiveProfileResolver;
import com.obj.nc.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.obj.nc.flows.inputEventRouting.config.InputEventRoutingFlowConfig.GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@ActiveProfiles(value = "test", resolver = SystemPropertyActiveProfileResolver.class)
@SpringIntegrationTest(noAutoStartup = GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME)
@SpringBootTest(properties = {
    "nc.contacts-store.jsonStorePathAndFileName=src/test/resources/contact-store/contact-store.json", 
})
public class ProcessingInfoGeneratorTest extends BaseIntegrationTest {
	
	@Autowired private GenericEventsSupplier eventSupplier;
	@Autowired private GenericEventRepository eventRepository;
    @Autowired private MessagesFromIntentGenerator generateMessagesFromIntent;
    @Autowired private EmailSender functionSend;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ProcessingInfoRepository procInfoRepo;
	
    @BeforeEach
    void setUp() {
        MDC.put("testName", this.getClass().getSimpleName());

        jdbcTemplate.execute("truncate table nc_processing_info");
        jdbcTemplate.execute("truncate table nc_endpoint cascade");        
    }
    
	@Test
	@SuppressWarnings("unchecked")
	void testCopyHeaderInOneToNProcessor() {
		//GIVEN
        NotificationIntent notificationIntent = NotificationIntent.createWithStaticContent(
			"Subject", 
			"Text"
		); 
        notificationIntent.addRecipientsByName(
            "John Doe",
            "John Dudly",
            "Objectify"
        );
        notificationIntent.getHeader().setAttributeValue("custom-property1", Arrays.asList("xx","yy"));
        notificationIntent.getHeader().setAttributeValue("custom-property2", "zz");

		//WHEN
		List<EmailMessage> result = (List<EmailMessage>)generateMessagesFromIntent.apply(notificationIntent);
		
		//THEN
		assertThat(result.size()).isEqualTo(3);
		
		Message<?> message = result.get(0);
		Header header = message.getHeader();
		assertThat(header.getFlowId()).isEqualTo(notificationIntent.getHeader().getFlowId());
		assertThat(header.getAttributes())
			.contains(
					entry("custom-property1", Arrays.asList("xx","yy")), 
					entry("custom-property2", "zz")
			);
	}
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonTypeInfo(include = As.PROPERTY, use = Id.NAME)
    @JsonSubTypes({ 
    	@Type(value = TestPayload.class, name = "TYPE_NAME")}
    )
    public static class TestPayload {
    	
    	private String content;
    }

    @Test
    void testPersistPIForGenericEvent() {
    	//GIVEN
        GenericEvent event = createUnprocessedEvent();		
		eventRepository.save(event);
		
		//WHEN
		GenericEvent eventFromDB = eventSupplier.get();

        // when
        // ProcessingInfo persistence is done using aspect and in an async way

        // then
        Assertions.assertThat(
        		eventFromDB.getId())
        	.isIn(Arrays.asList(
        			eventFromDB.getHeader().getProcessingInfo().getEventIds())
        );
        UUID eventId = eventFromDB.getId();
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> procInfoRepo.findByAnyEventIdAndStepName(eventId, "InputEventSupplier").size()>0);

        List<ProcessingInfo> persistedPIs = procInfoRepo.findByAnyEventIdAndStepName(eventId, "InputEventSupplier");
        Assertions.assertThat(persistedPIs.size()).isEqualTo(1);
        
        ProcessingInfo persistedPI = persistedPIs.iterator().next();

        Assertions.assertThat(persistedPI.getEventIds()[0]).isEqualTo(eventId);
        Assertions.assertThat(persistedPI.getPayloadJsonStart()).isNull();
        Assertions.assertThat(persistedPI.getPayloadJsonEnd()).contains(JsonUtils.writeObjectToJSONString(event.getPayloadJson()));
        Assertions.assertThat(persistedPI.getStepDurationMs()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(persistedPI.getStepName()).isEqualTo("InputEventSupplier");
        Assertions.assertThat(persistedPI.getTimeProcessingStart()).isNotNull();
        Assertions.assertThat(persistedPI.getTimeProcessingEnd()).isNotNull();
        Assertions.assertThat(persistedPI.getProcessingId()).isNotNull();
        Assertions.assertThat(persistedPI.getPrevProcessingId()).isNull();
        Assertions.assertThat(persistedPI.getStepIndex()).isEqualTo(0);
    }

    @Test
	@SuppressWarnings("unchecked")
    void testPersistPIForMessageFromIntentStep() {
        // given
        GenericEvent event = createUnprocessedEvent();		
		eventRepository.save(event);

        //generates processing info
        GenericEvent eventFromDB = eventSupplier.get();
        UUID eventId = eventFromDB.getEventId();
		
		NotificationIntent notificationIntent = NotificationIntent.createWithStaticContent(
			"Business Intelligence (BI) Developer", 
			"We are looking for a Business Intelligence"
		);  	
        notificationIntent.addRecipientsByName(
            "John Doe",
            "John Dudly",
            "Objectify"
        );
        //need to set manually to simulate that intent has been generated from event 
        notificationIntent.addPreviousEventId(eventId);
        notificationIntent.getHeader().setProcessingInfo(eventFromDB.getHeader().getProcessingInfo());
        
        UUID[] originalEventIDs = notificationIntent.getPreviousEventIds().toArray(new UUID[0]);
        List<EmailMessage> messages = (List<EmailMessage>)generateMessagesFromIntent.apply(notificationIntent);

        // ProcessingInfo persistence is done using aspect and in an async way

        // then
        Assertions.assertThat(messages.size()).isEqualTo(3);
        assertMessagesHaveOriginalEventId(originalEventIDs, messages);
            
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> procInfoRepo.findByAnyEventIdAndStepName(eventId, "GenerateMessagesFromIntent").size()>0);
       
        List<ProcessingInfo> persistedPIs = procInfoRepo.findByAnyEventIdAndStepName(eventId, "GenerateMessagesFromIntent");
        
        Assertions.assertThat(persistedPIs.size()).isEqualTo(3);
        
        ProcessingInfo persistedPI = persistedPIs.iterator().next();
        Message<?> message = messages.iterator().next();

        Assertions.assertThat(persistedPI.getEventIds()[0]).isEqualTo(eventId);
        Assertions.assertThat(persistedPI.getPayloadJsonStart()).contains(notificationIntent.getBody().toJSONString());
        Assertions.assertThat(persistedPI.getPayloadJsonEnd()).contains(message.getBody().toJSONString());
        Assertions.assertThat(persistedPI.getStepDurationMs()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(persistedPI.getStepName()).isEqualTo("GenerateMessagesFromIntent");
        Assertions.assertThat(persistedPI.getTimeProcessingStart()).isNotNull();
        Assertions.assertThat(persistedPI.getTimeProcessingEnd()).isAfterOrEqualTo(persistedPI.getTimeProcessingStart());
        Assertions.assertThat(persistedPI.getProcessingId()).isNotNull();
        Assertions.assertThat(notificationIntent.getProcessingInfo().getProcessingId()).isNotNull();        
        Assertions.assertThat(persistedPI.getPrevProcessingId()).isEqualTo(notificationIntent.getProcessingInfo().getProcessingId());
        Assertions.assertThat(persistedPI.getStepIndex()).isEqualTo(1);

    }

	private void assertMessagesHaveOriginalEventId(UUID[] originalEventIDs, List<EmailMessage> messages) {
		messages.forEach(message -> {
        	
            Assertions.assertThat(
            		message.getProcessingInfo().getEventIds())
            	.isEqualTo(originalEventIDs);
        	
            Assertions.assertThat(
            		message.getPreviousEventIds())
            	.isEqualTo(Arrays.asList(
            		message.getHeader().getProcessingInfo().getEventIds())
            );
        });
	}

    @SuppressWarnings("unchecked")
	@Test
    void testPersistPIForSendMessage() {
        // given
		GenericEvent event = GenericEventRepositoryTest.createProcessedEvent();
		UUID eventId = eventRepository.save(event).getId();

		NotificationIntent notificationIntent = NotificationIntent.createWithStaticContent(
			"Business Intelligence (BI) Developer", 
			"We are looking for a Business Intelligence"
		);  	
        notificationIntent.addRecipientsByName(
            "John Doe",
            "John Dudly",
            "Objectify"
        );
        notificationIntent.addPreviousEventId(eventId);
        UUID[] originalEventIDs = notificationIntent.getPreviousEventIds().toArray(new UUID[0]);
        
        List<EmailMessage> messages = (List<EmailMessage>)generateMessagesFromIntent.apply(notificationIntent);                      
        
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> procInfoRepo.findByAnyEventIdAndStepName(eventId, "GenerateMessagesFromIntent").size()>0);
        
    	 EmailMessage email = (EmailMessage)messages.iterator().next();
    	 email.getProcessingInfo().setVersion(0);//need to set to avoid async error in test. it's no hack
    	 String messageJsonBeforeSend = email.toJSONString();
         UUID previousProcessingId = email.getProcessingInfo().getProcessingId();
         
         // when
         // ProcessingInfo persistence is done using aspect and in an async way
         EmailMessage sendMessage = functionSend.apply(email);
     	 
         //then
         Assertions.assertThat(sendMessage).isNotNull();
         assertMessagesHaveOriginalEventId(originalEventIDs, Arrays.asList(sendMessage));
         

         Awaitility.await().atMost(Duration.ofSeconds(3)).until(() -> procInfoRepo.findByAnyEventIdAndStepName(eventId, "SendEmail").size()>0);
        
         List<ProcessingInfo> persistedPIs = procInfoRepo.findByAnyEventIdAndStepName(eventId, "SendEmail");
         
         Assertions.assertThat(persistedPIs.size()).isEqualTo(1);
         
         ProcessingInfo persistedPI = persistedPIs.iterator().next();

         Assertions.assertThat(persistedPI.getEventIds()[0]).isEqualTo(eventId);
         Assertions.assertThat(persistedPI.getPayloadJsonStart()).contains(messageJsonBeforeSend);
         Assertions.assertThat(persistedPI.getPayloadJsonEnd()).contains(sendMessage.getBody().toJSONString());
         Assertions.assertThat(persistedPI.getStepDurationMs()).isGreaterThanOrEqualTo(0);
         Assertions.assertThat(persistedPI.getStepName()).isEqualTo("SendEmail");
         Assertions.assertThat(persistedPI.getTimeProcessingStart()).isNotNull();
         Assertions.assertThat(persistedPI.getTimeProcessingEnd()).isAfterOrEqualTo(persistedPI.getTimeProcessingStart());
         Assertions.assertThat(persistedPI.getProcessingId()).isNotNull();
         Assertions.assertThat(persistedPI.getPrevProcessingId()).isEqualTo(previousProcessingId);
         Assertions.assertThat(persistedPI.getStepIndex()).isEqualTo(1);
         
         procInfoRepo.delete(persistedPI); //not to interfere with next iteration
    }
    
    
    @RegisterExtension
    protected static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
      	.withConfiguration(
      			GreenMailConfiguration.aConfig()
      			.withUser("no-reply@objectify.sk", "xxx"))
      	.withPerMethodLifecycle(true);


          
	public static GenericEvent createUnprocessedEvent() {
    	JsonNode payload = JsonUtils.writeObjectToJSONNode(TestPayload.builder().content("Test").build());
    	
		GenericEvent event = GenericEvent.builder()
				.externalId(UUID.randomUUID().toString())
				.flowId("FLOW_ID")
				.id(UUID.randomUUID())
				.payloadJson(payload)
				.build();
		return event;
	}
}
