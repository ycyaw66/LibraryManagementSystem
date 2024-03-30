import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.sun.net.httpserver.*;

import entities.Book;
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import utils.ConnectConfig;
import utils.DatabaseConnector;

public class BookHandler implements HttpHandler {

    private DatabaseConnector connector;
    private LibraryManagementSystem library;

    private static ConnectConfig connectConfig = null;

    static {
        try {
            connectConfig = new ConnectConfig();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public BookHandler() {
        try {
            connector = new DatabaseConnector(connectConfig);
            library = new LibraryManagementSystemImpl(connector);
            System.out.println("Successfully init class BookHandler.");
            connector.connect();
            System.out.println("Successfully connect to database.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 允许所有域的请求，cors处理
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        
        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equals("GET")) {
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) {
            handlePostRequest(exchange);
        } else if (requestMethod.equals("PUT")) {
            handlePutRequest(exchange);
        } else if (requestMethod.equals("DELETE")) {
            handleDeleteRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            // 其他请求返回405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();
        Map<String, String> queryParams = parseQuery(query);
        try {
            BookQueryConditions bookQueryConditions = new BookQueryConditions();
            bookQueryConditions.setCategory(queryParams.get("category"));
            bookQueryConditions.setTitle(queryParams.get("title"));
            bookQueryConditions.setPress(queryParams.get("press"));
            bookQueryConditions.setAuthor(queryParams.get("author"));
            if (queryParams.get("minPublishYear") != null) {
                bookQueryConditions.setMinPublishYear(Integer.valueOf(queryParams.get("minPublishYear")));
            }
            if (queryParams.get("maxPublishYear") != null) {
                bookQueryConditions.setMaxPublishYear(Integer.valueOf(queryParams.get("maxPublishYear")));
            }
            if (queryParams.get("minPrice") != null) {
                bookQueryConditions.setMinPrice(Double.valueOf(queryParams.get("minPrice")));
            }
            if (queryParams.get("maxPrice") != null) {
                bookQueryConditions.setMinPrice(Double.valueOf(queryParams.get("maxPrice")));
            }
            ApiResult result = library.queryBook(bookQueryConditions);
            if (result.ok == false) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(400, 0);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(result.message.getBytes());
                outputStream.close();
                return;
            }
            BookQueryResults bookQueryResults = (BookQueryResults) result.payload;
            List<Book> resBookList = bookQueryResults.getResults();
            String response = "";
            for (int i = 0; i < resBookList.size(); i++) {
                Book book = resBookList.get(i);
    
                String bookInfo = "{\"bookID\": " + book.getBookId() + ", \"category\": \"" + book.getCategory() + "\", \"title\": \"" + book.getTitle() + "\", \"press\": \"" + book.getPress() + "\", \"publishYear\": " + book.getPublishYear() + ", \"author\": \"" + book.getAuthor() + "\", \"price\": " + book.getPrice() + ", \"stock\": " + book.getStock() + "}";
                if (i > 0) {
                    bookInfo = "," + bookInfo;
                }
                response += bookInfo;
            }
            response = "[" + response + "]";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        } catch (Exception e) {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(500, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Failed to retrieve cards".getBytes());
            outputStream.close();
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        String request = parseRequestBody(exchange);
        JSONObject jsonObject = new JSONObject(request);
        try {
            String category = jsonObject.getString("category");
            String title = jsonObject.getString("title");
            String author = jsonObject.getString("author");
            String press = jsonObject.getString("press");
            int publishYear = jsonObject.getInt("publishYear");
            double price = jsonObject.getDouble("price");
            int stock = jsonObject.getInt("stock");

            Book book = new Book(category, title, press, publishYear, author, price, stock);
            ApiResult result = library.storeBook(book);
            if (result.ok == false) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(400, 0);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(result.message.getBytes());
                outputStream.close();
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (Exception e) {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(500, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("图书新建失败".getBytes());
            outputStream.close();
        }
    }

    private String parseRequestBody(HttpExchange exchange) throws IOException {
        InputStream requestBodyStream = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBodyStream));
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        return requestBodyBuilder.toString();
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
        // 处理 PUT 请求的逻辑，暂未实现
        exchange.sendResponseHeaders(501, -1); // 501 Not Implemented
    }
    
    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        // 处理 DELETE 请求的逻辑，暂未实现
        exchange.sendResponseHeaders(501, -1); // 501 Not Implemented
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    // 对参数值进行 URL 解码
                    try {
                        value = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    queryParams.put(key, value);
                }
            }
        }
        return queryParams;
    }
}
