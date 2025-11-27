import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginScreen extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;

    public LoginScreen() {
        // Set up the window
        setTitle("Library Management System - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // Create main panel
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add components
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel("Role:"));
        String[] roles = {"Admin", "Librarian", "Student"};
        roleComboBox = new JComboBox<>(roles);
        panel.add(roleComboBox);

        JButton loginButton = new JButton("Login");
        panel.add(loginButton);

        JButton forgotPasswordButton = new JButton("Forgot Password");
        panel.add(forgotPasswordButton);

        // Add panel to frame
        add(panel);

        // Login button action
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                String role = (String) roleComboBox.getSelectedItem();

                if (validateLogin(username, password, role)) {
                    JOptionPane.showMessageDialog(null, "Login successful!");
                    openDashboard(username, role); // Pass the username
                    dispose(); // Close login window
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid credentials!");
                }
            }
        });

        // Forgot password button (placeholder)
        forgotPasswordButton.addActionListener(e -> JOptionPane.showMessageDialog(null, "Contact admin to reset password."));
    }

    private boolean validateLogin(String username, String password, String role) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // True if a matching user is found
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
            return false;
        }
    }

    private void openDashboard(String username, String role) {
        switch (role) {
            case "Admin":
                new AdminDashboard(username).setVisible(true);
                break;
            case "Librarian":
                new LibrarianDashboard(username).setVisible(true);
                break;
            case "Student":
                new StudentDashboard(username).setVisible(true);
                break;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }
}