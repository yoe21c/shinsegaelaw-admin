package com.tbm.admin.utils;

public class ContentUtils {

    /**
     * 주어진 텍스트를 개행을 기준으로 분리하고, 각 줄을 80자로 나누어 총 라인 수를 계산합니다.
     *
     * @param text          계산할 텍스트
     * @param maxLineLength 한 줄에 허용되는 최대 문자 수
     * @return 총 라인 수
     */
    public static int calculateTotalLines(String text, int maxLineLength) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 개행 문자(\n 또는 \r\n)를 기준으로 텍스트 분리
        String[] lines = text.split("\\r?\\n");
        int totalLines = 0;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                // 빈 줄도 하나의 라인으로 카운트
                totalLines += 1;
                continue;
            }

            int lineLength = line.length();
            int lineCount = (int) Math.ceil((double) lineLength / maxLineLength);
            totalLines += lineCount;
        }

        return totalLines;
    }
}
