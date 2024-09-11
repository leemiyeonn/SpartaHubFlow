package com.sparta.company.client;

import com.sparta.company.domain.dto.HubDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubClient {
    @GetMapping("/api/v1/hubs/{hubId}")
    HubDto.Response getHubById(@PathVariable("hubId") UUID hubId);
}
