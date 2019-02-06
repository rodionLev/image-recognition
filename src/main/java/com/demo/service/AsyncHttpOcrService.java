package com.demo.service;

import org.asynchttpclient.Response;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface AsyncHttpOcrService extends Closeable {

    CompletableFuture<Response> postFile(byte[] content);

    int filePostSuccessStatus();

    CompletableFuture<Response> fetchResult(String location) throws RetryException;

    @Override
    void close();

    final class RetryException extends RuntimeException {
    }
}
