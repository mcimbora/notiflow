package com.obj.nc.koderia.mapper;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.obj.nc.SystemPropertyActiveProfileResolver;
import com.obj.nc.domain.message.Message;
import com.obj.nc.koderia.KoderiaFlowsApplication;
import com.obj.nc.koderia.dto.mailchimp.SendMessageWithTemplateDto;
import com.obj.nc.utils.JsonUtils;

@ActiveProfiles(value = "test", resolver = SystemPropertyActiveProfileResolver.class)
@JsonTest
@Import(MailchimpMessageMapperAggregateImplTestConfig.class)
@ContextConfiguration(classes = KoderiaFlowsApplication.class)
class MailchimpMessageMapperAggregateImplTest {

    public static final String MESSAGE_JSON_PATH = "mailchimp/aggregate_message.json";
    public static final String EXPECTED_DTO_JSON_PATH = "mailchimp/send_aggregate_message_dto.json";

    @Autowired
    @Qualifier(MailchimpMessageMapperAggregateImpl.COMPONENT_NAME)
    private MailchimpMessageMapperAggregateImpl mapper;

    @Test
    @Disabled
    void testMapWithTemplate() {
        // GIVEN
        Message inputMessage = JsonUtils.readObjectFromClassPathResource(MESSAGE_JSON_PATH, Message.class);

        SendMessageWithTemplateDto expectedSendMessageDto = JsonUtils.readObjectFromClassPathResource(EXPECTED_DTO_JSON_PATH, SendMessageWithTemplateDto.class);

        // WHEN
        SendMessageWithTemplateDto sendMessageDto = mapper.mapWithTemplate(inputMessage);

        // THEN
        MatcherAssert.assertThat(sendMessageDto, Matchers.equalTo(expectedSendMessageDto));
    }

}