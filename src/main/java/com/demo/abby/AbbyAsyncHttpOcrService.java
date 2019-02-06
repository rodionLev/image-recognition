package com.demo.abby;

import com.demo.service.AsyncHttpOcrService;
import com.typesafe.config.Config;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

public class AbbyAsyncHttpOcrService implements AsyncHttpOcrService {

    public AbbyAsyncHttpOcrService(Config config) {

    }

    @Override
    public CompletableFuture<Response> postFile(byte[] content) {
        return null;
    }

    @Override
    public int filePostSuccessStatus() {
        return 200;
    }

    @Override
    public CompletableFuture<Response> fetchResult(String location) throws RetryException {
        return null;
    }

    @Override
    public void close() {

    }
}
