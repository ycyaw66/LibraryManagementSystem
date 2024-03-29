import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.*;

import queries.ApiResult;
import queries.BorrowHistories;
import queries.BorrowHistories.Item;
import utils.ConnectConfig;
import utils.DatabaseConnector;

public class BorrowHandler implements HttpHandler {

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

    public BorrowHandler() {
        try {
            connector = new DatabaseConnector(connectConfig);
            library = new LibraryManagementSystemImpl(connector);
            System.out.println("Successfully init class BorrowHandler.");
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
        // 解析请求的方法，看GET还是POST
        String requestMethod = exchange.getRequestMethod();
        // 注意判断要用equals方法而不是==啊，java的小坑（
        if (requestMethod.equals("GET")) {
            // 处理GET
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) {
            // 处理POST
            handlePostRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) {
            // 处理OPTIONS
            // handleOptionsRequest(exchange);
        } else {
            // 其他请求返回405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        // 获取请求的URI
        URI uri = exchange.getRequestURI();
        // 获取请求的查询字符串（即参数部分）
        String query = uri.getQuery();
        // 解析查询字符串
        Map<String, String> queryParams = parseQuery(query);
        
        // 根据参数名获取参数值
        String cardIDStr = queryParams.get("cardID");
        
        int cardID = 0; // 默认值
        try {
            cardID = Integer.parseInt(cardIDStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        ApiResult result = library.showBorrowHistory(cardID);
        BorrowHistories resBorrowList = (BorrowHistories) result.payload;
        String response = "";
        for (int i = 0; i < resBorrowList.getCount(); i++) {
            Item item = resBorrowList.getItems().get(i);

            String itemInfo = "{\"cardID\": " + item.getCardId() + ", \"bookID\": \"" + item.getBookId() + "\", \"borrowTime\": \"" + item.transBorrowTime() + "\", \"returnTime\": \"" + item.transReturnTime() + "\"}";
            if (i > 0) {
                itemInfo = "," + itemInfo;
            }
            response += itemInfo;
        }
        response = "[" + response + "]";

        // 响应头，因为是JSON通信
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        // 写
        outputStream.write(response.getBytes());
        // 流一定要close！！！小心泄漏
        outputStream.close();
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

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        // 读取POST请求体
        InputStream requestBody = exchange.getRequestBody();
        // 用这个请求体（输入流）构造个buffered reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        // 拼字符串的
        StringBuilder requestBodyBuilder = new StringBuilder();
        // 用来读的
        String line;
        // 没读完，一直读，拼到string builder里
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }

        // 看看读到了啥
        // 实际处理可能会更复杂点
        System.out.println("Received POST request to create card with data: " + requestBodyBuilder.toString());

        // 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        // 响应状态码200
        exchange.sendResponseHeaders(200, 0);

        // 剩下三个和GET一样
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write("Card created successfully".getBytes());
        outputStream.close();
    }
}
