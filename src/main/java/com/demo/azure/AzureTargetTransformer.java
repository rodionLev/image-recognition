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

public class AzureTargetTransformer {

    private static final Pattern ITEM_REGEX = Pattern.compile("(\\d{9,})\\s(.[^$]*)(\\$(\\d+\\s*.\\s*\\d+))?");

    public static Receipt transform(RecognitionResponse recognitionResponse) {
        Receipt receipt = new Receipt();
        receipt.setStore("Target");
        TextLine[] lines = recognitionResponse.getRecognitionResult().getLines();
        receipt.setAddress(String.join(", ", linesBetween(lines, containsString("Target"), AzureTargetTransformer::isItemLine)));
        List<ReceiptItem> receiptItems = new LinkedList<>();
        receipt.setReceiptItems(receiptItems);
        for (String text : linesBetween(lines, AzureTargetTransformer::isItemLine, containsString("SUBTOTAL"))) {
            ReceiptItem receiptItem = parseItemLine(text);
            if (receiptItem != null) {
                receiptItems.add(receiptItem);
            }
        }
        return receipt;
    }

    private static ReceiptItem parseItemLine(String text) {
        Matcher matcher = ITEM_REGEX.matcher(text.trim());
        if (matcher.find()) {
            ReceiptItem result = new ReceiptItem();
            result.setUpc(matcher.group(1));
            result.setDescription(matcher.group(2));
            String price = matcher.group(3);
            if (price != null) {
                result.setPrice(price.replaceAll(" ", ""));
            }
            return result;
        } else return null;
    }

    private static boolean isItemLine(TextLine line) {
        return ITEM_REGEX.matcher(line.getText()).find();
    }
}
