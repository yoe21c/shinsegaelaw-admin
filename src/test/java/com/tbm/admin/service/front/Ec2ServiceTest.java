package com.tbm.admin.service.front;

import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.param.req.CreateEc2Request;
import com.tbm.admin.service.persist.AccountInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("local")
class Ec2ServiceTest {

    @Autowired
    private Ec2Service ec2Service;

    private AccountInfoService accountInfoService;

    private static String newInstanceId = "i-0672402e74337df24";

    @BeforeEach
    void setup() {
        accountInfoService = Mockito.mock(AccountInfoService.class);

        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setInstanceId(newInstanceId);

        given(accountInfoService.getAll()).willReturn(List.of(accountInfo));
        given(accountInfoService.getAccountInfoByInstanceId(newInstanceId)).willReturn(accountInfo);
    }

    @Test
    void changeInstance() {
        ec2Service.changeInstance(newInstanceId);
    }

    @Test
    void createInstance() {
        String instanceId = ec2Service.createInstance(CreateEc2Request.builder().name("aws-test").build());

        assertNotNull(instanceId);
        System.out.println("instanceId = " + instanceId);
    }

    @Test
    void allocateElasticIp() {
        ec2Service.allocateElasticIp(newInstanceId);
    }

    @Test
    void terminateInstance() {
        ec2Service.terminateInstance(newInstanceId);
    }

    @Test
    void getInstanceState() {
        String instanceState = ec2Service.getInstanceState("i-06406a64900cfa6d6");
        System.out.println("instanceState = " + instanceState);
    }
}