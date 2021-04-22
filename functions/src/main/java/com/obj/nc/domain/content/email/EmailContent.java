package com.obj.nc.domain.content.email;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.obj.nc.domain.Attachement;
import com.obj.nc.domain.content.Content;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@JsonTypeName(EmailContent.JSON_TYPE_IDENTIFIER)
public class EmailContent extends Content {
	
	public final static String JSON_TYPE_IDENTIFIER = "EMAIL_MESSAGE_CONTENT";

	private String subject;
	
	@NonNull
	@EqualsAndHashCode.Include
	private String text;
	
	@EqualsAndHashCode.Include
	private String contentType=MediaType.TEXT_PLAIN_VALUE;

	@EqualsAndHashCode.Include
	private List<Attachement> attachments = new ArrayList<Attachement>();
	
	public static EmailContent createWithSubject(String subject, String text) {
		EmailContent emailContent = new EmailContent(text);
		emailContent.setSubject(subject);
		return emailContent;
	}
	
	@JsonIgnore
	public String getTextForAggregation() {
		if (MediaType.TEXT_PLAIN_VALUE.equals(contentType)) {
			return text;
		} else if (MediaType.TEXT_HTML_VALUE.equals(contentType)) {
			Document textAsHtml = Jsoup.parse(text);
			return textAsHtml.body().html();
		} else {
			throw new RuntimeException(String.format("Could not get content for aggregation for MediaType: %s", contentType));
		}
	}
	


}
