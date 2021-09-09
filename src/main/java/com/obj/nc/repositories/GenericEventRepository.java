package com.obj.nc.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.obj.nc.domain.event.GenericEventWithStats;
import com.obj.nc.repositories.mappers.EventStatsRowMapper;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.obj.nc.domain.event.GenericEvent;
import com.obj.nc.domain.refIntegrity.EntityExistenceChecker;
import org.springframework.data.repository.query.Param;

public interface GenericEventRepository extends PagingAndSortingRepository<GenericEvent, UUID>, EntityExistenceChecker<UUID> {
	
	GenericEvent findFirstByTimeConsumedIsNullOrderByTimeCreatedAsc();
	
	GenericEvent findByExternalId(String externalId);
	
	@Query(
			value = "select " +
					"	event.*, " +
					"	count(distinct event.id) as events_count, " +
					"	count(distinct intent.id) as intents_count, " +
					"	count(distinct message.id) as messages_count, " +
					"	count(distinct message.endpoint_id) as endpoints_count, " +
					"	count(distinct di.id) filter(where di.status = 'SENT') as messages_sent_count, " +
					"	count(distinct di.id) filter(where di.status = 'READ') as messages_read_count, " +
					"	count(distinct di.id) filter(where di.status = 'FAILED') as messages_failed_count " +
					"from " +
					"	nc_event event " +
					"left join " +
					"	nc_intent intent on event.id = any ( intent.previous_event_ids ) " +
					"left join ( " +
					"	select msg.*, endpoint_id " +
					"	from nc_message msg " +
					"	cross join unnest(msg.endpoint_ids) as endpoint_id " +
					") message on event.id = any ( message.previous_event_ids ) " +
					"left join " +
					"	nc_delivery_info di on di.event_id = event.id " +
					"where " +
					"	event.time_consumed between (:consumedFrom) and (:consumedTo) " +
					"and " +
					"	(:eventId)::uuid is null or event.id = (:eventId)::uuid " +
					"group by " +
					"	event.id, " +
					"	event.flow_id, " +
					"	event.external_id, " +
					"	event.payload_json, " +
					"	event.time_created, " +
					"	event.time_consumed, " +
					"	event.payload_type " +
					"offset :pageNum rows fetch next :pageSize rows only", 
			rowMapperClass = EventStatsRowMapper.class)
	List<GenericEventWithStats> findAllEventsWithStats(@Param("consumedFrom") Instant consumedFrom,
													   @Param("consumedTo") Instant consumedTo,
													   @Param("eventId") UUID eventId,
													   @Param("pageNum") long offset,
													   @Param("pageSize") int pageSize);
	
	@Query(
			value = "select " +
					"	count(event.id) " +
					"from " +
					"	nc_event event " +
					"where " +
					"	event.time_consumed between (:consumedFrom) and (:consumedTo) " +
					"and " +
					"	(:eventId)::uuid is null or event.id = (:eventId)::uuid")
	long countAllEventsWithStats(@Param("consumedFrom") Instant consumedFrom,
								 @Param("consumedTo") Instant consumedTo,
								 @Param("eventId") UUID eventId);

}
