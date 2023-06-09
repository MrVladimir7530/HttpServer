package org.example.handlers;


import org.example.handler.HttpHandler;
import org.example.handler.HttpRequest;
import org.example.handler.HttpResponse;

public class SimpleHandler implements HttpHandler {
    @Override
    public String doGet(HttpRequest request, HttpResponse response) {
        return  "<html><body><h1>Hello, native2</h1>It</body></html>";
    }

    @Override
    public String doPost(HttpRequest request, HttpResponse response) {
        return null;
    }

    @Override
    public String doPut(HttpRequest request, HttpResponse response) {
        return null;
    }

    @Override
    public String doDelete(HttpRequest request, HttpResponse response) {
        return null;
    }
    //todo дописать методы
}
