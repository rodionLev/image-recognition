package com.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class Receipt {
    private String store;
    private String address;
    //TODO we should split address like this:
    //"storeStreetAddress":"121 Sheridan Blvd.",
    //"storeCity":"Broomfield",
    //"storeStateOrProvince":"CO",
    private List<ReceiptItem> receiptItems;

}
