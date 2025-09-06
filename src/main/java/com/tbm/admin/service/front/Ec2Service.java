package com.tbm.admin.service.front;

import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.enums.Ec2State;
import com.tbm.admin.model.param.req.CreateEc2Request;
import com.tbm.admin.model.param.res.Ec2Instance;
import com.tbm.admin.model.props.AwsEc2Props;
import com.tbm.admin.service.persist.AccountInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tbm.admin.utils.DateUtils.convertToInstantToLocalDateTime;
import static com.tbm.admin.utils.DateUtils.convertToLocalDateTimeToInstant;


@Slf4j
@Service
public class Ec2Service {

    private final Ec2Client ec2Client;
    private final CloudWatchClient cloudWatchClient;
    private final AwsEc2Props awsEc2Props;

    private final AccountInfoService accountInfoService;

    private static final String SCRAP_INSTANCE_NAME_PREFIX = "agent-scrap";

    public Ec2Service(Ec2Client ec2Client, CloudWatchClient cloudWatchClient, AccountInfoService accountInfoService, AwsEc2Props awsEc2Props) {
        this.ec2Client = ec2Client;
        this.cloudWatchClient = cloudWatchClient;
        this.accountInfoService = accountInfoService;
        this.awsEc2Props = awsEc2Props;
    }

    public List<Ec2Instance> getAllInstances() {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .build();

        ArrayList<Ec2Instance> result = new ArrayList<>();
        for (Reservation reservation : ec2Client.describeInstances(request).reservations()) {
            for (Instance instance : reservation.instances()) {
                String instanceId = instance.instanceId();
                InstanceState state = instance.state();

                String name = null;
                Optional<Tag> tagOptional = instance.tags().stream().filter(tag -> tag.key().equals("Name")).findFirst();
                if(tagOptional.isPresent()) name = tagOptional.get().value();

                Ec2Instance ec2Instance = Ec2Instance.builder()
                        .id(instanceId)
                        .name(name)
                        .ipAddress(instance.publicIpAddress())
                        .state(Ec2State.of(state.nameAsString()))
                        .launchTime(convertToInstantToLocalDateTime(instance.launchTime()))
                        .build();

                // 정지되지 않은 EC2만 CPU 사용량 조회
                if(!state.name().equals(InstanceStateName.STOPPED)){
                    // cpu 사용량이 없을 경우 null
                    Datapoint datapoint = getCpuUsage(instanceId);
                    if(datapoint != null) {
                        ec2Instance.setLastCpuUsage(
                                Ec2Instance.CpuUsage.builder()
                                        .maximum(datapoint.maximum())
                                        .unit(datapoint.unitAsString())
                                        .timestamp(convertToInstantToLocalDateTime(datapoint.timestamp()))
                                        .build()
                        );
                    }
                }

                result.add(ec2Instance);
            }
        }
        return result;
    }

    private Datapoint getCpuUsage(String instanceId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(10);
        GetMetricStatisticsRequest getMetricStatisticsRequest = GetMetricStatisticsRequest.builder()
                .namespace("AWS/EC2")
                .metricName("CPUUtilization")
                .period(3600)
                .statistics(Statistic.MAXIMUM)
                .dimensions(
                        Dimension.builder()
                                .name("InstanceId")
                                .value(instanceId)
                                .build()
                )
                .startTime(convertToLocalDateTimeToInstant(startTime))
                .endTime(convertToLocalDateTimeToInstant(endTime))
                .build();

        GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(getMetricStatisticsRequest);
        List<Datapoint> datapoints = response.datapoints();
        if(datapoints.isEmpty()) return null;
        return datapoints.getLast();
    }

    public void startInstance(String instanceId) {
        StartInstancesRequest request = StartInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        ec2Client.startInstances(request);
    }

    public void stopInstance(String instanceId) {
        StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        ec2Client.stopInstances(request);
    }

    public void startAllInstances() {
        getAllInstances().stream()
                .filter(ec2Instance -> ec2Instance.getName().startsWith(SCRAP_INSTANCE_NAME_PREFIX))
                .forEach(ec2Instance -> startInstance(ec2Instance.getId()));
    }

    public void stopAllInstances() {
        getAllInstances().stream()
                .filter(ec2Instance -> ec2Instance.getName().startsWith(SCRAP_INSTANCE_NAME_PREFIX))
                .forEach(ec2Instance -> stopInstance(ec2Instance.getId()));
    }

    public void changeAllInstances() {
        List<Ec2Instance> instances = getAllInstances();
        List<Ec2Instance> filteredInstances = filterInstances(instances);

        Map<String, AccountInfo> accountInfoMap = accountInfoService.getAll().stream()
                .filter(accountInfo -> accountInfo.getId().startsWith(SCRAP_INSTANCE_NAME_PREFIX))
                .collect(Collectors.toMap(AccountInfo::getId, Function.identity()));

        ArrayList<AccountInfo> updateList = new ArrayList<>();
        for (Ec2Instance instance : filteredInstances) {
            String instanceId = instance.getId();
            String instanceName = instance.getName();
            String elasticIp = getElasticIp(instanceId);

            String newInstanceId = createInstance(CreateEc2Request.builder().name(instanceName).build());
            associateElasticIp(newInstanceId, elasticIp);
            updateInstanceName(newInstanceId, instanceName);

            AccountInfo accountInfo = accountInfoMap.getOrDefault(instanceId, null);
            if (accountInfo != null) {
                accountInfo.setId(newInstanceId);
                updateList.add(accountInfo);
            }

            terminateInstance(instanceId);
        }
        accountInfoService.saveAll(updateList);
    }

    public void changeInstance(String instanceId) {
        Optional<Ec2Instance> instanceOpt = getAllInstances().stream().filter(ec2Instance -> ec2Instance.getId().equals(instanceId)).findFirst();
        if(instanceOpt.isEmpty()) {
            throw new RuntimeException("Instance not found");
        }
        Ec2Instance instance = instanceOpt.get();
        String instanceName = instance.getName();
        String elasticIp = getElasticIp(instanceId);

        // 새 EC2 생성
        String newInstanceId = createInstance(CreateEc2Request.builder().name(instanceName).build());
        String instanceState = getInstanceState(newInstanceId);
        // EC2 생성이 완료될 때까지 대기
        while (!"running".equals(instanceState)) {
            instanceState = getInstanceState(newInstanceId);
            try {
                Thread.sleep(3000); // 3초 대기
            } catch (InterruptedException e) {
                log.error("Thread sleep error", e);
            }
        }
        // Elastic IP 할당
        associateElasticIp(newInstanceId, elasticIp);
        // EC2 이름 변경
        updateInstanceName(newInstanceId, instanceName);

        AccountInfo accountInfo = accountInfoService.getAccountInfoByInstanceId(instanceId);
        accountInfo.setInstanceId(newInstanceId);
        accountInfoService.save(accountInfo);

        // 기존 EC2 종료
        terminateInstance(instanceId);
    }

    private List<Ec2Instance> filterInstances(List<Ec2Instance> instances) {
        return instances.stream()
                .filter(instance -> instance.getName().startsWith(SCRAP_INSTANCE_NAME_PREFIX)).toList();
    }

    private String getElasticIp(String instanceId) {
        DescribeAddressesRequest request = DescribeAddressesRequest.builder()
                .filters(Filter.builder().name("instance-id").values(instanceId).build())
                .build();
        DescribeAddressesResponse response = ec2Client.describeAddresses(request);
        return response.addresses().stream()
                .findFirst()
                .map(Address::publicIp)
                .orElse("");
    }

    private void associateElasticIp(String instanceId, String elasticIp) {
        AssociateAddressRequest associateRequest = AssociateAddressRequest.builder()
                .instanceId(instanceId)
                .publicIp(elasticIp)
                .build();
        ec2Client.associateAddress(associateRequest);
    }

    private void updateInstanceName(String instanceId, String instanceName) {
        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(Tag.builder().key("Name").value(instanceName).build())
                .build();
        ec2Client.createTags(tagRequest);
    }

    public String createInstance(CreateEc2Request request) {
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(awsEc2Props.getAmiId())
                .instanceType(InstanceType.T3_SMALL)
                .maxCount(1)
                .minCount(1)
                .keyName(awsEc2Props.getKeyName())
                .securityGroupIds(awsEc2Props.getSecurityGroup())
                .tagSpecifications(
                        TagSpecification.builder()
                                .resourceType(ResourceType.INSTANCE)
                                .tags(Tag.builder().key("Name").value(request.getName()).build())
                                .build()
                )
                .build();

        RunInstancesResponse runResponse = ec2Client.runInstances(runRequest);
        return runResponse.instances().getFirst().instanceId();
    }

    public String getInstanceState(String instanceId) {
        var describeInstanceStatusRequest = software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest.builder()
                .instanceIds(instanceId)
                .build();
        var describeInstanceStatusResponse = ec2Client.describeInstanceStatus(describeInstanceStatusRequest);

        if (!describeInstanceStatusResponse.instanceStatuses().isEmpty()) {
            return describeInstanceStatusResponse.instanceStatuses().getFirst().instanceState().nameAsString();
        }
        return null;
    }

    public void allocateElasticIp(String instanceId) {
        AllocateAddressRequest allocateRequest = AllocateAddressRequest.builder()
                .domain(DomainType.VPC)
                .build();
        AllocateAddressResponse allocateResponse = ec2Client.allocateAddress(allocateRequest);

        AssociateAddressRequest associateRequest = AssociateAddressRequest.builder()
                .instanceId(instanceId)
                .allocationId(allocateResponse.allocationId())
                .build();
        ec2Client.associateAddress(associateRequest);
    }

    public void terminateInstance(String instanceId) {
        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        ec2Client.terminateInstances(terminateRequest);
    }
}
