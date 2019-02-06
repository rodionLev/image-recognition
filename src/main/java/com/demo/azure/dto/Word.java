package com.demo.azure.dto;

import lombok.Data;

@Data
class Word {
    int[] boundingBox;
    String text;
    String confidence;
}
