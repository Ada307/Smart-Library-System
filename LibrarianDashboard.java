import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;

public class LibrarianDashboard extends JFrame {
    private DefaultTableModel tableModel;
    private JTable bookTable;
    private String loggedInUsername;

    public LibrarianDashboard(String username) {
        this.loggedInUsername = username;
        setTitle("Librarian Dashboard");
        setSize(600, 500);
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

        // Center panel for book table
        String[] columnNames = {"Book ID", "Title", "Author", "ISBN", "Available"};
        tableModel = new DefaultTableModel(columnNames, 0);
        bookTable = new JTable(tableModel);
        loadBooks();
        JScrollPane scrollPane = new JScrollPane(bookTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add Book");
        JButton updateButton = new JButton("Update Book");
        JButton deleteButton = new JButton("Delete Book");
        JButton issueButton = new JButton("Issue Books");
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(issueButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Logout button action
        logoutButton.addActionListener(e -> {
            new LoginScreen().setVisible(true);
            dispose();
        });

        // Add book button action
        addButton.addActionListener(e -> showAddBookDialog());

        // Update book button action
        updateButton.addActionListener(e -> showUpdateBookDialog());

        // Delete book button action
        deleteButton.addActionListener(e -> deleteBook());

        // Issue books button action
        issueButton.addActionListener(e -> showIssueBookDialog());
    }

    private void loadBooks() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM books";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("book_id"));
                row.add(rs.getString("title"));
                row.add(rs.getString("author"));
                row.add(rs.getString("isbn"));
                row.add(rs.getBoolean("is_available") ? "Yes" : "No");
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading books: " + e.getMessage());
        }
    }

    private void showAddBookDialog() {
        JDialog dialog = new JDialog(this, "Add Book", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(4, 2, 5, 5));

        dialog.add(new JLabel("Title:"));
        JTextField titleField = new JTextField();
        dialog.add(titleField);

        dialog.add(new JLabel("Author:"));
        JTextField authorField = new JTextField();
        dialog.add(authorField);

        dialog.add(new JLabel("ISBN:"));
        JTextField isbnField = new JTextField();
        dialog.add(isbnField);

        JButton saveButton = new JButton("Save");
        dialog.add(saveButton);
        JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);

        saveButton.addActionListener(e -> {
            String title = titleField.getText();
            String author = authorField.getText();
            String isbn = isbnField.getText();
            if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields!");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "INSERT INTO books (title, author, isbn, is_available) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, title);
                stmt.setString(2, author);
                stmt.setString(3, isbn);
                stmt.setBoolean(4, true);
                stmt.executeUpdate();
                loadBooks();
                JOptionPane.showMessageDialog(dialog, "Book added successfully!");
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding book: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showUpdateBookDialog() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to update!");
            return;
        }

        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String currentTitle = (String) tableModel.getValueAt(selectedRow, 1);
        String currentAuthor = (String) tableModel.getValueAt(selectedRow, 2);
        String currentIsbn = (String) tableModel.getValueAt(selectedRow, 3);

        JDialog dialog = new JDialog(this, "Update Book", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(4, 2, 5, 5));

        dialog.add(new JLabel("Title:"));
        JTextField titleField = new JTextField(currentTitle);
        dialog.add(titleField);

        dialog.add(new JLabel("Author:"));
        JTextField authorField = new JTextField(currentAuthor);
        dialog.add(authorField);

        dialog.add(new JLabel("ISBN:"));
        JTextField isbnField = new JTextField(currentIsbn);
        dialog.add(isbnField);

        JButton saveButton = new JButton("Save");
        dialog.add(saveButton);
        JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);

        saveButton.addActionListener(e -> {
            String title = titleField.getText();
            String author = authorField.getText();
            String isbn = isbnField.getText();
            if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields!");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "UPDATE books SET title = ?, author = ?, isbn = ? WHERE book_id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, title);
                stmt.setString(2, author);
                stmt.setString(3, isbn);
                stmt.setInt(4, bookId);
                stmt.executeUpdate();
                loadBooks();
                JOptionPane.showMessageDialog(dialog, "Book updated successfully!");
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating book: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void deleteBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to delete!");
            return;
        }

        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this book?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "DELETE FROM books WHERE book_id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, bookId);
                stmt.executeUpdate();
                loadBooks();
                JOptionPane.showMessageDialog(this, "Book deleted successfully!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting book: " + ex.getMessage());
            }
        }
    }

    private void showIssueBookDialog() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to issue!");
            return;
        }

        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String availability = (String) tableModel.getValueAt(selectedRow, 4);
        if (!availability.equals("Yes")) {
            JOptionPane.showMessageDialog(this, "This book is not available to issue!");
            return;
        }

        JDialog dialog = new JDialog(this, "Issue Book", true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(2, 2, 5, 5));

        dialog.add(new JLabel("Student Username:"));
        JTextField usernameField = new JTextField();
        dialog.add(usernameField);

        JButton issueButton = new JButton("Issue");
        dialog.add(issueButton);
        JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);

        issueButton.addActionListener(e -> {
            String studentUsername = usernameField.getText();
            if (studentUsername.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a student username!");
                return;
            }

            // Check if the username exists and is a student
            try (Connection conn = DatabaseConnection.getConnection()) {
                String checkUserQuery = "SELECT * FROM users WHERE username = ? AND role = 'Student'";
                PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery);
                checkStmt.setString(1, studentUsername);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(dialog, "Invalid student username!");
                    return;
                }

                // Update the book's availability
                String updateBookQuery = "UPDATE books SET is_available = FALSE WHERE book_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateBookQuery);
                updateStmt.setInt(1, bookId);
                updateStmt.executeUpdate();

                // Record the borrowing in borrowed_books
                String insertQuery = "INSERT INTO borrowed_books (book_id, username, borrow_date) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setInt(1, bookId);
                insertStmt.setString(2, studentUsername);
                insertStmt.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                insertStmt.executeUpdate();

                loadBooks();
                JOptionPane.showMessageDialog(dialog, "Book issued successfully!");
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error issuing book: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }
}