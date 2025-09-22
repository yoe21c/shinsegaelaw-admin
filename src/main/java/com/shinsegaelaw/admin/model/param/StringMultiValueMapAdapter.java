package com.shinsegaelaw.admin.model.param;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMapAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StringMultiValueMapAdapter extends MultiValueMapAdapter {

    public StringMultiValueMapAdapter(Map<String, List<String>> targetMap) {
        super(targetMap);
    }

    public Integer intVal(String key) {
        List<String> values = super.get(key);
        return (values != null && !values.isEmpty() ? (StringUtils.isNotBlank(values.get(0)) ? Integer.valueOf(values.get(0)) : null) : null);
    }

    public Long longVal(String key) {
        List<String> values = super.get(key);
        return (values != null && !values.isEmpty() ? (StringUtils.isNotBlank(values.get(0)) ? Long.valueOf(values.get(0)) : null) : null);
    }

    public Long longValNullable(String key, Long nullableValue) {
        List<String> values = super.get(key);
        final Long value = values != null && !values.isEmpty() ? (StringUtils.isNotBlank(values.get(0)) ? Long.valueOf(values.get(0)) : null) : null;
        if(value != null && value.equals(nullableValue)) {
            return null;
        }
        return value;
    }

    public String stringVal(String key) {
        List<String> values = super.get(key);
        return (values != null && !values.isEmpty() ? values.get(0) : null);
    }

    public Map<String, String> mapByStartWith(String startKey) {

        Map<String, String> map = new LinkedHashMap<>();
        Set<Map.Entry<String, ArrayList<String>>> sets = super.entrySet();
        for (Entry<String, ArrayList<String>> entry : sets) {
            if(entry.getKey().startsWith(startKey)) {
                map.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return map;
    }

    public List<Long> seqByStartWith(String startKey) {

        Set<Long> seqs = new LinkedHashSet<>();   // 중복을 자연스럽게 제거하기 위해서.
        Set<Map.Entry<String, ArrayList<String>>> sets = super.entrySet();
        for (Entry<String, ArrayList<String>> entry : sets) {
            if(entry.getKey().startsWith(startKey)) {
                String key = entry.getKey();
                seqs.add(Long.valueOf(key.split(startKey)[1]));
            }
        }
        return seqs.stream().toList();
    }

    public List<String> stringValuesByPrefixKey(String prefixKey) {
        final Set<String> keySet = super.keySet();
        List<String> values = new ArrayList<>();
        for (String key : keySet) {
            if(key.startsWith(prefixKey)) {
                values.add(stringVal(key));
            }
        }
        return values;
    }

    public List<String> stringValuesByPrefixKey(String prefixKey, String postfixKey) {
        final Set<String> keySet = super.keySet();
        List<String> values = new ArrayList<>();
        for (String key : keySet) {
            if(key.startsWith(prefixKey) && ! key.endsWith(postfixKey)) {
                values.add(stringVal(key) + ":::" + stringVal(key + postfixKey));
            }
        }
        return values;
    }

    public LocalDate localDateVal(String key) {
        List<String> values = super.get(key);
        if(values.size() > 0 && StringUtils.isBlank(values.get(0))) return null;
        return (values != null && !values.isEmpty() ? LocalDate.parse(values.get(0)) : null);
    }

    public LocalDateTime localDateTimeVal(String key) {
        List<String> values = super.get(key);
        if(values.size() > 0 && StringUtils.isBlank(values.get(0))) return null;
        return (values != null && !values.isEmpty() ? LocalDateTime.parse(values.get(0), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
    }

    public LocalDateTime localDateTimeValISO(String key) {
        List<String> values = super.get(key);
        if(values.size() > 0 && StringUtils.isBlank(values.get(0))) return null;
        if(values.get(0).length() == 16) {
            return (values != null && !values.isEmpty() ? LocalDateTime.parse(values.get(0), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) : null);
        }
        return (values != null && !values.isEmpty() ? LocalDateTime.parse(values.get(0), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : null);
    }

    public boolean exist(String key) {
        List<String> values = super.get(key);
        return (values != null && !values.isEmpty() && StringUtils.isNotBlank(values.get(0)));
    }

    public String stringVal(String key, String defaultValue) {
        List<String> values = super.get(key);
        return values != null && !values.isEmpty() ? values.get(0) : defaultValue;
    }

    public String stringValNullable(String key, String nullableValue) {
        List<String> values = super.get(key);
        final String value = values != null && !values.isEmpty() ? values.get(0) : nullableValue;
        if(value != null && value.equals(nullableValue)) {
            return null;
        }
        return value;
    }
}