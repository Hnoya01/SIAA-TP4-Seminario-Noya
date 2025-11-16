package persistencia;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Conexion {

    private static final String PROP_FILE = "/config.properties"; // en resources (src/main/resources)

    private static String url;
    private static String user;
    private static String pass;

    static {
        try (InputStream in = Conexion.class.getResourceAsStream(PROP_FILE)) {
            if (in == null) throw new RuntimeException("No se encontró " + PROP_FILE);
            Properties p = new Properties();
            p.load(in);
            url  = p.getProperty("db.url");
            user = p.getProperty("db.user");
            pass = p.getProperty("db.pass");
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar configuración de la base de datos", e);
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, user, pass);
    }
}
