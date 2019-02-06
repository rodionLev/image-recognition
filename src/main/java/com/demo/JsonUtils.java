package com.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonUtils {

    public static <T> T toObject(String content, Class<T> type) {
        try {
            return new ObjectMapper().readValue(content, type);
        } catch (IOException e) {
            throw new RuntimeException("Can't deserialize body to json: ", e);
        }
    }

    public static byte[] toJson(Object object) {
        try {
            return new ObjectMapper().writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't serialize object to json: ", e);
        }
    }
}
