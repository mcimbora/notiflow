package com.obj.nc.osk.functions.sources;

import com.obj.nc.functions.sources.BaseRestReceiverSourceSupplier;
import com.obj.nc.osk.dto.OskSendSmsRequestDto;
import com.obj.nc.services.BaseRestReceiver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "nc.flows.test-mode.enabled", havingValue = "true")
public class OskSmsRestReceiverSourceSupplier extends BaseRestReceiverSourceSupplier<OskSendSmsRequestDto> {
    
    public OskSmsRestReceiverSourceSupplier(BaseRestReceiver<OskSendSmsRequestDto, ?> restReceiver) {
        super(restReceiver);
    }
    
}
