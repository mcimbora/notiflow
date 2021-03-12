package com.obj.nc.testmode;

import static com.obj.nc.testmode.config.TestModeFlowConfig.TEST_MODE_GREEN_MAIL_SOURCE_BEAN_NAME;

import java.util.List;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.obj.nc.BaseIntegrationTest;
import com.obj.nc.SystemPropertyActiveProfileResolver;
import com.obj.nc.domain.Messages;
import com.obj.nc.domain.message.AggregatedEmail;
import com.obj.nc.domain.message.Message;
import com.obj.nc.functions.processors.senders.EmailSender;
import com.obj.nc.testmode.config.TestModeBeansConfig;
import com.obj.nc.testmode.config.TestModeGreenMailProperties;
import com.obj.nc.testmode.config.TestModeProperties;
import com.obj.nc.testmode.functions.sources.GreenMailReceiverSourceSupplier;
import com.obj.nc.utils.JsonUtils;

@ActiveProfiles(value = { "test"}, resolver = SystemPropertyActiveProfileResolver.class)
@SpringIntegrationTest(noAutoStartup = TEST_MODE_GREEN_MAIL_SOURCE_BEAN_NAME)
@SpringBootTest(properties = {
		"nc.flows.test-mode.enabled=true",
		"nc.flows.test-mode.recipients=cuzy@objectify.sk"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS) //Because of correct disposal of green mail used for test mode
public class TestmodeIntegrationTests extends BaseIntegrationTest {

	@Qualifier(TestModeBeansConfig.TEST_MODE_GREEN_MAIL_BEAN_NAME)
	@Autowired private GreenMail testModeEmailsReciver;

	@Autowired private EmailSender emailSenderSinkProcessingFunction;
	@Autowired private GreenMailReceiverSourceSupplier greenMailReceiverSourceSupplier;
	@Autowired private TestModeGreenMailProperties gmProps;
	@Autowired private TestModeProperties props;
	@Autowired private MockIntegrationContext mockIntegrationContext;
	@Autowired private JavaMailSenderImpl mailSender;

    @BeforeEach
    void cleanGreenMailMailBoxes() throws FolderException {
    	testModeEmailsReciver.purgeEmailFromAllMailboxes();
    }

    @Test
    void testTestmode() throws MessagingException {
    	//normaly, we would send all test mail to standrdTestGMServer. When testMode profile is activated, aditional testModeEmailsReciver
    	//is created which is a different instannce. In this mode testModeEmailsReciver will catch all emails normaly send to standardTestGMServer
    	//and thus in production will catch all emails send to standard SMTP server configured
    	//PRE-CONDITION
    	Assertions.assertThat(greenMail).isNotEqualTo(testModeEmailsReciver);
    	Assertions.assertThat(greenMail.getSmtp().getPort()).isNotEqualTo(testModeEmailsReciver.getSmtp().getPort());

    	//PRE-CONDITION
    	//Any injected JavaMailSenderImpl has to be configured to send email to testModeEmailReviver. Not to standard SMTP
    	Assertions.assertThat(mailSender.getHost()).isEqualTo("localhost");
    	Assertions.assertThat(mailSender.getPort()).isEqualTo(gmProps.getSmtpPort());


        // GIVEN
        Message message1 = JsonUtils.readObjectFromClassPathResource("messages/testmode/aggregate_input_message1.json", Message.class);
        Message message2 = JsonUtils.readObjectFromClassPathResource("messages/testmode/aggregate_input_message2.json", Message.class);
        Message message3 = JsonUtils.readObjectFromClassPathResource("messages/testmode/aggregate_input_message3.json", Message.class);

        //WHEN
        emailSenderSinkProcessingFunction.apply(message1);
        emailSenderSinkProcessingFunction.apply(message2);
        emailSenderSinkProcessingFunction.apply(message3);

        testModeEmailsReciver.waitForIncomingEmail(3);

        MimeMessage[] inputMimeMessages = testModeEmailsReciver.getReceivedMessages();
        Assertions.assertThat(inputMimeMessages.length).isEqualTo(3);

        Messages messagesWrapped = greenMailReceiverSourceSupplier.get();
        List<Message> messages = messagesWrapped.getMessages();

        // WHEN
        MessageSource<Messages> messageSource = () -> new GenericMessage<>(messagesWrapped);
        mockIntegrationContext.substituteMessageSourceFor(TEST_MODE_GREEN_MAIL_SOURCE_BEAN_NAME, messageSource);

        // THEN agregeted mail recieved by standardn green mail used by test and thus in producton standard SMTP server
        boolean success = greenMail.waitForIncomingEmail(1);
        Assertions.assertThat( success ).isEqualTo( true );

        MimeMessage[] outputMimeMessages = greenMail.getReceivedMessages();
        Assertions.assertThat( outputMimeMessages.length ).isEqualTo(1);

        String recipient = props.getRecipients().iterator().next();
        outputMimeMessages = greenMail.getReceivedMessagesForDomain(recipient);
        Assertions.assertThat( outputMimeMessages.length ).isEqualTo(1);

        AggregatedEmail aggregated1 = message1.getContentTyped();
        AggregatedEmail aggregated2 = message2.getContentTyped();
        AggregatedEmail aggregated3 = message3.getContentTyped();
        Assertions.assertThat(outputMimeMessages[0].getSubject()).contains(
        		aggregated1.getAggregateContent().get(0).getSubject(),
        		aggregated2.getAggregateContent().get(0).getSubject(),
        		aggregated3.getAggregateContent().get(0).getSubject());
        Assertions.assertThat(GreenMailUtil.getBody(outputMimeMessages[0])).contains(
        		aggregated1.getAggregateContent().get(0).getText(),
        		aggregated2.getAggregateContent().get(0).getText(),
        		aggregated3.getAggregateContent().get(0).getText());
    }

    @TestConfiguration
    public static class TestModeTestConfiguration {

        @Bean
        public Trigger sourceTrigger() {
            return new OnlyOnceTrigger();
        }

    }

}
