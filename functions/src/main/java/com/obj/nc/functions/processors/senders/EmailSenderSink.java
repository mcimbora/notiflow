package com.obj.nc.functions.processors.senders;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.obj.nc.aspects.DocumentProcessingInfo;
import com.obj.nc.domain.Attachement;
import com.obj.nc.domain.endpoints.EmailEndpoint;
import com.obj.nc.domain.endpoints.RecievingEndpoint;
import com.obj.nc.domain.message.Message;
import com.obj.nc.domain.message.MessageContent;
import com.obj.nc.exceptions.ProcessingException;
import com.obj.nc.exceptions.PayloadValidationException;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

@Configuration
public class EmailSenderSink {

	@Autowired
	private SendMessage fn;

	@Bean
	public Consumer<Flux<Message>> sendMessage() {
		return payloads -> payloads.doOnNext(payload -> fn.apply(payload));
	}

	@Component
	public static class SendMessage implements Function<Message, Message> {
		
		@Autowired
	    private JavaMailSender emailSender;


		@DocumentProcessingInfo("SendEmail")
		@Override
		public Message apply(Message payload) {
			List<RecievingEndpoint> to = payload.getBody().getRecievingEndpoints();
			if (to.size()!=1) {
				throw new PayloadValidationException("Email sender can send to only one recipient. Found more: " + to);
			}
			RecievingEndpoint endpoint = to.get(0);
			if (!(endpoint instanceof EmailEndpoint)) {
				throw new PayloadValidationException("Email sender can send to Email endpoints only. Found " + endpoint);
			}
			EmailEndpoint toEmail = (EmailEndpoint)endpoint;
			
			MessageContent msg = payload.getBody().getMessage();
	
			try {
				MimeMessage message = emailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(message, true);
		
				helper.setFrom("no-reply@objectify.sk");
		
				helper.setTo(toEmail.getEmail());
				
				helper.setSubject(msg.getSubject());
				helper.setText(msg.getText());
		
				for (Attachement attachement: payload.getBody().getAttachments()) {
					FileSystemResource file = new FileSystemResource(new File(attachement.getFileURI()));
					helper.addAttachment(attachement.getName(), file);
				}
		
				emailSender.send(message);
				
				return payload;
			} catch (MessagingException e) {
				throw new ProcessingException(EmailSenderSink.class, e);
			}
		}

	}

}
