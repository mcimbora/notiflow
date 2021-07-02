package com.obj.nc.flows.testmode.sms.funcitons.processors;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.obj.nc.aspects.DocumentProcessingInfo;
import com.obj.nc.domain.content.sms.SimpleTextContent;
import com.obj.nc.domain.message.SmsMessage;
import com.obj.nc.exceptions.PayloadValidationException;
import com.obj.nc.flows.testmode.sms.funcitons.sources.InMemorySmsSourceSupplier;
import com.obj.nc.functions.processors.ProcessorFunctionAdapter;
import com.obj.nc.functions.processors.senders.SmsSender;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnMissingBean(type = "SmsSender")
@RequiredArgsConstructor
@DocumentProcessingInfo("TestSMSSender")
public class InMemorySmsSender extends ProcessorFunctionAdapter<SmsMessage,SmsMessage> implements SmsSender  {
	
	private final InMemorySmsSourceSupplier reciever;

	
	@Override
	protected Optional<PayloadValidationException> checkPreCondition(SmsMessage payload) {
		if (!(payload.getBody() instanceof SimpleTextContent)) {
			throw new PayloadValidationException("TestModeSmsSender can only process SimpleTextContent content. Was " + payload.getBody() );
		}
		return Optional.empty();
	}
	
	@Override
	protected SmsMessage execute(SmsMessage smsMessage) {
				
		reciever.recieve(smsMessage);

		return smsMessage;
	}


}
