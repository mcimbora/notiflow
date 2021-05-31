package com.obj.nc.functions.processors.eventValidator;

import com.obj.nc.exceptions.PayloadValidationException;
import com.obj.nc.functions.processors.ProcessorFunctionAdapter;
import com.obj.nc.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GenericEventValidator extends ProcessorFunctionAdapter<String, String> {
    
    @Override
    protected Optional<PayloadValidationException> checkPreCondition(String payload) {
        Optional<String> jsonProblems = JsonUtils.checkValidAndGetError(payload);
        return jsonProblems.map(PayloadValidationException::new);
    }
    
    @Override
    protected String execute(String payload) {
        return payload;
    }
    
}
