package com.obj.nc.functions.processors.messageAggregator;

import com.obj.nc.aspects.DocumentProcessingInfo;
import com.obj.nc.domain.Messages;
import com.obj.nc.domain.message.Message;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Log4j2
public class MessageAggregatorExecution implements Function<Messages, Message> {

	@DocumentProcessingInfo("AggregateMessages")
	@Override
	public Message apply(Messages messages) {
		Message outputMessage = new Message();
		outputMessage.getBody().setRecievingEndpoints(messages.getMessages().get(0).getBody().getRecievingEndpoints());
		outputMessage.getBody().setDeliveryOptions(messages.getMessages().get(0).getBody().getDeliveryOptions());

		Message aggregateMessage = messages.getMessages().stream()
				.reduce(Message::merge)
				.orElseThrow(() -> new IllegalStateException("Failed to Aggregate messages"));

		return outputMessage.merge(aggregateMessage);
	}

}