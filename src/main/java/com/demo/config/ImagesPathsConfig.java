package com.demo.config;

import com.typesafe.config.Config;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Slf4j
public class ImagesPathsConfig {

    private File outputDir;
    private Collection<File> inputFiles;

    public static ImagesPathsConfig fromConfig(Config config) {
        ImagesPathsConfig imagesPathsConfig = new ImagesPathsConfig();

        imagesPathsConfig.inputFiles =
                config.getStringList("files.input").stream()
                        .map(stringPath -> Paths.get(stringPath))
                        .flatMap(ImagesPathsConfig::subFolderFiles)
                        .map(Path::toFile)
                        .collect(Collectors.toList());

        imagesPathsConfig.outputDir = new File(config.getString("files.output"));

        return imagesPathsConfig;
    }

    private static Stream<? extends Path> subFolderFiles(Path path) {
        if (Files.isDirectory(path)) {
            try {
                return Files.walk(path).filter(Files::isRegularFile);
            } catch (IOException e) {
                log.error("Error occurred when ");
                return Stream.empty();
            }
        } else {
            return Stream.of(path);
        }
    }

}
