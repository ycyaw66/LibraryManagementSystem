import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.*;

import entities.Book;
import queries.ApiResult;
import utils.ConnectConfig;
import utils.DatabaseConnector;

public class BookSetHandler implements HttpHandler {

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

    public BookSetHandler() {
        try {
            connector = new DatabaseConnector(connectConfig);
            library = new LibraryManagementSystemImpl(connector);
            System.out.println("Successfully init class BookSetHandler.");
            connector.connect();
            System.out.println("Successfully connect to database.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "POST");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("POST")) {
            handlePostRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        try {
            String line;
            List<Book> books = new ArrayList<>();
            int flag = 0;
            while ((line = reader.readLine()) != null) {
                String[] bookInfo = line.split(",");
                if (bookInfo.length == 7 && flag == 0) { // 跳过.csv属性标题
                    flag = 1;
                    continue;
                }
                if (bookInfo.length == 7 && flag == 1) {
                    String category = bookInfo[0];
                    String title = bookInfo[1];
                    String press = bookInfo[2];
                    int publishYear = Integer.parseInt(bookInfo[3]);
                    String author = bookInfo[4];
                    double price = Double.parseDouble(bookInfo[5]);
                    int stock = Integer.parseInt(bookInfo[6]);
                    Book book = new Book(category, title, press, publishYear, author, price, stock);
                    books.add(book);
                }
            }
            ApiResult result = library.storeBook(books);
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
            outputStream.write("批量入库失败".getBytes());
            outputStream.close();
        }
    }
}
