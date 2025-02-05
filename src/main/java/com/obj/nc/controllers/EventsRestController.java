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

package com.obj.nc.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.obj.nc.domain.dto.GenericEventTableViewDto;
import com.obj.nc.domain.event.EventRecieverResponce;
import com.obj.nc.domain.event.GenericEvent;
import com.obj.nc.exceptions.PayloadValidationException;
import com.obj.nc.functions.processors.eventValidator.GenericEventJsonSchemaValidator;
import com.obj.nc.functions.processors.eventValidator.SimpleJsonValidator;
import com.obj.nc.functions.sink.inputPersister.GenericEventPersister;
import com.obj.nc.repositories.GenericEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Validated
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventsRestController {

	private final GenericEventPersister persister;
	private final SimpleJsonValidator simpleJsonValidator;
	private final GenericEventJsonSchemaValidator jsonSchemaValidator;
	private final GenericEventRepository eventsRepository;
	
	@PostMapping( consumes="application/json", produces="application/json")
    public EventRecieverResponce persistGenericEvent(
    		@RequestBody(required = true) String eventJsonString, 
    		@RequestParam(value = "flowId", required = false) String flowId,
    		@RequestParam(value = "externalId", required = false) String externalId,
			@RequestParam(value = "payloadType", required = false) String payloadType) {
		
		JsonNode eventJson = simpleJsonValidator.apply(eventJsonString);
		
		GenericEvent event = GenericEvent.from(eventJson);
    	event.overrideFlowIdIfApplicable(flowId);
    	event.overrideExternalIdIfApplicable(externalId);
    	event.overridePayloadTypeIfApplicable(payloadType);
		
		if (payloadType != null) {
			event = jsonSchemaValidator.apply(event);
		}

    	try {
    		persister.accept(event);
    	} catch (DbActionExecutionException e) {
    		if (DuplicateKeyException.class.equals(e.getCause().getClass())) {
    			throw new PayloadValidationException("Duplicate external ID detected. Payload rejected: " + eventJson);
    		}
    	}

    	return EventRecieverResponce.from(event.getId());
    }
	
	@GetMapping(produces = APPLICATION_JSON_VALUE)
	public Page<GenericEventTableViewDto> findAllEvents(@RequestParam(value = "consumedFrom", required = false, defaultValue = "2000-01-01T12:00:00Z") 
															@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant consumedFrom,
														@RequestParam(value = "consumedTo", required = false, defaultValue = "9999-01-01T12:00:00Z") 
															@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant consumedTo,
														@RequestParam(value = "eventId", required = false) String eventId,
														Pageable pageable) {
		UUID eventUUID = eventId == null ? null : UUID.fromString(eventId);
		
		List<GenericEventTableViewDto> events = eventsRepository
				.findAllEventsWithStats(consumedFrom, consumedTo, eventUUID, pageable.getOffset(), pageable.getPageSize())
				.stream()
				.map(GenericEventTableViewDto::from)
				.collect(Collectors.toList());
		
		long eventsTotalCount = eventsRepository.countAllEventsWithStats(consumedFrom, consumedTo, eventUUID);
		return new PageImpl<>(events, pageable, eventsTotalCount);
	}
	
	@GetMapping(value = "/{eventId}", produces = APPLICATION_JSON_VALUE)
	public GenericEvent findEvent(@PathVariable("eventId") String eventId) {
		return eventsRepository
				.findById(UUID.fromString(eventId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
