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
            String sameBookCheck = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";

            pStmt = conn.prepareStatement(sameBookCheck);
            pStmt.setString(1, category);
            pStmt.setString(2, title);
            pStmt.setString(3, press);
            pStmt.setInt(4, publishYear);
            pStmt.setString(5, author);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "图书添加失败：存在相同图书");
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
        return new ApiResult(true, "图书添加成功");
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String selectStockQuery = "SELECT stock FROM book WHERE book_id = ?";
            pStmt = conn.prepareStatement(selectStockQuery);
            pStmt.setInt(1, bookId);
            rSet = pStmt.executeQuery();

            int currentStock = 0;
            if (rSet.next()) {
                currentStock = rSet.getInt("stock");
            }
            else {
                return new ApiResult(false, "库存修改失败：图书不存在");
            }

            if (currentStock + deltaStock < 0) {
                return new ApiResult(false, "库存修改失败：库存为负");
            }

            String incBookStockQuery = "UPDATE book SET stock = stock + ? WHERE book_id = ?";
            
            pStmt = conn.prepareStatement(incBookStockQuery);
            pStmt.setInt(1, deltaStock);
            pStmt.setInt(2, bookId);
            pStmt.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
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
        return new ApiResult(true, "库存修改成功");
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        PreparedStatement pStmt_i = null;
        ResultSet rSet = null;
        try {
            String sameBookCheck = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
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
                    return new ApiResult(false, "图书添加失败：存在相同图书");
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
        return new ApiResult(true, "图书添加成功");
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
                return new ApiResult(false, "图书删除失败：图书处于出借状态");
            }

            String bookExistCheck = "SELECT * FROM book WHERE book_id = ?";
            pStmt = conn.prepareStatement(bookExistCheck);
            pStmt.setInt(1, bookId);
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "图书删除失败：图书不存在");
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
        return new ApiResult(true, "图书删除成功");
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
                return new ApiResult(false, "图书修改失败：图书不存在");
            }

            String sameBookCheck = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ? AND book_id <> ?";
            pStmt = conn.prepareStatement(sameBookCheck);
            pStmt.setString(1, book.getCategory());
            pStmt.setString(2, book.getTitle());
            pStmt.setString(3, book.getPress());
            pStmt.setInt(4, book.getPublishYear());
            pStmt.setString(5, book.getAuthor());
            pStmt.setInt(6, book.getBookId());
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "图书修改失败：存在相同图书");
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
        return new ApiResult(true, "图书修改成功");
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        BookQueryResults bookQueryResults;
        try {
            String category = conditions.getCategory();
            String title = conditions.getTitle();
            String press = conditions.getPress();
            Integer tminPublishYear = conditions.getMinPublishYear();
            Integer tmaxPublishYear = conditions.getMaxPublishYear();
            int minPublishYear = -2147483647, maxPublishYear = 2147483647;
            String author = conditions.getAuthor();
            Double tminPrice = conditions.getMinPrice();
            Double tmaxPrice = conditions.getMaxPrice();
            double minPrice = -2147483647, maxPrice = 2147483647;
            if(tminPublishYear != null) minPublishYear = tminPublishYear.intValue();
            if(tmaxPublishYear != null) maxPublishYear = tmaxPublishYear.intValue();
            if(tminPrice != null) minPrice = tminPrice.doubleValue();
            if(tmaxPrice != null) maxPrice = tmaxPrice.doubleValue();

            String selectBookQuery = "SELECT * FROM book WHERE publish_year >= ? AND publish_year <= ? AND price >= ? AND price <= ?";
            int index = 5;
            int categoryIndex = 0, titleIndex = 0, pressIndex = 0, authorIndex = 0;
            if (category != null) {
                selectBookQuery += " AND category = ?";
                categoryIndex = index++;
            }
            if (title != null) {
                selectBookQuery += " AND title LIKE ?";
                title = "%" + title + "%";
                titleIndex = index++;
            }
            if (press != null) {
                selectBookQuery += " AND press LIKE ?";
                press = "%" + press + "%";
                pressIndex = index++;
            }
            if (author != null) {
                selectBookQuery += " AND author LIKE ?";
                author = "%" + author + "%";
                authorIndex = index++;
            }
            selectBookQuery += " ORDER BY " + conditions.getSortBy().getValue() + " " + conditions.getSortOrder().getValue();
            if (conditions.getSortBy().getValue() != "book_id") {
                selectBookQuery += ", book_id ASC";
            }
            pStmt = conn.prepareStatement(selectBookQuery);
            pStmt.setInt(1, minPublishYear);
            pStmt.setInt(2, maxPublishYear);
            pStmt.setDouble(3, minPrice);
            pStmt.setDouble(4, maxPrice);
            if (categoryIndex > 0) pStmt.setString(categoryIndex, category);
            if (titleIndex > 0) pStmt.setString(titleIndex, title);
            if (pressIndex > 0) pStmt.setString(pressIndex, press);
            if (authorIndex > 0) pStmt.setString(authorIndex, author);
            rSet = pStmt.executeQuery();

            List<Book> books = new ArrayList<Book>();
            while (rSet.next()) {
                int bookId = rSet.getInt("book_id");
                String tempCategory = rSet.getString("category");
                String tempTitle = rSet.getString("title");
                String tempPress = rSet.getString("press");
                int tempPublishYear = rSet.getInt("publish_year");
                String tempAuthor = rSet.getString("author");
                double tempPrice = rSet.getDouble("price");
                int tempStock = rSet.getInt("stock");
                Book book = new Book(tempCategory, tempTitle, tempPress, tempPublishYear, tempAuthor, tempPrice, tempStock);
                book.setBookId(bookId);
                books.add(book);
            }
            bookQueryResults = new BookQueryResults(books);

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
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

            String userCheck = "SELECT * FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0";
            pStmt = conn.prepareStatement(userCheck);
            pStmt.setInt(1, bookId);
            pStmt.setInt(2, cardId);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "借书失败：该书尚未归还");
            }

            String bookDecQuery = "UPDATE book SET stock = stock - 1 WHERE book_id = ? AND stock > 0";
            pStmt = conn.prepareStatement(bookDecQuery);
            pStmt.setInt(1, bookId);
            int affectedRows = pStmt.executeUpdate();
            if (affectedRows == 0) {
                rollback(conn);
                return new ApiResult(false, "借书失败：库存不足");
            }

            String insertBorrowQuery = "INSERT INTO borrow (card_id, book_id, borrow_time, return_time) VALUES (?, ?, ?, 0)";
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
        return new ApiResult(true, "借书成功");
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
            long borrowTime = 0;
            if (rSet.next()) {
                borrowTime = rSet.getLong("borrow_time");
            }
            else {
                return new ApiResult(false, "还书失败：图书/借书证不存在或无需归还该书");
            }
            if (borrowTime >= returnTime) {
                return new ApiResult(false, "还书失败：还书时间早于借书时间");
            }

            String bookIncQuery = "UPDATE book SET stock = stock + 1 WHERE book_id = ?";
            pStmt = conn.prepareStatement(bookIncQuery);
            pStmt.setInt(1, bookId);
            pStmt.executeUpdate();

            String returnQuery = "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND return_time = 0";
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
        return new ApiResult(true, "还书成功");
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

                if (resBook.next()) {
                    Book book = new Book(resBook.getString("category"), resBook.getString("title"), resBook.getString("press"), resBook.getInt("publish_year"), resBook.getString("author"), resBook.getDouble("price"), resBook.getInt("stock"));
                    book.setBookId(bookId);

                    Borrow borrow = new Borrow(bookId, cardId);
                    borrow.setBorrowTime(rSet.getLong("borrow_time"));
                    borrow.setReturnTime(rSet.getLong("return_time"));

                    Item item = new Item(cardId, book, borrow);
                    items.add(item);
                }
            }
            borrowHistories = new BorrowHistories(items);
            
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
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
            
            String sameCardCheck = "SELECT * FROM card WHERE name = ? AND department = ? AND type = ?";
            pStmt = conn.prepareStatement(sameCardCheck);
            pStmt.setString(1, name);
            pStmt.setString(2, department);
            pStmt.setString(3, type);
            rSet = pStmt.executeQuery();
            if (rSet.next()) {
                return new ApiResult(false, "借书证新建失败：存在相同借书证");
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
        return new ApiResult(true, "借书证新建成功");
    }

    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            String cardExistCheck = "SELECT * FROM card WHERE card_id = ?";
            pStmt = conn.prepareStatement(cardExistCheck);
            pStmt.setInt(1, card.getCardId());
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "不存在该借书证");
            }

            String modifyCardInfoQuery = "UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?";
            pStmt = conn.prepareStatement(modifyCardInfoQuery);
            pStmt.setString(1, card.getName());
            pStmt.setString(2, card.getDepartment());
            pStmt.setString(3, card.getType().getStr());
            pStmt.setInt(4, card.getCardId());
            pStmt.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } finally {
            try {
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
        return new ApiResult(true, "借书证修改成功");
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
                return new ApiResult(false, "借书证删除失败：有未归还的图书");
            }

            String cardExistCheck = "SELECT * FROM card WHERE card_id = ?";
            pStmt = conn.prepareStatement(cardExistCheck);
            pStmt.setInt(1, cardId);
            rSet = pStmt.executeQuery();
            if (!rSet.next()) {
                return new ApiResult(false, "不存在该借书证");
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
        return new ApiResult(true, "借书证删除成功");
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
