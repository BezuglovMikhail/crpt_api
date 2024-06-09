package ru.bezuglov;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final ExecutorService executorService;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.executorService =
                new ThreadPoolExecutor(requestLimit, requestLimit, 0L, timeUnit,
                        new LinkedBlockingQueue<Runnable>());
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }

    public enum DocType {
        LP_INTRODUCE_GOODS
    }

    @Getter
    @Setter
    @Builder
    @ToString
    @AllArgsConstructor
    public static class Product {
        private String certificateDocument;
        private LocalDate certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private LocalDate productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private DocType docType;
        private Boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private LocalDate productionDate;
        private String productionType;
        private List<Product> products;
        private LocalDate regDate;
        private String regNumber;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class SignedDocument {
        private Description description;
        private String docId;
        private String docStatus;
        private DocType docType;
        private Boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private LocalDate productionDate;
        private String productionType;
        private List<Product> products;
        private LocalDate regDate;
        private String regNumber;
        private String signature;
        private LocalDate signatureDate;
    }

    public class LocalDateTimeAdapter extends TypeAdapter<LocalDate> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void write(JsonWriter jsonWriter, LocalDate localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDate.format(formatter));
            }
        }

        @Override
        public LocalDate read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == null) {
                jsonReader.nextNull();
                return null;
            } else {
                return LocalDate.parse(jsonReader.nextString(), formatter);
            }
        }
    }

    public Document deserializationDocument(String jsonDocument) {
        Gson gson;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateTimeAdapter());
        gson = gsonBuilder.create();
        Document document = new Document();

        try {
            JsonObject jsonObject = gson.fromJson(jsonDocument, JsonObject.class);
            Description description = gson.fromJson(jsonObject.get("description"), Description.class);
            JsonArray jsonArray = jsonObject.get("products").getAsJsonArray();
            List<Product> products = new ArrayList<>();

            for (JsonElement element : jsonArray) {
                Product product = Product
                        .builder()
                        .certificateDocument(gson.fromJson(element.getAsJsonObject()
                                .get("certificate_document"), String.class))
                        .certificateDocumentDate(gson.fromJson(element.getAsJsonObject()
                                .get("certificate_document_date"), LocalDate.class))
                        .certificateDocumentNumber(gson.fromJson(element.getAsJsonObject()
                                .get("certificate_document_number"), String.class))
                        .ownerInn(gson.fromJson(element.getAsJsonObject().get("owner_inn"), String.class))
                        .producerInn(gson.fromJson(element.getAsJsonObject().get("producer_inn"), String.class))
                        .productionDate(gson.fromJson(element.getAsJsonObject()
                                .get("production_date"), LocalDate.class))
                        .tnvedCode(gson.fromJson(element.getAsJsonObject().get("tnved_code"), String.class))
                        .uitCode(gson.fromJson(element.getAsJsonObject().get("uit_code"), String.class))
                        .uituCode(gson.fromJson(element.getAsJsonObject().get("uitu_code"), String.class))
                        .build();
                products.add(product);
            }

            document = Document
                    .builder()
                    .description(description)
                    .docId(gson.fromJson(jsonObject.getAsJsonObject().get("doc_id"), String.class))
                    .docStatus(gson.fromJson(jsonObject.getAsJsonObject().get("doc_status"), String.class))
                    .docType(gson.fromJson(jsonObject.getAsJsonObject().get("doc_type"), DocType.class))
                    .importRequest(gson.fromJson(jsonObject.getAsJsonObject().get("importRequest"), Boolean.class))
                    .ownerInn(gson.fromJson(jsonObject.getAsJsonObject().get("owner_inn"), String.class))
                    .participantInn(gson.fromJson(jsonObject.getAsJsonObject().get("participant_inn"), String.class))
                    .producerInn(gson.fromJson(jsonObject.getAsJsonObject().get("producer_inn"), String.class))
                    .productionDate(gson.fromJson(jsonObject.getAsJsonObject().get("production_date"), LocalDate.class))
                    .productionType(gson.fromJson(jsonObject.getAsJsonObject().get("production_type"), String.class))
                    .products(products)
                    .regDate(gson.fromJson(jsonObject.getAsJsonObject().get("reg_date"), LocalDate.class))
                    .regNumber(gson.fromJson(jsonObject.getAsJsonObject().get("reg_number"), String.class))
                    .build();
        } catch (NullPointerException e) {
            System.out.println("Получен пустой json");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return document;
    }

    public SignedDocument createDocument(Document document, String signature) throws InterruptedException {
        SignedDocument signedDocument = new SignedDocument();
        if (document != null && signature != null) {
            signedDocument = SignedDocument.builder()
                    .description(document.getDescription())
                    .docId(document.getDocId())
                    .docStatus(document.getDocStatus())
                    .docType(document.getDocType())
                    .importRequest(document.getImportRequest())
                    .ownerInn(document.getOwnerInn())
                    .participantInn(document.getParticipantInn())
                    .producerInn(document.getProducerInn())
                    .productionDate(document.getProductionDate())
                    .productionType(document.getProductionType())
                    .products(document.getProducts())
                    .regDate(document.getRegDate())
                    .regNumber(document.getRegNumber())
                    .signature(signature)
                    .signatureDate(LocalDate.now())
                    .build();
        }
        return signedDocument;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        String signature = "example_signature";
        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(8080), 0);
        httpServer.createContext("/api/v3/lk/documents/create", new HttpHandler() {

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                crptApi.executorService.submit(() -> {
                    String requestMethod = exchange.getRequestMethod();
                    OutputStream outputStream = exchange.getResponseBody();
                    InputStream inputStream = exchange.getRequestBody();
                    try {
                        String documentString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        SignedDocument signedDocument = crptApi
                                .createDocument(crptApi.deserializationDocument(documentString), signature);
                        exchange.sendResponseHeaders(201, 0);
                        outputStream.write((signedDocument.toString()).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    exchange.close();
                });
            }
        });
        httpServer.start();
    }
}
