package com.demo.azure.service;

import com.demo.service.AsyncHttpOcrService;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.asynchttpclient.Dsl.asyncHttpClient;

@Slf4j
public class AzureOcrService implements AsyncHttpOcrService, Closeable {

    private final String serviceUrl;
    private final String apiKey;
    private final Collection<String> notReadyResponses;
    private final AsyncHttpClient asyncHttpClient;


    public AzureOcrService(Config config) {
        Config azureConfig = config.getConfig("services.azure");
        this.apiKey = azureConfig.getString("apiKey");
        serviceUrl = azureConfig.getString("endpoint");
        notReadyResponses = azureConfig.getStringList("notReadyResponses");
        asyncHttpClient = asyncHttpClient();
    }

    public CompletableFuture<Response> postFile(byte[] content) {
        BoundRequestBuilder requestBuilder = asyncHttpClient.preparePost(serviceUrl);
        requestBuilder.addQueryParam("mode", "Printed");
        requestBuilder.setHeader("Content-Type", "application/octet-stream");
        requestBuilder.setBody(content);
        return setApiKey(requestBuilder).execute().toCompletableFuture()
                .thenApplyAsync(response -> {
                            if (response.getStatusCode() == 429) {
                                throw new RetryException();
                            } else return response;
                        }
                );
    }

    @Override
    public int filePostSuccessStatus() {
        return 202;
    }

    private BoundRequestBuilder setApiKey(BoundRequestBuilder requestBuilder) {
        return requestBuilder.setHeader("Ocp-Apim-Subscription-Key", apiKey);
    }

    public CompletableFuture<Response> fetchResult(String location) {
        return setApiKey(asyncHttpClient.prepareGet(location))
                .execute().toCompletableFuture().thenApplyAsync(response -> {
                    if (!isNotReadyResponse(response) && response.getStatusCode() != 429) {
                        return response;
                    }
                    throw new RetryException();
                });
    }

    private boolean isNotReadyResponse(Response response) {
        return notReadyResponses.contains(response.getResponseBody());
    }


    @Override
    public void close() {
        try {
            asyncHttpClient.close();
        } catch (IOException e) {
            log.error("Can't close http client: ", e);
        }
    }
}
