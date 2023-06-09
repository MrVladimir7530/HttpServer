package org.example.server;

import org.example.handler.HttpHandler;
import org.example.handler.HttpMethod;
import org.example.handler.HttpRequest;
import org.example.handler.HttpResponse;
import org.example.util.PropertyManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Server {
    private final static int BUFFER_SIZE = 256;
    private AsynchronousServerSocketChannel server;

    private Map<String, BlockingQueue<HttpHandler>> handlerPoolMap = new HashMap<>();
    ExecutorService executorService = Executors.newFixedThreadPool(PropertyManager.getPropertyAsInteger("threads", 16));


    public Server() throws Exception {

    }

    public void addHandler(String path, Class handlerClass) {
        int sizeHandlers = PropertyManager.getPropertyAsInteger("capacity", 10);
        BlockingQueue<HttpHandler> handlerPool = new ArrayBlockingQueue<>(sizeHandlers);
        for (int i = 0; i < sizeHandlers; i++) {
            HttpHandler handler = null;
            try {
                handler = (HttpHandler) handlerClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            handlerPool.add(handler);
        }
        handlerPoolMap.put(path, handlerPool);
    }

    public void bootstrap() {
        try {
            server = AsynchronousServerSocketChannel.open();
            server.bind(new InetSocketAddress("127.0.0.1", 8088));

            while (true) {
                Future<AsynchronousSocketChannel> future = server.accept();
                handleClient(future);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
    private void handleClient(Future<AsynchronousSocketChannel> future)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {


        AsynchronousSocketChannel clientChannel = future.get();

        while (clientChannel != null && clientChannel.isOpen()) {

            AsynchronousSocketChannel finalClientChannel = clientChannel;

            connectedClient(finalClientChannel);

            clientChannel = null;
        }
    }

    private void connectedClient(AsynchronousSocketChannel clientChannel) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        System.out.println("new client connection");

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuilder builder = new StringBuilder();
            boolean keepReading = true;

            while (keepReading) {
                int readResult = clientChannel.read(buffer).get();

                keepReading = readResult == BUFFER_SIZE;
                buffer.flip();
                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
                builder.append(charBuffer);

                buffer.clear();
            }

            if (!builder.toString().isBlank()) {
                HttpRequest request = new HttpRequest(builder.toString());
                HttpResponse response = new HttpResponse();

                try {
                    BlockingQueue<HttpHandler> handlerPool = handlerPoolMap.get(request.getUrl());
                    if (handlerPool == null) {
                        response.setStatusCode(404);
                        response.setStatus("Not found");
                        response.addHeader("Content-Type", "text/html; charset = utf-8");
                        response.setBody("<html><body><h1>Resource not found</h1></body></html>");
                        writeAndCloseChannel(clientChannel, response);

                    }
                    else {
                        HttpHandler handler = (HttpHandler) handlerPool.poll();
                            HttpMethod method = request.getMethod();
                            String body = null;
                            switch (method) {
                                case GET:
                                    body = handler.doGet(request, response);
                                    break;
                                case POST:
                                    body = handler.doPost(request, response);
                                    break;
                                case PUT:
                                    body = handler.doPut(request, response);
                                    break;
                                case DELETE:
                                    body = handler.doDelete(request, response);
                                    break;
                            }
                            handlerPool.put(handler);
                            if (body != null && !body.isBlank()) {
                                if (response.getHeaders().get("Content-Type") == null) {
                                    response.addHeader("Content-Type", "text/html; charset = utf-8");
                                }
                                response.setBody(body);
                            }
                    }

                    Runnable task = createRunnable(clientChannel, request, response);
                    executorService.submit(task);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                ByteBuffer resp = ByteBuffer.wrap(response.getBytes());
                    clientChannel.write(resp);
                }

        clientChannel.close();
    }

    private Runnable createRunnable(AsynchronousSocketChannel clientChannel, HttpRequest request, HttpResponse response) {

        Runnable task = () -> {
            try {
                String body = handlerWork(request, response);

                if (body != null && !body.isBlank()) {
                    if (response.getHeaders().get("Content-Type") == null) {
                        response.addHeader("Content-Type", "text/html; charset=utf-8");
                    }

                    response.setBody(body);
                }

//                writeAndCloseChannel(clientChannel, response);

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        return task;
    }
    private String handlerWork(HttpRequest request, HttpResponse response) throws Exception {

        HttpHandler handler = this.handlerPoolMap.get(request.getUrl()).take();

        String body = handler.doGet(request, response);

        BlockingQueue<HttpHandler> pool = this.handlerPoolMap.get(request.getUrl());

        pool.add(handler);

        return body;
    }

    private void writeAndCloseChannel(AsynchronousSocketChannel clientChannel, HttpResponse response) throws IOException {
        ByteBuffer resp = ByteBuffer.wrap(response.getBytes());
        clientChannel.write(resp);

        System.out.println("client disconnected");
        clientChannel.close();
    }
}
