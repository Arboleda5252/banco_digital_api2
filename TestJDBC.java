import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestJDBC {
    public static void main(String[] args) {
        String[] urls = {
            "jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:6543/postgres?user=postgres.zqjxmrtezichbtqbjpcy",
            "jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:5432/postgres?user=postgres.zqjxmrtezichbtqbjpcy",
            "jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:6543/postgres?user=postgres",
            "jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:5432/postgres?user=postgres"
        };
        for (String url : urls) {
            System.out.println("Testing " + url);
            try (Connection conn = DriverManager.getConnection(url, null, "SomosGrupo2")) {
                System.out.println("Success with " + url);
            } catch (SQLException e) {
                System.out.println("Failed: " + e.getMessage());
            }
        }
    }
}
