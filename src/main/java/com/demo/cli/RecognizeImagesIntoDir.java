package com.demo.cli;

import com.demo.recognition.impl.AzureImageRecognitionService;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.lang.String.format;

@CommandLine.Command(description = "Recognize FILE(s) and save results in OUTPUT_DIR folder",
        name = "image-recognition", mixinStandardHelpOptions = true, version = "0.0.1")
public class RecognizeImagesIntoDir implements Callable<Void> {

    private static final Logger logger
            = LoggerFactory.getLogger(RecognizeImagesIntoDir.class);

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "OUTPUT_DIR", description = "Output dir")
    private File outputDir;

    @CommandLine.Parameters(index = "1", arity = "1..*", paramLabel = "FILE", description = "File(s) to process.")
    private Collection<File> imagesToRecognize;


    @Override
    public String toString() {
        return "RecognizeImagesIntoDir{" +
                "outputDir=" + outputDir + ", already exist: " + outputDir.exists() +
                ", imagesToRecognize=" + imagesToRecognize +
                '}';
    }

    public static void main(String[] args) throws Exception {
        RecognizeImagesIntoDir command = new RecognizeImagesIntoDir();
        CommandLine.call(command, args);
    }

    @Override
    public Void call() throws Exception {
        try (AzureImageRecognitionService service = new AzureImageRecognitionService()) {
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    logger.info("Unable to create output dir '{}'", outputDir);
                }
            }
            imagesToRecognize.parallelStream().forEach(image ->
                    postImage(service, image)
                            .thenAccept(postResult ->
                                    postResult.ifPresent(processPostedImage(image, service))));
            return null;
        }

    }

    private CompletableFuture<Optional<Response>> postImage(AzureImageRecognitionService service, File image) {
        try {
            Path imagePath = image.toPath();
            logger.info("Processing '{}' file", imagePath);
            if (image.exists() && image.isFile()) {
                CompletableFuture<Response> completableFuture = service.postFile(readFileContent(image));
                completableFuture.join();
                return completableFuture
                        .thenApply(Optional::of);
            } else logger.info("Skip irregular file {}", image);

        } catch (IOException e) {
            logger.error("Unable to read input image file: {} ", image, e);
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    private Consumer<Response> processPostedImage(File image, AzureImageRecognitionService service) {
        return response -> {
            if (response.getStatusCode() == 202) {
                fetchResponse(image, response, service);
            } else {
                processError(image, response);
            }
        };
    }

    private void processError(File image, Response response) {
        logger.error("Service wasn't able to process file: {}:\n response code: {} \n body: {} \n ",
                image.getAbsolutePath(), response.getStatusCode(), response.getResponseBody());
    }

    private byte[] readFileContent(File image) throws IOException {
        return Files.readAllBytes(image.toPath());
    }

    private void fetchResponse(File image, Response response, AzureImageRecognitionService service) {
        //TODO wait and retry
        Path filePath = Paths.get(outputDir.getAbsolutePath(), outputFileName(image));
        CompletableFuture<Response> fetchResult = service.fetchResult(response.getHeader("Operation-Location"));
        fetchResult.thenAccept(fetchResponse -> saveContentToFile(image, response, filePath));
        fetchResult.join();
    }

    private void saveContentToFile(File image, Response response, Path filePath) {
        try {

            Files.write(filePath, response.getResponseBody().getBytes());
            logger.info("Image '{}' recognition result saved to '{}'", image, filePath);
        } catch (IOException e) {
            logger.error(format("Unable to save service response into file: %s ", filePath), e);
        }
    }

    private String outputFileName(File image) {
        return format("%s_%s.json", image.getName().replace('.', '_'), currentTimeStamp());
    }

    private String currentTimeStamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(new Date());
    }
}
