import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.List;

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
            int affectedRows = pStmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rset = pStmt.getGeneratedKeys();
                if(rset.next()) {
                    int bookId = rset.getInt(1);
                    book.setBookId(bookId);
                }
                commit(conn);
            }
            else {
                rollback(conn);
                return new ApiResult(false, "Fail to store book.");
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
        ResultSet rSet = null;
        try {

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
    public ApiResult removeBook(int bookId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult registerCard(Card card) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult removeCard(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {

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
