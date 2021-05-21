package com.obj.nc.functions.processors.messageTemplating;

import java.util.Locale;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import com.obj.nc.aspects.DocumentProcessingInfo;
import com.obj.nc.domain.content.Content;
import com.obj.nc.domain.content.TemplateWithModelContent;
import com.obj.nc.domain.content.email.EmailContent;
import com.obj.nc.domain.content.email.TemplateWithModelEmailContent;
import com.obj.nc.domain.message.Message;
import com.obj.nc.exceptions.PayloadValidationException;
import com.obj.nc.functions.processors.messageTemplating.config.ThymeleafConfiguration;

@Component
@DocumentProcessingInfo("EmailFormatter")
public class EmailTemplateFormatter extends BaseTemplateFormatter<EmailContent> {

	public EmailTemplateFormatter(TemplateEngine templateEngine, ThymeleafConfiguration config) {
		super(templateEngine, config);
	}

	@Override
	public Optional<PayloadValidationException> checkPreCondition(Message<TemplateWithModelContent<?>> message) {
		Content content = message.getBody();
		
		if (!(content instanceof  TemplateWithModelEmailContent)) {
			return Optional.of(new PayloadValidationException("EmailTemplateFormatter cannot format message because its content is not of type TemplateWithModelEmailContent. Instead is " +  content.getClass().getSimpleName()));
		}		

		return Optional.empty();
	}

	protected Message<EmailContent> createMessageWithFormattedContent(String formatedContent, Locale locale,  Message<TemplateWithModelContent<?>> payload) {		
		Message<EmailContent> htmlMessage = Message.createAsEmail();

		EmailContent emailContent = htmlMessage.getBody();
		emailContent.setContentType(MediaType.TEXT_HTML_VALUE);
		
		TemplateWithModelEmailContent<?> emailFromTemplate = (TemplateWithModelEmailContent<?>)payload.getBody();
		emailContent.setSubject(emailFromTemplate.getSubjectLocalised(locale));
		emailContent.setText(formatedContent);
		
		return htmlMessage;
	}

}
