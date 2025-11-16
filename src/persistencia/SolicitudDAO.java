package persistencia;

import modelo.IRepositorioSolicitud;
import modelo.Solicitud;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SolicitudDAO implements IRepositorioSolicitud {

    @Override
    public void crear(Solicitud s) throws Exception {
        String sql = "INSERT INTO solicitudes " +
                "(afiliado, dni, tipo_practica, cantidad, fecha, estado, observacion) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, s.getAfiliado());
            ps.setLong(2, s.getDni());
            ps.setString(3, s.getTipoPractica());
            ps.setInt(4, s.getCantidad());
            ps.setDate(5, Date.valueOf(s.getFecha()));
            ps.setString(6, s.getEstado());
            ps.setString(7, s.getObservacion());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) s.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new Exception("Error al crear solicitud: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Solicitud> listar() throws Exception {
        List<Solicitud> lista = new ArrayList<>();
        String sql = "SELECT * FROM solicitudes ORDER BY id DESC";
        try (Connection cn = Conexion.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Solicitud s = new Solicitud(
                        rs.getInt("id"),
                        rs.getString("afiliado"),
                        rs.getLong("dni"),
                        rs.getString("tipo_practica"),
                        rs.getInt("cantidad"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getString("estado")
                );
                s.setObservacion(rs.getString("observacion"));
                lista.add(s);
            }
        } catch (SQLException e) {
            throw new Exception("Error al listar solicitudes: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public int actualizarEstado(int id, String estado, String observacion) throws Exception {
        String sql = "UPDATE solicitudes SET estado=?, observacion=? WHERE id=?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setString(2, observacion);
            ps.setInt(3, id);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new Exception("Error al actualizar estado: " + e.getMessage(), e);
        }
    }


    public int actualizarEstado(int id, String estado) throws Exception {
        return actualizarEstado(id, estado, null);
    }
}

