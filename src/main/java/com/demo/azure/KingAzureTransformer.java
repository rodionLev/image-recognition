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

public class KingAzureTransformer {

    private static Pattern ITEM_PATTERN = Pattern.compile("(.*?)(\\d+\\s*.\\s*\\d+)(-)?(\\s*B)?");

    public static Receipt transform(RecognitionResponse recognitionResponse) {
        Receipt receipt = new Receipt();
        receipt.setStore("King Scoopers");
        TextLine[] lines = recognitionResponse.getRecognitionResult().getLines();
        receipt.setAddress(String.join(", ", linesBetween(lines, containsString("King Scoopers"), containsString("Your cashier"))));
        List<ReceiptItem> receiptItems = new LinkedList<>();
        receipt.setReceiptItems(receiptItems);
        for (String text : linesBetween(lines, containsString("Your cashier"), containsString("TAX"))) {
            ReceiptItem receiptItem = parseItemLine(text);
            if (receiptItem != null) {
                receiptItems.add(receiptItem);
            }
        }
        return receipt;
    }

    private static ReceiptItem parseItemLine(String text) {
        Matcher matcher = ITEM_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            ReceiptItem receiptItem = new ReceiptItem();
            receiptItem.setDescription(matcher.group(1));
            receiptItem.setPrice(matcher.group(2));
            return receiptItem;
        }
        return null;
    }

    private static boolean isItemLine(TextLine line) {
        return ITEM_PATTERN.matcher(line.getText()).find();
    }
}
