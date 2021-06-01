package com.safeway.android.network.model;

import java.util.List;
import java.util.Map;

public class BaseNetworkResult<T> {
    private int httpStatusCode;
    private T outputContent;
    private BaseNetworkError err;
    private String resultContentType;
    private boolean expectingJsonResponse = true;
    private Map<String, List<String>> responseHeaders;
    private String tag;
    // Error?
    // Headers?  Cookies?

    public BaseNetworkResult(T object) {
        outputContent = object;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public T getOutputContent() {
        return outputContent;
    }

    public void setOutputContent(T outputContent) {
        this.outputContent = outputContent;
    }

    public BaseNetworkError getError() {
        return this.err;
    }

    public void setError(BaseNetworkError ne) {
        this.err = ne;
    }

    public String getResultContentType() {
        return resultContentType;
    }

    public void setResultContentType(String resultType) {
        this.resultContentType = resultType;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public BaseNetworkError getErr() {
        return err;
    }

    public void setErr(BaseNetworkError err) {
        this.err = err;
    }

    public boolean isExpectingJsonResponse() {
        return expectingJsonResponse;
    }

    public void setExpectingJsonResponse(boolean expectingJsonResponse) {
        this.expectingJsonResponse = expectingJsonResponse;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
