import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        headers.add("Access-Control-Allow-Methods", "GET");
        headers.add("Access-Control-Allow-Headers", "Content-Type");

        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("GET")) {
            handleGetRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        // 获取请求的URI
        URI uri = exchange.getRequestURI();
        // 获取请求的查询字符串
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

            String itemInfo = "{\"cardID\": " + item.getCardId() + ", \"bookID\": " + item.getBookId() + ", \"borrowTime\": \"" + item.transBorrowTime() + "\", \"returnTime\": \"" + item.transReturnTime() + "\"}";
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
        
        outputStream.write(response.getBytes());
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
}
