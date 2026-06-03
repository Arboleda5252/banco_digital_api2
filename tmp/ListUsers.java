import java.sql.*;

public class ListUsers {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:h2:file:./data/pancodb";
        String user = "SA";
        String password = "";
        
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT name, email, password FROM users");
        
        System.out.println("Cuentas Encontradas:");
        while (rs.next()) {
            System.out.println("Nombre: " + rs.getString("name"));
            System.out.println("Email: " + rs.getString("email"));
            System.out.println("Password (Hash): " + rs.getString("password"));
            System.out.println("---");
        }
        conn.close();
    }
}
