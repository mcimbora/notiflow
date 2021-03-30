package com.obj.nc.domain.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.obj.nc.domain.BasePayload;
import com.obj.nc.domain.content.email.AggregatedEmailContent;
import com.obj.nc.domain.content.email.EmailContent;
import com.obj.nc.domain.endpoints.DeliveryOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = false)
public class Message extends BasePayload {
	
	public static final String JSON_TYPE_IDENTIFIER = "MESSAGE";
	
	public static Message createAsEmail() {
		Message msg = new Message();
		msg.getBody().setMessage(new EmailContent());
		return msg;
	}
	
	public static Message createAsAggregatedEmail() {
		Message msg = new Message();
		msg.getBody().setMessage(new AggregatedEmailContent());
		return msg;
	}

	@Override
	@JsonIgnore
	public String getPayloadTypeName() {
		return JSON_TYPE_IDENTIFIER;
	}

	@JsonIgnore
	public boolean isAggregateMessage() {
		return this.getBody().getMessage() instanceof AggregatedEmailContent;
	}

}
