package com.tbm.admin.controller.api;

import com.tbm.admin.model.param.IpAddress;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.AccountInfoFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/public-api/v1")
@RequiredArgsConstructor
public class AggregatorApi {

    private final AccountInfoFrontService accountInfoFrontService;

    @PostMapping("/aggregator")
    public RestResult aggregator(@RequestBody IpAddress ipAddress) {

        log.info("/aggregator ipAddress = {}", ipAddress);

        log.error("This function is not implemented yet. !!!! ");
//        return accountInfoFrontService.mappingAccountInfo(ipAddress.getIp());

        return RestResult.success();
    }
}
