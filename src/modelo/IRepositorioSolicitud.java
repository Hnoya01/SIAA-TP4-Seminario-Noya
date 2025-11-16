package modelo;

import java.util.List;

public interface IRepositorioSolicitud {
    void crear(Solicitud s) throws Exception;
    List<Solicitud> listar() throws Exception;
    int actualizarEstado(int id, String estado, String observacion) throws Exception;
}
