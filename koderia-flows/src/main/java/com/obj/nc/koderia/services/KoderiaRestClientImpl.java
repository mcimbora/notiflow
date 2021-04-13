package com.obj.nc.koderia.services;

import com.obj.nc.functions.processors.KoderiaRecipientsConfigProperties;
import com.obj.nc.domain.endpoints.RecievingEndpoint;
import com.obj.nc.koderia.dto.RecipientDto;
import com.obj.nc.koderia.dto.RecipientsQueryDto;
import com.obj.nc.koderia.mapper.RecipientMapper;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class KoderiaRestClientImpl implements KoderiaClient {

    public static final String RECIPIENTS_PATH = "/recipients";

    private final RecipientMapper recipientMapper;

    private final RestTemplate restTemplate;

    public KoderiaRestClientImpl(ResponseErrorHandler responseErrorHandler, RecipientMapper recipientMapper,
                                 KoderiaRecipientsConfigProperties koderiaRecipientsConfigProperties, RestTemplateBuilder restTemplateBuilder) {
        this.recipientMapper = recipientMapper;
        this.restTemplate = restTemplateBuilder
                .rootUri(koderiaRecipientsConfigProperties.getKoderiaApiUrl())
                .additionalInterceptors(
                        (httpRequest, bytes, clientHttpRequestExecution) -> {
                            httpRequest.getHeaders().add(HttpHeaders.AUTHORIZATION,
                                    "Bearer " + koderiaRecipientsConfigProperties.getKoderiaApiToken());
                            return clientHttpRequestExecution.execute(httpRequest, bytes);
                        })
                .errorHandler(responseErrorHandler)
                .build();
    }

    @Override
    public List<RecievingEndpoint> findReceivingEndpoints(RecipientsQueryDto query) {
        ResponseEntity<RecipientDto[]> responseEntity = restTemplate.postForEntity(RECIPIENTS_PATH, query, RecipientDto[].class);
        RecipientDto[] responseBody = responseEntity.getBody();

        if (responseBody == null) {
            throw new RestClientException("Response body is null");
        }

        return Arrays.stream(responseBody)
                .map(recipientMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

}
