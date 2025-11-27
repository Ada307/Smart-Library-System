import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class AdminDashboard extends JFrame {
    private DefaultTableModel librarianModel;
    private DefaultTableModel userModel;
    private JTable librarianTable;
    private JTable userTable;
    private String loggedInUsername;
    private JTabbedPane tabbedPane;

    public AdminDashboard(String username) {
        this.loggedInUsername = username;
        setTitle("Admin Dashboard");
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

        // Tabbed pane for different admin features
        tabbedPane = new JTabbedPane();

        // Manage Librarians tab
        String[] librarianColumns = {"Username", "Password"};
        librarianModel = new DefaultTableModel(librarianColumns, 0);
        librarianTable = new JTable(librarianModel);
        loadLibrarians();
        JScrollPane librarianScrollPane = new JScrollPane(librarianTable);
        JPanel librarianPanel = new JPanel(new BorderLayout());
        JPanel librarianButtonPanel = new JPanel(new FlowLayout());
        JButton addLibrarianButton = new JButton("Add Librarian");
        JButton updateLibrarianButton = new JButton("Update Librarian");
        JButton deleteLibrarianButton = new JButton("Delete Librarian");
        librarianButtonPanel.add(addLibrarianButton);
        librarianButtonPanel.add(updateLibrarianButton);
        librarianButtonPanel.add(deleteLibrarianButton);
        librarianPanel.add(librarianScrollPane, BorderLayout.CENTER);
        librarianPanel.add(librarianButtonPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Manage Librarians", librarianPanel);

        // User Account Management tab
        String[] userColumns = {"Username", "Password", "Role"};
        userModel = new DefaultTableModel(userColumns, 0);
        userTable = new JTable(userModel);
        loadUsers();
        JScrollPane userScrollPane = new JScrollPane(userTable);
        JPanel userPanel = new JPanel(new BorderLayout());
        JPanel userButtonPanel = new JPanel(new FlowLayout());
        JButton addUserButton = new JButton("Add User");
        JButton updateUserButton = new JButton("Update User");
        JButton deleteUserButton = new JButton("Delete User");
        userButtonPanel.add(addUserButton);
        userButtonPanel.add(updateUserButton);
        userButtonPanel.add(deleteUserButton);
        userPanel.add(userScrollPane, BorderLayout.CENTER);
        userPanel.add(userButtonPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("User Account Management", userPanel);

        // View Reports tab
        JPanel reportPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        reportPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        reportPanel.add(new JLabel("Total Books:"));
        JLabel totalBooksLabel = new JLabel("0");
        reportPanel.add(totalBooksLabel);
        reportPanel.add(new JLabel("Books Borrowed:"));
        JLabel borrowedBooksLabel = new JLabel("0");
        reportPanel.add(borrowedBooksLabel);
        reportPanel.add(new JLabel("Total Users:"));
        JLabel totalUsersLabel = new JLabel("0");
        reportPanel.add(totalUsersLabel);
        JButton refreshButton = new JButton("Refresh Reports");
        reportPanel.add(refreshButton);
        tabbedPane.addTab("View Reports", reportPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);

        // Logout button action
        logoutButton.addActionListener(e -> {
            new LoginScreen().setVisible(true);
            dispose();
        });

        // Manage Librarians actions
        addLibrarianButton.addActionListener(e -> showAddLibrarianDialog());
        updateLibrarianButton.addActionListener(e -> showUpdateLibrarianDialog());
        deleteLibrarianButton.addActionListener(e -> deleteLibrarian());

        // User Account Management actions
        addUserButton.addActionListener(e -> showAddUserDialog());
        updateUserButton.addActionListener(e -> showUpdateUserDialog());
        deleteUserButton.addActionListener(e -> deleteUser());

        // Refresh reports action
        refreshButton.addActionListener(e -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books");
                rs.next();
                totalBooksLabel.setText(String.valueOf(rs.getInt(1)));

                rs = stmt.executeQuery("SELECT COUNT(*) FROM borrowed_books WHERE return_date IS NULL");
                rs.next();
                borrowedBooksLabel.setText(String.valueOf(rs.getInt(1)));

                rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                rs.next();
                totalUsersLabel.setText(String.valueOf(rs.getInt(1)));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error loading reports: " + ex.getMessage());
            }
        });

        // Load reports on startup
        refreshButton.doClick();
    }

    private void loadLibrarians() {
        librarianModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT username, password FROM users WHERE role = 'Librarian'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("username"));
                row.add(rs.getString("password"));
                librarianModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading librarians: " + e.getMessage());
        }
    }

    private void showAddLibrarianDialog() {
        JDialog dialog = new JDialog(this, "Add Librarian", true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(3, 2, 5, 5));

        dialog.add(new JLabel("Username:"));
        JTextField usernameField = new JTextField();
        dialog.add(usernameField);

        dialog.add(new JLabel("Password:"));
        JTextField passwordField = new JTextField();
        dialog.add(passwordField);

        JButton saveButton = new JButton("Save");
        dialog.add(saveButton);
        JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);

        saveButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields!");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "INSERT INTO users (username, password, role) VALUES (?, ?, 'Librarian')";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.executeUpdate();
                loadLibrarians();
                JOptionPane.showMessageDialog(dialog, "Librarian added successfully!");
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding librarian: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showUpdateLibrarianDialog() {
        int selectedRow = librarianTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a librarian to update!");
            return;
        }

        String currentUsername = (String) librarianModel.getValueAt(selectedRow, 0);
        String currentPassword = (String) librarianModel.getValueAt(selectedRow, 1);

        JDialog dialog = new JDialog(this, "Update Librarian", true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(3, 2, 5, 5));

        dialog.add(new JLabel("Username:"));
        JTextField usernameField = new JTextField(currentUsername);
        usernameField.setEditable(false); // Username can't be changed
        dialog.add(usernameField);

        dialog.add(new JLabel("Password:"));
        JTextField passwordField = new JTextField(currentPassword);
        dialog.add(passwordField);

        JButton saveButton = new JButton("Save");
        dialog.add(saveButton);
        JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);

        saveButton.addActionListener(e -> {
            String password = passwordField.getText();
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in the password field!");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "UPDATE users SET password = ? WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, password);
                stmt.setString(2, currentUsername);
                stmt.executeUpdate();
                loadLibrarians();
                JOptionPane.showMessageDialog(dialog, "Librarian updated successfully!");
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating librarian: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void deleteLibrarian() {
        int selectedRow = librarianTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a librarian to delete!");
            return;
        }

        String username = (String) librarianModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this librarian?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "DELETE FROM users WHERE username = ? AND role = 'Librarian'";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.executeUpdate();
                loadLibrarians();
                JOptionPane.showMessageDialog(this, "Librarian deleted successfully!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting librarian: " + ex.getMessage());
            }
        }
    }

    private void loadUsers() {
        userModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT username, password, role FROM users";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("username"));
                row.add(rs.getString("password"));
                row.add(rs.getString("role"));
                userModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage());
        }
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "Add User", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(4, 2, 5, 5));

        dialog.add(new JLabel("Username:"));
        JTextField usernameField = new JTextField();
        dialog.add(usernameField);

        dialog.add(new JLabel("Password:"));
        JTextField passwordField = new JTextField();
        dialog.add(passwordField);

        dialog.add(new JLabel("Role:"));
        String[] roles = {"Admin", "Librarian", "Student"};
        JComboBox<String> roleComboBox = new JComboBox<>(roles);
        dialog.add(roleComboBox);

        JButton saveButton = new JButton("Save");
        dialog.add(saveButton);
        JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);

        saveButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = (String) roleComboBox.getSelectedItem();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields!");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, role);
                stmt.executeUpdate();
                loadUsers();
                JOptionPane.showMessageDialog(dialog, "User added successfully!");
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding user: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showUpdateUserDialog() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update!");
            return;
        }

        String currentUsername = (String) userModel.getValueAt(selectedRow, 0);
        String currentPassword = (String) userModel.getValueAt(selectedRow, 1);
        String currentRole = (String) userModel.getValueAt(selectedRow, 2);

        JDialog dialog = new JDialog(this, "Update User", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(4, 2, 5, 5));

        dialog.add(new JLabel("Username:"));
        JTextField usernameField = new JTextField(currentUsername);
        usernameField.setEditable(false);
        dialog.add(usernameField);

        dialog.add(new JLabel("Password:"));
        JTextField passwordField = new JTextField(currentPassword);
        dialog.add(passwordField);

        dialog.add(new JLabel("Role:"));
        String[] roles = {"Admin", "Librarian", "Student"};
        JComboBox<String> roleComboBox = new JComboBox<>(roles);
        roleComboBox.setSelectedItem(currentRole);
        dialog.add(roleComboBox);

        JButton saveButton = new JButton("Save");
        dialog.add(saveButton);
        JButton cancelButton = new JButton("Cancel");
        dialog.add(cancelButton);

        saveButton.addActionListener(e -> {
            String password = passwordField.getText();
            String role = (String) roleComboBox.getSelectedItem();
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in the password field!");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "UPDATE users SET password = ?, role = ? WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, password);
                stmt.setString(2, role);
                stmt.setString(3, currentUsername);
                stmt.executeUpdate();
                loadUsers();
                JOptionPane.showMessageDialog(dialog, "User updated successfully!");
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating user: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete!");
            return;
        }

        String username = (String) userModel.getValueAt(selectedRow, 0);
        if (username.equals(loggedInUsername)) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own account!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "DELETE FROM users WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.executeUpdate();
                loadUsers();
                JOptionPane.showMessageDialog(this, "User deleted successfully!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting user: " + ex.getMessage());
            }
        }
    }
}