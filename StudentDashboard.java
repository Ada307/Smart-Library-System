import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;

public class StudentDashboard extends JFrame {
    private DefaultTableModel availableBooksModel;
    private DefaultTableModel borrowedBooksModel;
    private JTable availableBooksTable;
    private JTable borrowedBooksTable;
    private String loggedInUsername;
    private JTabbedPane tabbedPane;

    public StudentDashboard(String username) {
        this.loggedInUsername = username;
        setTitle("Student Dashboard");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for welcome message and logout button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Welcome, " + username + "!"), BorderLayout.WEST);
        JButton logoutButton = new JButton("Logout");
        topPanel.add(logoutButton, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Tabbed pane for Available Books and Borrowed Books
        tabbedPane = new JTabbedPane();

        // Available Books tab
        String[] availableColumns = {"Book ID", "Title", "Author", "ISBN", "Available"};
        availableBooksModel = new DefaultTableModel(availableColumns, 0);
        availableBooksTable = new JTable(availableBooksModel);
        loadAvailableBooks();
        JScrollPane availableScrollPane = new JScrollPane(availableBooksTable);
        JPanel availablePanel = new JPanel(new BorderLayout());
        JButton borrowButton = new JButton("Borrow Book");
        availablePanel.add(availableScrollPane, BorderLayout.CENTER);
        availablePanel.add(borrowButton, BorderLayout.SOUTH);
        tabbedPane.addTab("Available Books", availablePanel);

        // Borrowed Books tab
        String[] borrowedColumns = {"Borrow ID", "Book ID", "Title", "Borrow Date", "Return Date"};
        borrowedBooksModel = new DefaultTableModel(borrowedColumns, 0);
        borrowedBooksTable = new JTable(borrowedBooksModel);
        loadBorrowedBooks();
        JScrollPane borrowedScrollPane = new JScrollPane(borrowedBooksTable);
        JPanel borrowedPanel = new JPanel(new BorderLayout());
        JButton returnButton = new JButton("Return Book");
        borrowedPanel.add(borrowedScrollPane, BorderLayout.CENTER);
        borrowedPanel.add(returnButton, BorderLayout.SOUTH);
        tabbedPane.addTab("Borrowed Books", borrowedPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);

        // Logout button action
        logoutButton.addActionListener(e -> {
            new LoginScreen().setVisible(true);
            dispose();
        });

        // Borrow book button action
        borrowButton.addActionListener(e -> borrowBook());

        // Return book button action
        returnButton.addActionListener(e -> returnBook());
    }

    private void loadAvailableBooks() {
        availableBooksModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM books WHERE is_available = TRUE";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("book_id"));
                row.add(rs.getString("title"));
                row.add(rs.getString("author"));
                row.add(rs.getString("isbn"));
                row.add(rs.getBoolean("is_available") ? "Yes" : "No");
                availableBooksModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading available books: " + e.getMessage());
        }
    }

    private void loadBorrowedBooks() {
        borrowedBooksModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT bb.borrow_id, bb.book_id, b.title, bb.borrow_date, bb.return_date " +
                          "FROM borrowed_books bb " +
                          "JOIN books b ON bb.book_id = b.book_id " +
                          "WHERE bb.username = ? AND bb.return_date IS NULL";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, loggedInUsername);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("borrow_id"));
                row.add(rs.getInt("book_id"));
                row.add(rs.getString("title"));
                row.add(rs.getDate("borrow_date"));
                row.add(rs.getDate("return_date"));
                borrowedBooksModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading borrowed books: " + e.getMessage());
        }
    }

    private void borrowBook() {
        int selectedRow = availableBooksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to borrow!");
            return;
        }

        int bookId = (int) availableBooksModel.getValueAt(selectedRow, 0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Update the book's availability
            String updateQuery = "UPDATE books SET is_available = FALSE WHERE book_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setInt(1, bookId);
            updateStmt.executeUpdate();

            // Record the borrowing in borrowed_books
            String insertQuery = "INSERT INTO borrowed_books (book_id, username, borrow_date) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setInt(1, bookId);
            insertStmt.setString(2, loggedInUsername);
            insertStmt.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            insertStmt.executeUpdate();

            loadAvailableBooks();
            loadBorrowedBooks(); // Refresh both tables
            tabbedPane.setSelectedIndex(1); // Switch to Borrowed Books tab
            JOptionPane.showMessageDialog(this, "Book borrowed successfully!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error borrowing book: " + ex.getMessage());
        }
    }

    private void returnBook() {
        int selectedRow = borrowedBooksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to return!");
            return;
        }

        int borrowId = (int) borrowedBooksModel.getValueAt(selectedRow, 0);
        int bookId = (int) borrowedBooksModel.getValueAt(selectedRow, 1);
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Update the book's availability
            String updateBookQuery = "UPDATE books SET is_available = TRUE WHERE book_id = ?";
            PreparedStatement updateBookStmt = conn.prepareStatement(updateBookQuery);
            updateBookStmt.setInt(1, bookId);
            updateBookStmt.executeUpdate();

            // Update the return date in borrowed_books
            String updateBorrowQuery = "UPDATE borrowed_books SET return_date = ? WHERE borrow_id = ?";
            PreparedStatement updateBorrowStmt = conn.prepareStatement(updateBorrowQuery);
            updateBorrowStmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            updateBorrowStmt.setInt(2, borrowId);
            updateBorrowStmt.executeUpdate();

            loadAvailableBooks();
            loadBorrowedBooks(); // Refresh both tables
            tabbedPane.setSelectedIndex(0); // Switch to Available Books tab
            JOptionPane.showMessageDialog(this, "Book returned successfully!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error returning book: " + ex.getMessage());
        }
    }
}