package persistencia;

import modelo.ItemSolicitud;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemSolicitudDAO {

    public void crear(ItemSolicitud it) throws Exception {
        String sql = "INSERT INTO item_solicitud (id_solicitud, codigo, descripcion, cantidad, precio_unit) " +
                "VALUES (?,?,?,?,?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1,  it.getSolicitudId());
            ps.setString(2, it.getCodigo());
            ps.setString(3, it.getDescripcion());
            ps.setInt(4,    it.getCantidad());
            ps.setDouble(5, it.getPrecioUnit());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) it.setId(rs.getInt(1));
            }
        }
    }

    public List<ItemSolicitud> listarPorSolicitud(int solicitudId) throws Exception {
        String sql = "SELECT id, id_solicitud, codigo, descripcion, cantidad, precio_unit " +
                "FROM item_solicitud WHERE id_solicitud = ? ORDER BY id";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, solicitudId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ItemSolicitud> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ItemSolicitud(
                            rs.getInt("id"),
                            rs.getInt("id_solicitud"),
                            rs.getString("codigo"),
                            rs.getString("descripcion"),
                            rs.getInt("cantidad"),
                            rs.getDouble("precio_unit")
                    ));
                }
                return out;
            }
        }
    }

    public double totalPorSolicitud(int solicitudId) throws Exception {
        String sql = "SELECT COALESCE(SUM(cantidad * precio_unit),0) AS total " +
                "FROM item_solicitud WHERE id_solicitud = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitudId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
                return 0.0;
            }
        }
    }
}

