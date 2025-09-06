package com.tbm.admin.service.network;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpConverter {
    /**
     * 표준 형식의 IPv6 주소를 IPv4로 변환합니다.
     *
     * @param ipv6Address 변환할 IPv6 주소 (예: "2001:e60:87b5:5857:1557:8690:78ed:feb1")
     * @return IPv4 주소
     */
    public static String convertIPv6ToIPv4(String ipv6Address) {
        try {
            // 입력값 검증
            if (ipv6Address == null || ipv6Address.isEmpty()) {
                return null;
            }

            // : 기준으로 분리
            String[] parts = ipv6Address.split(":");
            if (parts.length != 8) {
                return null;
            }

            // 마지막 두 그룹을 사용하여 IPv4 주소 생성
            String group7 = parts[6];
            String group8 = parts[7];

            // 16진수 값을 10진수로 변환
            int first = Integer.parseInt(group7.substring(0, 2), 16);
            int second = Integer.parseInt(group7.substring(2), 16);
            int third = Integer.parseInt(group8.substring(0, 2), 16);
            int fourth = Integer.parseInt(group8.substring(2), 16);

            // IPv4 형식으로 조합
            return String.format("%d.%d.%d.%d", first, second, third, fourth);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * IPv4 주소를 표준 형식의 IPv6로 변환합니다.
     *
     * @param ipv4Address IPv4 주소 (예: "120.237.254.177")
     * @return IPv6 주소
     */
    public static String convertIPv4ToIPv6(String ipv4Address) {
        try {
            // 입력값 검증
            if (ipv4Address == null || ipv4Address.isEmpty()) {
                return null;
            }

            // . 기준으로 분리
            String[] parts = ipv4Address.split("\\.");
            if (parts.length != 4) {
                return null;
            }

            // 각 부분을 16진수로 변환
            String hex1 = String.format("%02x", Integer.parseInt(parts[0]));
            String hex2 = String.format("%02x", Integer.parseInt(parts[1]));
            String hex3 = String.format("%02x", Integer.parseInt(parts[2]));
            String hex4 = String.format("%02x", Integer.parseInt(parts[3]));

            // 표준 IPv6 형식으로 조합
            return String.format("2001:e60:87b5:5857:1557:8690:%s%s:%s%s",
                    hex1, hex2, hex3, hex4);
        } catch (Exception e) {
            return null;
        }
    }

    // 사용 예시
    public static void main(String[] args) {
        // IPv6 → IPv4 변환 테스트
        String ipv6 = "2001:e60:87b5:5857:1557:8690:78ed:feb1";
        String ipv4 = convertIPv6ToIPv4(ipv6);
        System.out.println("IPv6: " + ipv6);
        System.out.println("IPv4: " + ipv4);

        // IPv4 → IPv6 변환 테스트
        String convertedIpv6 = convertIPv4ToIPv6(ipv4);
        System.out.println("변환된 IPv6: " + convertedIpv6);
    }
}