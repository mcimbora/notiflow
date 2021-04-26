package com.obj.nc.koderia.dto;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LinkEventDataDto extends EventDataDto {

    @NotBlank
    private String id;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String siteName;

    @NotEmpty
    private List<String> images;

    @NotBlank
    private String url;

    @NotNull
    private List<String> favicons;

    @Override
    public String getMessageSubject() {
        return title;
    }

    @Override
    public String getMessageText() {
        return description;
    }

}
