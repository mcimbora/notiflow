package com.obj.nc.functions.processors.senders.mailchimp.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.obj.nc.domain.content.mailchimp.MailchimpContent;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class MailchimpRenderTemplateRequest {
    
    @NotBlank private String key;
    @NotBlank @JsonProperty("template_name") private String templateName;
    @JsonProperty("template_content") private List<MailchimpTemplateContentDto> templateContent;
    @JsonProperty("merge_vars") private List<MailchimpMergeVariableDto> mergeVars = new ArrayList<>();
    
}
