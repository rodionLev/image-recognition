package com.demo.azure.dto;

import lombok.Data;

@Data
public class TextLine {
    int[] boundingBox;
    String text;
    Word[] words;


    public int upperLeftY() {
        return boundingBox[1];
    }

    public int bottomLeftY() {
        return boundingBox[7];
    }

    public int upperLeftX() {
        return boundingBox[0];
    }
}
