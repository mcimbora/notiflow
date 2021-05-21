package com.obj.nc.functions.processors.senders;

import java.util.function.Function;

import com.obj.nc.domain.content.sms.SimpleTextContent;
import com.obj.nc.domain.message.Message;

public interface SmsSender extends Function<Message<SimpleTextContent>, Message<SimpleTextContent>>{

}
