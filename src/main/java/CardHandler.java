import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.sun.net.httpserver.*;

import entities.Card;
import entities.Card.CardType;
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
        try {
            ApiResult result = library.showCards();
            if (result.ok == false) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(400, 0);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(result.message.getBytes());
                outputStream.close();
                return;
            }
            CardList resCardList = (CardList) result.payload;
            String response = "";
            for (int i = 0; i < resCardList.getCount(); i++) {
                Card card = resCardList.getCards().get(i);
                
                String type = card.getType().getStr();
                if (type.equals("S")) type = "学生";
                else type = "教师";
    
                String cardInfo = "{\"id\": " + card.getCardId() + ", \"name\": \"" + card.getName() + "\", \"department\": \"" + card.getDepartment() + "\", \"type\": \"" + type + "\"}";
                if (i > 0) {
                    cardInfo = "," + cardInfo;
                }
                response += cardInfo;
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
            String name = jsonObject.getString("name");
            String department = jsonObject.getString("department");
            String type = jsonObject.getString("type");
            CardType cardType = null;
            if (type.equals("学生")) {
                cardType = CardType.Student;
            }
            else {
                cardType = CardType.Teacher;
            }
            Card card = new Card(0, name, department, cardType);
            ApiResult result = library.registerCard(card);
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
            outputStream.write("借书证新建失败".getBytes());
            outputStream.close();
        }
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
        String request = parseRequestBody(exchange);
        JSONObject jsonObject = new JSONObject(request);

        try {
            int cardId = jsonObject.getInt("id");
            String name = jsonObject.getString("name");
            String department = jsonObject.getString("department");
            String type = jsonObject.getString("type");
            CardType cardType = null;
            if (type.equals("学生")) {
                cardType = CardType.Student;
            }
            else {
                cardType = CardType.Teacher;
            }
            Card card = new Card(cardId, name, department, cardType);
            ApiResult result = library.modifyCardInfo(card);
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
            outputStream.write("借书证修改失败".getBytes());
            outputStream.close();
        }
    }
    
    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        String request = parseRequestBody(exchange);
        JSONObject jsonObject = new JSONObject(request);

        try {
            int cardId = jsonObject.getInt("id");
            ApiResult result = library.removeCard(cardId);
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
            outputStream.write("借书证删除失败".getBytes());
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
}