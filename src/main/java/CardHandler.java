import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.sun.net.httpserver.*;

import entities.Card;
import queries.ApiResult;
import queries.CardList;
import utils.ConnectConfig;
import utils.DatabaseConnector;
import org.json.JSONObject;

public class CardHandler implements HttpHandler {

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

    public CardHandler() {
        try {
            connector = new DatabaseConnector(connectConfig);
            library = new LibraryManagementSystemImpl(connector);
            System.out.println("Successfully init class CardHandler.");
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
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
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
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        ApiResult result = library.showCards();
        CardList resCardList = (CardList) result.payload;
        String response = "";
        for (int i = 0; i < resCardList.getCount(); i++) {
            Card card = resCardList.getCards().get(i);

            String type = card.getType().getStr();
            if(type == "S") type = "学生";
            else type = "教师";
            
            String cardInfo = "{\"id\": " + card.getCardId() + ", \"name\": \"" + card.getName() + "\", \"department\": \"" + card.getDepartment() + "\", \"type\": \"" + type + "\"}";
            if (i > 0) {
                cardInfo = "," + cardInfo;
            }
            response += cardInfo;
        }
        response = "[" + response + "]";
        
        // 响应头，因为是JSON通信
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        
        outputStream.write(response.getBytes());
        outputStream.close();
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        String request = parseRequestBody(exchange);
        JSONObject jsonObject = new JSONObject(request);


        System.out.println("Received POST request to create card with data: " + request);

        // 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        // 响应状态码200
        exchange.sendResponseHeaders(200, 0);

        // 剩下三个和GET一样
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write("Card created successfully".getBytes());
        outputStream.close();
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
}