package com.demo.azure.dto;

import lombok.Data;

import java.nio.file.Path;

@Data
public class RecognitionResponse {
    String status;
    RrecognitionResult recognitionResult;
}
