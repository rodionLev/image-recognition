package com.demo.azure.dto;

import lombok.Data;

@Data
public class Word {
    int[] boundingBox;
    String text;
    String confidence;
}
