package com.obj.nc.koderia.dto;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BlogCategoryDto {

    @NotBlank
    private String id;

    @NotBlank
    private String slug;

    @NotBlank
    private String name;

    @JsonProperty("color_a")
    private String colorA;

    @JsonProperty("color_b")
    private String colorB;

}
