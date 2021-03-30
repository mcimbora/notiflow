package com.obj.nc.functions.sources.eventGenerator;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.obj.nc.domain.notifIntent.NotificationIntent;

@Component
@Profile({"dev"})
@EnableScheduling
public class EventGeneratorScheduler {

    @Autowired
    private EventGeneratorMicroService sourceMicroService;

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void generateEventAndAddToFlux() {
        NotificationIntent notificationIntent = sourceMicroService.getSourceSupplier().get();
        sourceMicroService.onNext(notificationIntent);
    }

}
