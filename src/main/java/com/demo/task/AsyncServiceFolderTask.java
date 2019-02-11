package com.demo.task;

import com.demo.azure.service.AzureOcrService;
import com.demo.service.AsyncHttpOcrService;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;
import com.nurkiewicz.asyncretry.RetryExecutor;
import com.typesafe.config.Config;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AsyncServiceFolderTask implements Function<Collection<File>, List<AsyncServiceFolderTask.ServiceResponse>> {
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private RetryExecutor executor = new AsyncRetryExecutor(executorService).
            withExponentialBackoff(10000, 2).   //10s times 2 after each retry
            withMaxDelay(60_000).                                      //60 seconds
            withUniformJitter().                                       //add between +/- 100 ms randomly
            withMaxRetries(20);

    private final Config config;

    public AsyncServiceFolderTask(Config config) {
        this.config = config;
    }

    @Override
    public List<ServiceResponse> apply(Collection<File> files) {
        try (AsyncHttpOcrService service = getService()) {

            Stream<CompletableFuture<ServiceResponse>> futureStream = files.parallelStream()
                    .filter(image -> image.exists() && image.isFile())
                    .map(File::toPath)
                    .map(image -> executor.getFutureWithRetry(ctx -> postImage(service, image)))
                    .filter(Objects::nonNull)
                    .map(imageUploadResult ->
                            imageUploadResult.thenComposeAsync(
                                    postResult ->
                                            tryToFetchOcrResult(postResult.response, postResult.image, service)));
            //wait for all fired requests
            CompletableFuture<ServiceResponse>[] completableFutures = futureStream.toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(completableFutures).join();

            //todo how are we going to process exceptions here?
            return Arrays.stream(completableFutures)
                    .map(f -> f.getNow(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } finally {
            executorService.shutdown();
        }
    }

    private AsyncHttpOcrService getService() {
        return new AzureOcrService(config);
    }

    private CompletableFuture<ImageUploadResult> postImage(AsyncHttpOcrService service, Path image) {
        try {
            log.info("Processing '{}' file", image);
            return service.postFile(Files.readAllBytes(image))
                    .thenApplyAsync(response -> new ImageUploadResult(image, response));
        } catch (IOException e) {
            log.error("Unable to read input image file: {} ", image, e);
        }
        return null;
    }

    private CompletableFuture<ServiceResponse> tryToFetchOcrResult(Response response, Path image, AsyncHttpOcrService service) {
        if (response.getStatusCode() == service.filePostSuccessStatus()) {
            return executor.getFutureWithRetry(ctx -> fetchImage(response, image, service));
        } else {
            log.error("Service wasn't able to process file: {}:\n response code: {} \n body: {} \n ", response.getStatusCode(), response.getResponseBody());
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<ServiceResponse> fetchImage(Response response, Path image, AsyncHttpOcrService service) {
        return service.fetchResult(response.getHeader("Operation-Location"))
                .thenApplyAsync(fetchResponse -> new ServiceResponse(image, fetchResponse));
    }

    private static final class ImageUploadResult {
        private final Path image;
        private final Response response;

        ImageUploadResult(Path image, Response response) {
            this.image = image;
            this.response = response;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ServiceResponse {
        private final Path inputFile;
        private final Response response;
    }
}
