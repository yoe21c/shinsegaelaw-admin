package com.shinsegaelaw.admin.controller;

import com.shinsegaelaw.admin.model.param.req.CreateEc2Request;
import com.shinsegaelaw.admin.model.param.res.Ec2Instance;
import com.shinsegaelaw.admin.service.front.Ec2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public-api/v1/ec2")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_superadmin')")
public class Ec2Api {

    private final Ec2Service ec2Service;

    @GetMapping("")
    public List<Ec2Instance> getInstances() {
        return ec2Service.getAllInstances();
    }

    @GetMapping("/start/{instanceId}")
    public void startInstance(@PathVariable String instanceId) {
        ec2Service.startInstance(instanceId);
    }

    @GetMapping("/stop/{instanceId}")
    @PreAuthorize("hasRole('ROLE_superadmin')")
    public void stopInstance(@PathVariable String instanceId) {
        ec2Service.stopInstance(instanceId);
    }

    @GetMapping("/start/all")
    public void startAllInstances() {
        ec2Service.startAllInstances();
    }

    @GetMapping("/stop/all")
    @PreAuthorize("hasRole('ROLE_superadmin')")
    public void stopAllInstances() {
        ec2Service.stopAllInstances();
    }

    @GetMapping("/change-instance/{instanceId}")
    @PreAuthorize("hasRole('ROLE_superadmin')")
    public void changeInstance(@PathVariable String instanceId) {
        ec2Service.changeInstance(instanceId);
    }

    @PostMapping("/create")
    public String createInstance(@RequestBody CreateEc2Request request) {
        return ec2Service.createInstance(request);
    }

    @GetMapping("/allocate-eip/{instanceId}")
    public void allocateElasticIp(@PathVariable String instanceId) {
        ec2Service.allocateElasticIp(instanceId);
    }

    @DeleteMapping("/delete/{instanceId}")
    @PreAuthorize("hasRole('ROLE_superadmin')")
    public void deleteInstance(@PathVariable String instanceId) {
        ec2Service.terminateInstance(instanceId);
    }
}
