package com.demo.cli;

import com.demo.JsonUtils;
import com.demo.azure.AzureTargetTransformer;
import com.demo.azure.AzureWalmartTransformer;
import com.demo.azure.KingAzureTransformer;
import com.demo.azure.dto.RecognitionItem;
import com.demo.azure.dto.RecognitionResponse;
import com.demo.azure.dto.TextLine;
import com.demo.config.ImagesPathsConfig;
import com.demo.task.AsyncServiceFolderTask;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import static com.demo.FileUtils.saveContentToFile;
import static com.demo.azure.TextLinesUtils.groupWithDistance;
import static java.lang.Integer.min;
import static java.lang.String.format;

@Slf4j
public class RecognizeImagesIntoDirTask {

    public static void main(String[] args) throws Exception {
        Config config = loadConfiguration();


        log.debug("Loaded config:\n {}", config.root().render(ConfigRenderOptions.concise()));

        AsyncServiceFolderTask asynchService = new AsyncServiceFolderTask(config);

        ImagesPathsConfig imagesPathsConfig = ImagesPathsConfig.fromConfig(config);

        if (!imagesPathsConfig.getOutputDir().exists()) {
            if (!imagesPathsConfig.getOutputDir().mkdirs()) {
                log.info("Unable to create output dir '{}'", imagesPathsConfig.getOutputDir());
            }
        }

        asynchService.apply(imagesPathsConfig.getInputFiles()).stream()
                .map(serviceResponse -> {
                    log.debug("response body:\n {}", serviceResponse.getResponse().getResponseBody());
                    RecognitionResponse recognitionResponse =
                            JsonUtils.toObject(serviceResponse.getResponse().getResponseBody(), RecognitionResponse.class);
                    //todo
                    List<TextLine> textLines = groupWithDistance(recognitionResponse.getRecognitionResult().getLines());
                    recognitionResponse.getRecognitionResult().setLines(textLines.toArray(new TextLine[]{}));
                    return new RecognitionItem(recognitionResponse, serviceResponse.getInputFile());
                })
                .forEach(item ->
                        saveToFile(item, imagesPathsConfig)
                                .compose(contentTransformer(item)).apply(item.getRecognitionResponse()));
    }

    private static Function<RecognitionResponse, ?> contentTransformer(RecognitionItem item) {
        if (containsText(item.getRecognitionResponse().getRecognitionResult().getLines(), "Walmart", 10))
            return AzureWalmartTransformer::transform;
        if (containsText(item.getRecognitionResponse().getRecognitionResult().getLines(), "Target", 10))
            return AzureTargetTransformer::transform;
        if (containsText(item.getRecognitionResponse().getRecognitionResult().getLines(), "King", 5))
            return KingAzureTransformer::transform;
        return Function.identity();
    }


    private static boolean containsText(TextLine[] lines, String brandName, int linesNumberToCheck) {
        for (int i = 0; i < min(lines.length, linesNumberToCheck); i++) {
            if (lines[i].getText() != null && lines[i].getText().toLowerCase().contains(brandName.toLowerCase()))
                return true;
        }
        return false;
    }


    private static Function<Object, Path> saveToFile(RecognitionItem item, ImagesPathsConfig imagesPathsConfig) {
        return object -> {
            Path outputFile = Paths.get(imagesPathsConfig.getOutputDir().getAbsolutePath(), outputFileName(item.getInputFile()));
            saveContentToFile(object, outputFile);
            return outputFile;
        };
    }

    private static String outputFileName(Path image) {
        return format("%s_%s.json", image.getFileName().toString().replace('.', '_'), currentTimeStamp());
    }

    private static String currentTimeStamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(new Date());
    }


    private static Config loadConfiguration() {
        Config config;
        String configPath = System.getProperty("config");
        if (configPath != null) {
            config = ConfigFactory
                    .parseFile(new File(configPath))
                    .withFallback(defaultConfig());
        } else {
            config = defaultConfig();
        }
        return config;
    }

    private static Config defaultConfig() {
        return ConfigFactory.load("conf/application.conf");
    }
}
