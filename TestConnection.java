import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:6543/postgres?user=postgres.zqjxmrtezichbtqbjpcy&password=SomosGrupo2";
        System.out.println("Testing " + url);
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("SUCCESS!");
        } catch (SQLException e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }
}
