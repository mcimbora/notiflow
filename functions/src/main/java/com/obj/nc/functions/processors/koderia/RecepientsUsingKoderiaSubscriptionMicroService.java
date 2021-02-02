package com.obj.nc.functions.processors.koderia;

import com.obj.nc.domain.event.Event;
import com.obj.nc.functions.processors.ProcessorMicroService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@Log4j2
public class RecepientsUsingKoderiaSubscriptionMicroService extends ProcessorMicroService<Event, Event, RecepientsUsingKoderiaSubscriptionProcessingFunction>{

	@Autowired
	private RecepientsUsingKoderiaSubscriptionProcessingFunction fn;

	@Bean
	public Function<Flux<Event>, Flux<Event>> resolveRecipients() {
		return super.executeProccessingService();
	}

	@Override
	public RecepientsUsingKoderiaSubscriptionProcessingFunction getProccessingFuction() {
		return fn;
	}

}
