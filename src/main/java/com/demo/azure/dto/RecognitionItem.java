package com.demo.azure.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

@Data
@AllArgsConstructor
public class RecognitionItem {
    private RecognitionResponse recognitionResponse;
    private Path inputFile;
}
