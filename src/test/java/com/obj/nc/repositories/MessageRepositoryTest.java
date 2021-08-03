package com.obj.nc.repositories;

import static com.obj.nc.flows.inputEventRouting.config.InputEventRoutingFlowConfig.GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.obj.nc.testUtils.BaseIntegrationTest;
import com.obj.nc.testUtils.SystemPropertyActiveProfileResolver;
import com.obj.nc.domain.endpoints.EmailEndpoint;
import com.obj.nc.domain.message.EmailMessage;
import com.obj.nc.domain.message.MessagePersistantState;
import com.obj.nc.utils.JsonUtils;

@ActiveProfiles(value = "test", resolver = SystemPropertyActiveProfileResolver.class)
@SpringIntegrationTest(noAutoStartup = GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME)
@SpringBootTest
public class MessageRepositoryTest extends BaseIntegrationTest {
	
	@Autowired MessageRepository messageRepository;
	@Autowired EndpointsRepository endpointRepo;
	
	@BeforeEach
	void setUp(@Autowired JdbcTemplate jdbcTemplate) {
		purgeNotifTables(jdbcTemplate);
	}
	
	@Test
	public void testPersistingSingleMessage() {
		//GIVEN
		EmailMessage emailMsg = createTestMessage();	
		
		emailMsg.getHeader().setFlowId("default-flow");
		UUID[] eventIds = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
		emailMsg.getHeader().setEventIdsAsArray(eventIds);
		
		messageRepository.save(emailMsg.toPersistantState());
		
		Optional<MessagePersistantState> oEmailInDB = messageRepository.findById(emailMsg.getId());
		
		Assertions.assertThat(oEmailInDB.isPresent()).isTrue();
		EmailMessage emailInDB = oEmailInDB.get().toMessage();
		Assertions.assertThat(emailInDB.getPayloadTypeName()).isEqualTo("EMAIL_MESSAGE"); 
		Assertions.assertThat(emailInDB.getTimeCreated()).isNotNull();
		Assertions.assertThat(emailInDB.getHeader().getFlowId()).isEqualTo("default-flow");
		Assertions.assertThat(emailInDB.getHeader().getEventIdsAsArray()).isEqualTo(eventIds);
	}

	
	@Test
	public void testFindByIdInContainingIntentsId() {
		EmailMessage emailMsg = createTestMessage();		

		UUID[] eventIds = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
		emailMsg.getHeader().setEventIdsAsArray(eventIds);
		emailMsg.setId(UUID.fromString("bf44aedf-6439-4e7f-a136-3dc78202981b"));
		messageRepository.save(emailMsg.toPersistantState());
		// WHEN
		List<MessagePersistantState> oIntentInDB = messageRepository.findByIdIn(Arrays.asList(
				UUID.fromString("bf44aedf-6439-4e7f-a136-3dc78202981a"),
				UUID.fromString("bf44aedf-6439-4e7f-a136-3dc78202981b")));
		// THEN
		Assertions.assertThat(oIntentInDB.size()).isEqualTo(1);
	}
	
	@Test
	public void testFindByIdInNotContainingIntentsId() {
		EmailMessage emailMsg = createTestMessage();		
		
		emailMsg.setId(UUID.fromString("bf44aedf-6439-4e7f-a136-3dc78202981b"));
		messageRepository.save(emailMsg.toPersistantState());
		// WHEN
		List<MessagePersistantState> oIntentInDB = messageRepository.findByIdIn(Arrays.asList(
				UUID.fromString("bf44aedf-6439-4e7f-a136-3dc78202981c")));
		// THEN
		Assertions.assertThat(oIntentInDB.isEmpty()).isTrue();
	}
	
	@Test
	public void testMessageReferencingNonExistingEnpoindFails() {
		//GIVEN
		String INPUT_JSON_FILE = "messages/email_message.json";
		EmailMessage emailMsg = JsonUtils.readObjectFromClassPathResource(INPUT_JSON_FILE, EmailMessage.class);				
		emailMsg.setId(UUID.fromString("bf44aedf-6439-4e7f-a136-3dc78202981b"));
		
		// WHEN
		Assertions.assertThatThrownBy(
				() -> messageRepository.save(emailMsg.toPersistantState()))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Message is referencing endpoint via endpointIds atribute which cannot be found in the DB");
	}
	

	private EmailMessage createTestMessage() {
		String INPUT_JSON_FILE = "messages/email_message.json";
		EmailMessage emailMsg = JsonUtils.readObjectFromClassPathResource(INPUT_JSON_FILE, EmailMessage.class);
		emailMsg.ensureEnpointsPersisted();
		
		return emailMsg;
	}
	
}
