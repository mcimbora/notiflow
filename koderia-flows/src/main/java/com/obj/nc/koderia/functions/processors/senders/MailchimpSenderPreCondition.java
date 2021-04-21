package com.obj.nc.koderia.functions.processors.senders;

import com.obj.nc.domain.content.Content;
import com.obj.nc.domain.content.email.EmailContent;
import com.obj.nc.domain.endpoints.EmailEndpoint;
import com.obj.nc.domain.message.Message;
import com.obj.nc.exceptions.PayloadValidationException;
import com.obj.nc.functions.PreCondition;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.obj.nc.koderia.functions.processors.KoderiaEventConverterExecution.ORIGINAL_EVENT_FIELD;

import java.util.Collections;
import java.util.Optional;

@Component
public class MailchimpSenderPreCondition implements PreCondition<Message> {

	@Override
	public Optional<PayloadValidationException> apply(Message message) {
		if (message == null) {
			return Optional.of(new PayloadValidationException("Message must not be null"));
		}

		Content messageContent = message.getBody().getMessage();
		if (messageContent instanceof AggregatedEmailContent) {
			AggregatedEmailContent aggregatedContent = (AggregatedEmailContent)messageContent;
			
			for (EmailContent messageContentPart : aggregatedContent.getAggregateContent()) {
				
				Optional<PayloadValidationException> exception = checkMessageContent(messageContentPart);
				if (exception.isPresent()) {
					return exception;
				}
			}
		} else {
			Optional<PayloadValidationException> exception = checkMessageContent(message.getContentTyped());
			if (exception.isPresent()) {
				return exception;
			}
		}

		return checkReceivingEndpoints(message);
	}

	private Optional<PayloadValidationException> checkMessageContent(EmailContent messageContent) {
		if (!messageContent.containsAttributes(Collections.singletonList(ORIGINAL_EVENT_FIELD))) {
			return Optional.of(new PayloadValidationException(String.format("Message must contain attribute: %s", ORIGINAL_EVENT_FIELD)));
		}

		if (!messageContent.containsNestedAttributes(Collections.singletonList("type"), ORIGINAL_EVENT_FIELD)) {
			return Optional.of(new PayloadValidationException(String.format("Message must contain attribute: %s.type", ORIGINAL_EVENT_FIELD)));
		}

		if (!StringUtils.hasText(messageContent.getSubject())) {
			return Optional.of(new PayloadValidationException("Message must contain Subject with at least 1 non-whitespace character"));
		}

		return Optional.empty();
	}

	private Optional<PayloadValidationException> checkReceivingEndpoints(Message message) {
		boolean hasNoneOrTooMuchEndpoints = message.getBody().getRecievingEndpoints().size() != 1;
		boolean containsNonEmailEndpoint = message.getBody().getRecievingEndpoints().stream()
				.anyMatch(endpoint -> !EmailEndpoint.JSON_TYPE_IDENTIFIER.equals(endpoint.getEndpointType()));

		if (hasNoneOrTooMuchEndpoints || containsNonEmailEndpoint) {
			return Optional.of(new PayloadValidationException(String.format("Mailchimp can only send message %s to 1 EmailContent endpoint", message)));
		}

		return Optional.empty();
	}

}