import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection{
    private static final String URL="jdbc:mysql://localhost:3306/library_system";
    private static final String USER="root"; // Replace with your MySQL username
    private static final String PASSWORD="Ada0610"; // Replace with your MySQL password

    public static Connection getConnection(){
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            return null;
        }
    }
}