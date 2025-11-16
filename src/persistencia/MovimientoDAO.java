package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class MovimientoDAO {

    public void log(int solicitudId,
                    String accion,
                    String estadoAnterior,
                    String estadoNuevo,
                    String usuario,
                    String detalle) throws Exception {

        String sql = "INSERT INTO movimientos (id_solicitud, accion, estado_anterior, estado_nuevo, usuario, detalle) " +
                "VALUES (?,?,?,?,?,?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, solicitudId);
            ps.setString(2, accion);
            ps.setString(3, estadoAnterior);
            ps.setString(4, estadoNuevo);
            ps.setString(5, usuario);
            ps.setString(6, detalle);
            ps.executeUpdate();
        }
    }
}
