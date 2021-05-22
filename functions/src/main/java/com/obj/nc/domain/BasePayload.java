package com.obj.nc.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.obj.nc.domain.endpoints.RecievingEndpoint;
import com.obj.nc.domain.headers.HasHeader;
import com.obj.nc.domain.headers.Header;
import com.obj.nc.domain.headers.ProcessingInfo;
import com.obj.nc.domain.message.Message;
import com.obj.nc.domain.notifIntent.NotificationIntent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ 
	@Type(value = NotificationIntent.class, name = NotificationIntent.JSON_TYPE_IDENTIFIER),
	@Type(value = Message.class, name = Message.JSON_TYPE_IDENTIFIER) })
@ToString(callSuper = true)
@Log4j2
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class BasePayload<BODY_TYPE> extends BaseJSONObject implements HasHeader, HasRecievingEndpoints, HasEventIds, HasProcessingInfo, Persistable<UUID> {
	
	@Id
	@EqualsAndHashCode.Include
	private UUID id = UUID.randomUUID();
	@CreatedDate
	private Instant timeCreated;
	@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
	protected Header header = new Header();
	
	@Column("payload_json")
	protected BODY_TYPE body;
	
	//Ak je body sucastou message tak recievingEndpoints.size() = 1
	//move to content
	private List<? extends RecievingEndpoint> recievingEndpoints = new ArrayList<RecievingEndpoint>();

	//move to ?? (only for notification intent?)
//	private DeliveryOptions deliveryOptions = new DeliveryOptions();	
	
	public BasePayload<BODY_TYPE> addRecievingEndpoints(RecievingEndpoint ... r) {
		((List<RecievingEndpoint>)this.recievingEndpoints).addAll(Arrays.asList(r));
		return this;
	}
	
	@Override
	@Transient
	public List<? extends RecievingEndpoint> getRecievingEndpoints() {
		return recievingEndpoints;
	}
	
	@JsonIgnore
	@Transient
	public ProcessingInfo getProcessingInfo() {
		return getHeader().getProcessingInfo();
	}
	
	@Override
	@JsonIgnore
	@Transient
	public List<UUID> getEventIds() {
		return getHeader().getEventIds();
	}

	@Transient
	public abstract String getPayloadTypeName();
	
//	@JsonIgnore
//	@Transient
//	public <T> T getContentTyped() {
//		return (T)getBody().getMessage();
//	}
	
	@Override
	@JsonIgnore
	@Transient
	public boolean isNew() {
		return timeCreated == null;
	}

}
