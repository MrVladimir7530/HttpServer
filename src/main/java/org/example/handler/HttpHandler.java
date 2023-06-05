package org.example.handler;

public interface HttpHandler {
    String doGet(HttpRequest request, HttpResponse response);
    String doPost(HttpRequest request, HttpResponse response);
    String doPut(HttpRequest request, HttpResponse response);
    String doDelete(HttpRequest request, HttpResponse response);
}
