package org.example.handler;

public interface HttpHandler {
    String handle(HttpRequest request, HttpResponse response);
}
