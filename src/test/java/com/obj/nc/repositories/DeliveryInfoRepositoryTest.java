package com.obj.nc.repositories;

import static com.obj.nc.flows.inputEventRouting.config.InputEventRoutingFlowConfig.GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.context.ActiveProfiles;

import com.obj.nc.testUtils.SystemPropertyActiveProfileResolver;
import com.obj.nc.domain.endpoints.EmailEndpoint;
import com.obj.nc.domain.event.GenericEvent;
import com.obj.nc.functions.processors.deliveryInfo.domain.DeliveryInfo;
import com.obj.nc.functions.processors.deliveryInfo.domain.DeliveryInfo.DELIVERY_STATUS;

@ActiveProfiles(value = "test", resolver = SystemPropertyActiveProfileResolver.class)
@SpringIntegrationTest(noAutoStartup = GENERIC_EVENT_CHANNEL_ADAPTER_BEAN_NAME)
@SpringBootTest
public class DeliveryInfoRepositoryTest {

	@Autowired DeliveryInfoRepository deliveryInfoRepo;
	@Autowired GenericEventRepository eventRepo;
	@Autowired EndpointsRepository endpointRepo;
	
	@BeforeEach
	public void clean() {
		deliveryInfoRepo.deleteAll();
	}
	
	@Test
	public void testPersistingSingleInfo() {
		//GIVEN
		GenericEvent event = GenericEventRepositoryTest.createDirectMessageEvent();
		eventRepo.save(event);
		
		EmailEndpoint email1 = EmailEndpoint.builder().email("jancuzy@gmail.com").build();
		UUID endpointId = endpointRepo.persistEnpointIfNotExists(email1).getId();
		
		//WHEN
		DeliveryInfo deliveryInfo = DeliveryInfo.builder()
				.endpointId(endpointId)
				.eventId(event.getId())
				.status(DELIVERY_STATUS.SENT)
				.id(UUID.randomUUID())
				.build();
		
		deliveryInfoRepo.save(deliveryInfo);
		
		//AND WHEN
		Optional<DeliveryInfo> infoInDb = deliveryInfoRepo.findById(deliveryInfo.getId());
		
		//THEN
		Assertions.assertThat(infoInDb.isPresent()).isTrue();
	}
	
	@Test
	public void testFindByEventId() {
		//GIVEN
		GenericEvent event = GenericEventRepositoryTest.createDirectMessageEvent();
		UUID eventId = eventRepo.save(event).getId();
		
		GenericEvent event2 = GenericEventRepositoryTest.createDirectMessageEvent();
		UUID eventId2 = eventRepo.save(event2).getId();
		
		EmailEndpoint email1 = EmailEndpoint.builder().email("jancuzy@gmail.com").build();
		UUID endpointId = endpointRepo.persistEnpointIfNotExists(email1).getId();
		
		//AND GIVEN
		DeliveryInfo deliveryInfo1 = DeliveryInfo.builder()
				.endpointId(endpointId)
				.eventId(eventId)
				.status(DELIVERY_STATUS.SENT)
				.id(UUID.randomUUID())
				.build();
		
		DeliveryInfo deliveryInfo2 = DeliveryInfo.builder()
				.endpointId(endpointId)
				.eventId(eventId)
				.status(DELIVERY_STATUS.SENT)
				.id(UUID.randomUUID())
				.build();
		
		DeliveryInfo deliveryInfo3 = DeliveryInfo.builder()
				.endpointId(endpointId)
				.eventId(eventId2)
				.status(DELIVERY_STATUS.SENT)
				.id(UUID.randomUUID())
				.build();
		
		deliveryInfoRepo.save(deliveryInfo1);
		deliveryInfoRepo.save(deliveryInfo2);
		deliveryInfoRepo.save(deliveryInfo3);
		
		List<DeliveryInfo> infosInDb = deliveryInfoRepo.findByEventIdOrderByProcessedOn(eventId);
		
		Assertions.assertThat(infosInDb.size()).isEqualTo(2);
	}
	
	@Test
	public void testFindByEndpointId() {
		//GIVEN
		GenericEvent event = GenericEventRepositoryTest.createDirectMessageEvent();
		UUID eventId = eventRepo.save(event).getId();
		
		EmailEndpoint email1 = EmailEndpoint.builder().email("jancuzy@gmail.com").build();
		UUID endpointId = endpointRepo.persistEnpointIfNotExists(email1).getId();
		
		EmailEndpoint email2 = EmailEndpoint.builder().email("jancuzy2@gmail.com").build();
		UUID endpoint2Id = endpointRepo.persistEnpointIfNotExists(email2).getId();
		
		// AND GIVEN
		DeliveryInfo deliveryInfo1 = DeliveryInfo.builder()
				.endpointId(endpointId)
				.eventId(eventId)
				.status(DELIVERY_STATUS.SENT)
				.id(UUID.randomUUID())
				.build();
		
		DeliveryInfo deliveryInfo2 = DeliveryInfo.builder()
				.endpointId(endpointId)
				.eventId(eventId)
				.status(DELIVERY_STATUS.SENT)
				.id(UUID.randomUUID())
				.build();
		
		DeliveryInfo deliveryInfo3 = DeliveryInfo.builder()
				.endpointId(endpoint2Id)
				.eventId(eventId)
				.status(DELIVERY_STATUS.SENT)
				.id(UUID.randomUUID())
				.build();
		
		//WHEN
		deliveryInfoRepo.save(deliveryInfo1);
		deliveryInfoRepo.save(deliveryInfo2);
		deliveryInfoRepo.save(deliveryInfo3);
		
		//THEN
		List<DeliveryInfo> infosInDb = deliveryInfoRepo.findByEndpointIdOrderByProcessedOn(endpointId);
		
		Assertions.assertThat(infosInDb.size()).isEqualTo(2);
	}

}
