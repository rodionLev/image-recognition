package com.demo.azure;

import com.demo.azure.dto.TextLine;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.Math.*;

public class TextLinesUtils {

    public static List<TextLine> groupWithDistance(TextLine[] lines) {
        LinkedList<TextLine> result = new LinkedList<>();
        Arrays.sort(lines, (first, second) -> {
            if (isSameLine(first, second, min(oneThirdOfLineHeight(second), oneThirdOfLineHeight(first)))) {
                return first.upperLeftX() - second.upperLeftX();
            }
            return first.upperLeftY() - second.upperLeftY();
        });
        result.add(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            if (isSameLine(result.getLast(), lines[i], min(oneThirdOfLineHeight(result.getLast()), oneThirdOfLineHeight(lines[i])))) {
                result.addLast(concat(result.removeLast(), lines[i]));
            } else result.add(lines[i]);
        }

        return result;
    }

    private static int oneThirdOfLineHeight(TextLine second) {
        return (int) ((second.bottomLeftY() - second.upperLeftY()) / 1.7);
    }

    private static TextLine concat(TextLine removed, TextLine line) {
        int[] boundingBox = new int[]{
                min(removed.getBoundingBox()[0], line.getBoundingBox()[0]),
                min(removed.getBoundingBox()[1], line.getBoundingBox()[1]),

                max(removed.getBoundingBox()[2], line.getBoundingBox()[2]),
                min(removed.getBoundingBox()[3], line.getBoundingBox()[3]),

                max(removed.getBoundingBox()[4], line.getBoundingBox()[4]),
                max(removed.getBoundingBox()[5], line.getBoundingBox()[5]),

                min(removed.getBoundingBox()[6], line.getBoundingBox()[6]),
                max(removed.getBoundingBox()[7], line.getBoundingBox()[7])
        };
        TextLine textLine = new TextLine();
        textLine.setBoundingBox(boundingBox);
        textLine.setText(removed.getText() + " " +line.getText());
        textLine.setWords(ArrayUtils.addAll(removed.getWords(), line.getWords()));
        return textLine;
    }

    private static boolean isSameLine(TextLine first, TextLine second, int distance) {
        return abs(first.upperLeftY() - second.upperLeftY()) <= distance;
    }

    public static List<String> linesBetween(TextLine[] lines, Predicate<TextLine> startsWith, Predicate<TextLine> endsWith) {
        List<String> result = new LinkedList<>();
        boolean startCollect = false;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].getText() == null) continue;
            if (!startCollect && startsWith.test(lines[i])) {
                startCollect = true;
                continue;
            }
            if (startCollect) {
                if (endsWith.test(lines[i]))
                    return result;
                result.add(lines[i].getText());
            }
        }

        return result;
    }

    public static Predicate<TextLine> containsString(String str) {
        return line -> line.getText().toLowerCase().contains(str.toLowerCase());
    }


}
