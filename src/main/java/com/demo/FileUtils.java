package com.demo;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.demo.JsonUtils.toJson;
import static java.lang.String.format;

@Slf4j
public class FileUtils {

    public static void saveContentToFile(Object object, Path filePath) {
        try {
            Files.write(filePath, toJson(object));
            log.info("Recognition result saved to '{}'", filePath);
        } catch (IOException e) {
            log.error(format("Unable to save service response into file: %s ", filePath), e);
        }
    }

}
