package com.demo.azure;

import com.demo.azure.dto.RecognitionResponse;
import com.demo.azure.dto.TextLine;
import com.demo.dto.Receipt;
import com.demo.dto.ReceiptItem;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.demo.azure.TextLinesUtils.containsString;
import static com.demo.azure.TextLinesUtils.linesBetween;

public class AzureWalmartTransformer {

    private static final String RECEIPT_INFO_LINE_START = "ST#";
    private static final Pattern ITEM_REGEXP = Pattern.compile("(.*)\\s+(\\d{9,})\\s+[A-Z]?\\s*(\\d+\\s*.\\s*\\d+)?\\s*([NYXTRO]*)");

    public static Receipt transform(RecognitionResponse recognitionResponse) {
        Receipt receipt = new Receipt();
        receipt.setStore("Walmart");
        TextLine[] lines = recognitionResponse.getRecognitionResult().getLines();
        receipt.setAddress(String.join(", ", linesBetween(lines, containsString("walmart"), containsString(RECEIPT_INFO_LINE_START))));
        List<ReceiptItem> receiptItems = new LinkedList<>();
        receipt.setReceiptItems(receiptItems);
        for (String text : linesBetween(lines, containsString(RECEIPT_INFO_LINE_START), containsString("SUBTOTAL"))) {
            ReceiptItem receiptItem = parseItemLine(text);
            if (receiptItem != null) {
                receiptItems.add(receiptItem);
            }
        }
        return receipt;
    }


    private static ReceiptItem parseItemLine(String text) {
        Matcher matcher = ITEM_REGEXP.matcher(text.trim());
        if (matcher.find()) {
            ReceiptItem result = new ReceiptItem();
            result.setDescription(matcher.group(1));
            result.setUpc(matcher.group(2));
            String price = matcher.group(3);
            if (price != null) {
                result.setPrice(price.replaceAll(" ", ""));
            }
            return result;
        } else return null;
    }
}
