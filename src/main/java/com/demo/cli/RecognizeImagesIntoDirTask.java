package com.demo.cli;

import com.demo.recognition.impl.AzureImageRecognitionService;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;
import com.nurkiewicz.asyncretry.RetryExecutor;
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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;

@CommandLine.Command(description = "Recognize FILE(s) and save results in OUTPUT_DIR folder",
        name = "image-recognition", mixinStandardHelpOptions = true, version = "0.0.1")
public class RecognizeImagesIntoDirTask implements Callable<Void> {

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private RetryExecutor executor = new AsyncRetryExecutor(executorService).
            withExponentialBackoff(500, 2).    //500ms times 2 after each retry
            withMaxDelay(10_000).                                     //10 seconds
            withUniformJitter().                                      //add between +/- 100 ms randomly
            withMaxRetries(20);

    private static final Logger logger
            = LoggerFactory.getLogger(RecognizeImagesIntoDirTask.class);

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "OUTPUT_DIR", description = "Output dir")
    private File outputDir;

    @CommandLine.Parameters(index = "1", arity = "1..*", paramLabel = "FILE", description = "File(s) to process.")
    private Collection<File> imagesToRecognize;


    @Override
    public String toString() {
        return "RecognizeImagesIntoDirTask{" +
                "outputDir=" + outputDir + ", already exist: " + outputDir.exists() +
                ", imagesToRecognize=" + imagesToRecognize +
                '}';
    }

    public static void main(String[] args) throws Exception {
        RecognizeImagesIntoDirTask command = new RecognizeImagesIntoDirTask();
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

            CompletableFuture[] allrequests = imagesToRecognize.parallelStream()
                    .filter(image -> image.exists() && image.isFile())
                    .map(File::toPath)
                    .map(image -> postImage(service, image))
                    .filter(Objects::nonNull)
                    .map(imageUploadResult ->
                            imageUploadResult.thenComposeAsync(
                                    postResult ->
                                            tryToFetchRecognitionResult(postResult.response, postResult.image, service)))
                    .toArray(CompletableFuture[]::new);

            //wait for all fired requests
            CompletableFuture.allOf(allrequests).join();

            return null;
        } finally {
            executorService.shutdown();
        }
    }

    private CompletableFuture<ImageUploadResult> postImage(AzureImageRecognitionService service, Path image) {
        try {
            logger.info("Processing '{}' file", image);
            return service.postFile(Files.readAllBytes(image))
                    .thenApplyAsync(response -> new ImageUploadResult(image, response));
        } catch (IOException e) {
            logger.error("Unable to read input image file: {} ", image, e);
        }
        return null;
    }

    private CompletableFuture<Void> tryToFetchRecognitionResult(Response response, Path image, AzureImageRecognitionService service) {

        if (response.getStatusCode() == 202) {
            return executor.getFutureWithRetry(ctx -> fetchImage(response, image, service));
        } else {
            return processError(response);
        }
    }

    private CompletableFuture<Void> processError(Response response) {
        logger.error("Service wasn't able to process file: {}:\n response code: {} \n body: {} \n ", response.getStatusCode(), response.getResponseBody());
        return CompletableFuture.completedFuture(null);
    }


    private CompletableFuture<Void> fetchImage(Response response, Path image, AzureImageRecognitionService service) {
        Path outputFile = Paths.get(outputDir.getAbsolutePath(), outputFileName(image));
        return service.fetchResult(response.getHeader("Operation-Location"))
                .thenAccept(fetchResponse -> saveContentToFile(fetchResponse, outputFile));
    }

    private void saveContentToFile(Response response, Path filePath) {
        try {
            String responseBody = response.getResponseBody();
            if ("{\"status\":\"Running\"}".equals(responseBody)) {
                throw new NotReadyException();
            }
            Files.write(filePath, responseBody.getBytes());
            logger.info("Recognition result saved to '{}'", filePath);
        } catch (IOException e) {
            logger.error(format("Unable to save service response into file: %s ", filePath), e);
        }
    }

    private String outputFileName(Path image) {
        return format("%s_%s.json", image.getFileName().toString().replace('.', '_'), currentTimeStamp());
    }

    private String currentTimeStamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(new Date());
    }

    private static final class NotReadyException extends RuntimeException {
    }

    private static final class ImageUploadResult {
        private final Path image;
        private final Response response;

        ImageUploadResult(Path image, Response response) {
            this.image = image;
            this.response = response;
        }
    }
}
