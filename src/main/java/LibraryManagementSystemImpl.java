import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import queries.BorrowHistories.Item;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String category = book.getCategory();
            String title = book.getTitle();
            String press = book.getPress();
            int publishYear = book.getPublishYear();
            String author = book.getAuthor();
            double price = book.getPrice();
            int stock = book.getStock();
            
            /* check if there are same books */
            String sameBookCheck = "SELECT COUNT(*) FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";

            pStmt = conn.prepareStatement(sameBookCheck);
            pStmt.setString(1, category);
            pStmt.setString(2, title);
            pStmt.setString(3, press);
            pStmt.setInt(4, publishYear);
            pStmt.setString(5, author);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "Already exist a same book.");
            }

            String storeBookQuery = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";

            pStmt = conn.prepareStatement(storeBookQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            pStmt.setString(1, category);
            pStmt.setString(2, title);
            pStmt.setString(3, press);
            pStmt.setInt(4, publishYear);
            pStmt.setString(5, author);
            pStmt.setDouble(6, price);
            pStmt.setInt(7, stock);
            pStmt.executeUpdate();

            rSet = pStmt.getGeneratedKeys();
            if (rSet.next()) {
                int bookId = rSet.getInt(1);
                book.setBookId(bookId);
            }
            
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String selectStockQuery = "SELECT stock FROM book where book_id = ?";
            pStmt = conn.prepareStatement(selectStockQuery);
            pStmt.setInt(1, bookId);
            rSet = pStmt.executeQuery();

            int currentStock = 0;
            if (rSet.next()) {
                currentStock = rSet.getInt("stock");
            }
            else {
                return new ApiResult(false, "Book not found.");
            }

            if (currentStock + deltaStock < 0) {
                deltaStock = -currentStock;
            }

            String incBookStockQuery = "UPDATE book SET stock = stock + ? WHERE book_id = ?";
            
            pStmt = conn.prepareStatement(incBookStockQuery);
            pStmt.setInt(1, deltaStock);
            pStmt.setInt(2, bookId);
            int affectedRows = pStmt.executeUpdate();

            if (affectedRows > 0) {
                commit(conn);
            }
            else {
                return new ApiResult(false, "Fail to increase book stock.");
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        PreparedStatement pStmt_i = null;
        ResultSet rSet = null;
        try {
            String sameBookCheck = "SELECT COUNT(*) FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
            pStmt = conn.prepareStatement(sameBookCheck);

            String storeBookQuery = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
            pStmt_i = conn.prepareStatement(storeBookQuery, PreparedStatement.RETURN_GENERATED_KEYS);

            for (Book book : books) {
                String category = book.getCategory();
                String title = book.getTitle();
                String press = book.getPress();
                int publishYear = book.getPublishYear();
                String author = book.getAuthor();
                double price = book.getPrice();
                int stock = book.getStock();
                
                /* check if there are same books */
                pStmt.setString(1, category);
                pStmt.setString(2, title);
                pStmt.setString(3, press);
                pStmt.setInt(4, publishYear);
                pStmt.setString(5, author);
                rSet = pStmt.executeQuery();

                if (rSet.next()) {
                    rollback(conn);
                    return new ApiResult(false, "Already exist a same book.");
                }

                pStmt_i.setString(1, category);
                pStmt_i.setString(2, title);
                pStmt_i.setString(3, press);
                pStmt_i.setInt(4, publishYear);
                pStmt_i.setString(5, author);
                pStmt_i.setDouble(6, price);
                pStmt_i.setInt(7, stock);
                pStmt_i.addBatch();
            }

            pStmt_i.executeBatch();
            rSet = pStmt_i.getGeneratedKeys();
            int index = 0;
            while (rSet.next()) {
                int bookId = rSet.getInt(1);
                books.get(index++).setBookId(bookId);
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
                if (pStmt_i != null) {
                    pStmt_i.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String bookBorrowedCheck = "SELECT * FROM borrow WHERE book_id = ? AND return_time = 0";
            pStmt = conn.prepareStatement(bookBorrowedCheck);
            pStmt.setInt(1, bookId);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "Book has been borrowed.");
            }

            String bookExistCheck = "SELECT * FROM book WHERE book_id = ?";
            pStmt = conn.prepareStatement(bookExistCheck);
            pStmt.setInt(1, bookId);
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "Book not found.");
            }

            String removeBookQuery = "DELETE FROM book WHERE book_id = ?";
            pStmt = conn.prepareStatement(removeBookQuery);
            pStmt.setInt(1, bookId);
            pStmt.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String bookExistCheck = "SELECT * FROM book WHERE book_id = ?";
            pStmt = conn.prepareStatement(bookExistCheck);
            pStmt.setInt(1, book.getBookId());
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "Book not found.");
            }

            String modifyBookInfoQuery = "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? WHERE book_id = ?";
            pStmt = conn.prepareStatement(modifyBookInfoQuery);
            pStmt.setString(1, book.getCategory());
            pStmt.setString(2, book.getTitle());
            pStmt.setString(3, book.getPress());
            pStmt.setInt(4, book.getPublishYear());
            pStmt.setString(5, book.getAuthor());
            pStmt.setDouble(6, book.getPrice());
            pStmt.setInt(7, book.getBookId());
            pStmt.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        BookQueryResults bookQueryResults;
        try {
            String catagoryLimit = conditions.getCategory();
            String titleLimit = conditions.getTitle();
            String pressLimit = conditions.getPress();
            Integer tminPublishYear = conditions.getMinPublishYear().intValue();
            Integer tmaxPublishYear = conditions.getMaxPublishYear().intValue();
            int minPublishYear = -2147483647, maxPublishYear = 2147483647;
            String authorLimit = conditions.getAuthor();
            Double tminPrice = conditions.getMinPrice();
            Double tmaxPrice = conditions.getMaxPrice();
            double minPrice = 0, maxPrice = 2147483647;
            if(tminPublishYear != null) minPublishYear = tminPublishYear.intValue();
            if(tmaxPublishYear != null) maxPublishYear = tmaxPublishYear.intValue();
            if(tminPrice != null) minPrice = tminPrice.doubleValue();
            if(tmaxPrice != null) maxPrice = tmaxPrice.doubleValue();

            String selectBookQuery = "SELECT * FROM book";
            pStmt = conn.prepareStatement(selectBookQuery);
            rSet = pStmt.executeQuery();

            List<Book> books = new ArrayList<Book>();
            while (rSet.next()) {
                int bookId = rSet.getInt("book_id");
                String category = rSet.getString("catagory");
                String title = rSet.getString("title");
                String press = rSet.getString("press");
                int publishYear = rSet.getInt("publish_year");
                String author = rSet.getString("author");
                double price = rSet.getDouble("price");
                int stock = rSet.getInt("stock");
                if (catagoryLimit != null && category != catagoryLimit) continue;
                if (titleLimit != null && title != titleLimit) continue;
                if (pressLimit != null && press != pressLimit) continue;
                if (authorLimit != null && author != authorLimit) continue;
                if (publishYear < minPublishYear || publishYear > maxPublishYear) continue;
                if (price < minPrice || price > maxPrice) continue;

                Book book = new Book(category, title, press, publishYear, author, price, stock);
                book.setBookId(bookId);
                books.add(book);
            }
            bookQueryResults = new BookQueryResults(books);
            Book.SortColumn sortBy = conditions.getSortBy();
            SortOrder sortOrder = conditions.getSortOrder();
            Comparator<Book> comparator = sortBy.getComparator();
            if (sortOrder == SortOrder.DESC) {
                comparator.reversed();
            }
            books.sort(comparator);

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null, bookQueryResults);
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            int cardId = borrow.getCardId();
            int bookId = borrow.getBookId();
            long borrowTime = borrow.getBorrowTime();

            String bookCheck = "SELECT * FROM book WHERE book_id = ? AND stock > 0";
            pStmt = conn.prepareStatement(bookCheck);
            pStmt.setInt(1, bookId);
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "Insufficient or non-existent books.");
            }

            String cardCheck = "SELECT * FROM card WHERE card_id = ?";
            pStmt = conn.prepareStatement(cardCheck);
            pStmt.setInt(1, cardId);
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "Card does not exist.");
            }

            String userCheck = "SELECT * FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0";
            pStmt = conn.prepareStatement(userCheck);
            pStmt.setInt(1, bookId);
            pStmt.setInt(2, cardId);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "The book has not been returned yet.");
            }

            String bookDecQuery = "UPDATE book SET stock = stock - 1 WHERE book_id = ?";
            pStmt = conn.prepareStatement(bookDecQuery);
            pStmt.setInt(1, bookId);
            pStmt.executeUpdate();

            String insertBorrowQuery = "INSERT INTO borrow (card_id, book_id, borrow_time) VALUES (?, ?, ?)";
            pStmt = conn.prepareStatement(insertBorrowQuery);
            pStmt.setInt(1, cardId);
            pStmt.setInt(2, bookId);
            pStmt.setLong(3, borrowTime);
            pStmt.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            int cardId = borrow.getCardId();
            int bookId = borrow.getBookId();
            long returnTime = borrow.getReturnTime();

            String userCheck = "SELECT * FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0";
            pStmt = conn.prepareStatement(userCheck);
            pStmt.setInt(1, bookId);
            pStmt.setInt(2, cardId);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "Fail to return the book.");
            }

            String bookIncQuery = "UPDATE book SET stock = stock + 1 WHERE book_id = ?";
            pStmt = conn.prepareStatement(bookIncQuery);
            pStmt.setInt(1, bookId);
            pStmt.executeUpdate();

            String returnQuery = "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ?";
            pStmt = conn.prepareStatement(returnQuery);
            pStmt.setLong(1, returnTime);
            pStmt.setInt(2, cardId);
            pStmt.setInt(3, bookId);
            pStmt.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null, resBook = null;
        BorrowHistories borrowHistories;
        try {
            String borrowQuery = "SELECT * FROM borrow WHERE card_id = ? ORDER BY borrow_time DESC, book_id ASC";
            pStmt = conn.prepareStatement(borrowQuery);
            pStmt.setInt(1, cardId);
            rSet = pStmt.executeQuery();

            List<Item> items = new ArrayList<Item>();
            while (rSet.next()) {
                int bookId = rSet.getInt("book_id");
                String bookQuery = "SELECT * FROM book WHERE book_id = ?";
                pStmt = conn.prepareStatement(bookQuery);
                pStmt.setInt(1, bookId);
                resBook = pStmt.executeQuery();

                Book book = new Book(resBook.getString("catagory"), resBook.getString("title"), resBook.getString("press"), resBook.getInt("publish_year"), resBook.getString("author"), resBook.getDouble("price"), resBook.getInt("stock"));
                book.setBookId(bookId);

                Borrow borrow = new Borrow(bookId, cardId);
                borrow.setBorrowTime(rSet.getLong("borrow_time"));
                borrow.setReturnTime(rSet.getLong("return_time"));

                Item item = new Item(cardId, book, borrow);
                items.add(item);
            }
            borrowHistories = new BorrowHistories(items);
            
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (resBook != null) {
                    resBook.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null, borrowHistories);
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String name = card.getName();
            String department = card.getDepartment();
            String type = card.getType().getStr();
            
            String sameCardCheck = "SELECT COUNT(*) FROM card WHERE name = ? AND department = ? AND type = ?";

            pStmt = conn.prepareStatement(sameCardCheck);
            pStmt.setString(1, name);
            pStmt.setString(2, department);
            pStmt.setString(3, type);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "Already exist a same card.");
            }

            String storeCardQuery = "INSERT INTO card (name, department, type) VALUES (?, ?, ?)";
            pStmt = conn.prepareStatement(storeCardQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            pStmt.setString(1, name);
            pStmt.setString(2, department);
            pStmt.setString(3, type);
            pStmt.executeUpdate();

            rSet = pStmt.getGeneratedKeys();
            if (rSet.next()) {
                int cardId = rSet.getInt(1);
                card.setCardId(cardId);
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String bookBorrowedCheck = "SELECT * FROM borrow WHERE card_id = ? AND return_time = 0";
            pStmt = conn.prepareStatement(bookBorrowedCheck);
            pStmt.setInt(1, cardId);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "The card has unreturned books.");
            }

            String cardExistCheck = "SELECT * FROM card WHERE card_id = ?";
            pStmt = conn.prepareStatement(cardExistCheck);
            pStmt.setInt(1, cardId);
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "Card not found.");
            }

            String removeBookQuery = "DELETE FROM card WHERE card_id = ?";
            pStmt = conn.prepareStatement(removeBookQuery);
            pStmt.setInt(1, cardId);
            pStmt.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        CardList cardList;
        try {
            String cardQuery = "SELECT * FROM card ORDER BY card_id ASC";
            pStmt = conn.prepareStatement(cardQuery);
            rSet = pStmt.executeQuery();
            List<Card> cards = new ArrayList<Card>();
            while (rSet.next()) {
                int cardId = rSet.getInt("card_id");
                String name = rSet.getString("name");
                String department = rSet.getString("department");
                String tmptype = rSet.getString("type");
                Card.CardType type = Card.CardType.values(tmptype);
                Card card = new Card(cardId, name, department, type);
                cards.add(card);
            }
            cardList = new CardList(cards);

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (rSet != null) {
                    rSet.close();
                }
                if (pStmt != null) {
                    pStmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ApiResult(true, null, cardList);
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
