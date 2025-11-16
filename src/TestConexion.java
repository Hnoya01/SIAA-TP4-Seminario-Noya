import persistencia.Conexion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConexion {
    public static void main(String[] args) {
        try (Connection cn = Conexion.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE()")) {

            rs.next();
            System.out.println("OK MySQL. BD: " + rs.getString(1));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

