package com.demo.dto;

import lombok.Data;

@Data
public class ReceiptItem {
    private String price;
    private String description;
    private String upc;
}
