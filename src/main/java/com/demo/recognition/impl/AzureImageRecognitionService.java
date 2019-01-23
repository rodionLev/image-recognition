package com.demo.recognition.impl;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class AzureImageRecognitionService implements Closeable {

    private static final String SERVICE_URL = "https://westcentralus.api.cognitive.microsoft.com/vision/v2.0/recognizeText";
    private final String apiKey;
    private final AsyncHttpClient asyncHttpClient;


    public AzureImageRecognitionService() {
        this.apiKey = System.getProperty("apiKey", "");
        asyncHttpClient = asyncHttpClient();
    }

    public CompletableFuture<Response> postFile(byte[] content) {
        BoundRequestBuilder requestBuilder = asyncHttpClient.preparePost(SERVICE_URL);
        requestBuilder.addQueryParam("mode", "Printed");
        requestBuilder.setHeader("Content-Type", "application/octet-stream");
        requestBuilder.setBody(content);
        return setApiKey(requestBuilder).execute().toCompletableFuture();
    }

    private BoundRequestBuilder setApiKey(BoundRequestBuilder requestBuilder) {
        return requestBuilder.setHeader("Ocp-Apim-Subscription-Key", apiKey);
    }


    public CompletableFuture<Response> fetchResult(String location) {
        return setApiKey(asyncHttpClient.prepareGet(location)).execute().toCompletableFuture();
    }


    @Override
    public void close() throws IOException {
        asyncHttpClient.close();
    }
}
