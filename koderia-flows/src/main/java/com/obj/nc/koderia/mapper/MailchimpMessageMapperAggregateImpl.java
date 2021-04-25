package com.obj.nc.koderia.mapper;

import static com.obj.nc.koderia.mapper.MailchimpMessageMapperAggregateImpl.COMPONENT_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.obj.nc.domain.message.Message;
import com.obj.nc.koderia.dto.EmitEventDto;
import com.obj.nc.koderia.dto.mailchimp.AttachmentDto;
import com.obj.nc.koderia.dto.mailchimp.MergeVarDto;

@Component(COMPONENT_NAME)
public class MailchimpMessageMapperAggregateImpl extends MailchimpMessageMapperImpl {

    public static final String COMPONENT_NAME = "mailchimpAggregateMessageMapper";

    @Override
    protected String mapSubject(Message message) {
        return mailchimpApiConfig.getTemplate().getAggregateSubject();
    }

    @Override
    protected List<MergeVarDto> mapGlobalMergeVars(Message message) {
//    	AggregatedEmailContent aggregateContent = (AggregatedEmailContent)message.getBody().getMessage();

        Map<String, List<Object>> globalMergeCategoryValues = new HashMap<>();
        Arrays.stream(EmitEventDto.Type.values())
                .forEach(type -> globalMergeCategoryValues.put(type.name(), new ArrayList<>()));

//        aggregateContent.getAggregateContent().stream()
//                .map(messageContent -> messageContent.getAttributeValueAs(ORIGINAL_EVENT_FIELD, EmitEventDto.class))
//                .forEach(originalEvent -> globalMergeCategoryValues.get(originalEvent.getType().name()).add(originalEvent.asMap()));

        return globalMergeCategoryValues.entrySet().stream().map(this::mapMergeVar).collect(Collectors.toList());
    }

    @Override
    protected List<AttachmentDto> mapAttachments(Message message) {
//    	AggregatedEmailContent aggregateContent = (AggregatedEmailContent)message.getBody().getMessage();
//    	
//        return aggregateContent.getAggregateContent().stream()
//                .flatMap(messageContent -> messageContent.getAttachments().stream().map(this::mapAttachment))
//                .collect(Collectors.toList());
        return new ArrayList<>();
    }

    @Override
    protected String getTemplateName(Message message) {
        return mailchimpApiConfig.getTemplate().getAggregateName();
    }

}
