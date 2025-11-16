package modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Solicitud {

    private Integer id;
    private String afiliado;
    private long dni;
    private String tipoPractica;
    private int cantidad;
    private LocalDate fecha;
    private String estado;         // PENDIENTE, EN_EVALUACION, SOLICITAR_DOC, EN_CORRECCION, ELEVADA, APROBADA, RECHAZADA, ANULADA, INFORMADA, ARCHIVADA
    private String observacion;    // motivo de rechazo o notas

    // Ítems detallados
    private List<ItemSolicitud> items = new ArrayList<>();

    public Solicitud(Integer id, String afiliado, long dni, String tipoPractica,
                     int cantidad, LocalDate fecha, String estado) {
        this.id = id;
        this.afiliado = afiliado;
        this.dni = dni;
        this.tipoPractica = tipoPractica;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.estado = estado;
    }
    public Solicitud(String afiliado, long dni, String tipoPractica,
                     int cantidad, LocalDate fecha, String estado) {
        this(null, afiliado, dni, tipoPractica, cantidad, fecha, estado);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getAfiliado() { return afiliado; }
    public void setAfiliado(String afiliado) { this.afiliado = afiliado; }

    public long getDni() { return dni; }
    public void setDni(long dni) { this.dni = dni; }

    public String getTipoPractica() { return tipoPractica; }
    public void setTipoPractica(String tipoPractica) { this.tipoPractica = tipoPractica; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public List<ItemSolicitud> getItems() { return items; }
    public void setItems(List<ItemSolicitud> items) { this.items = items; }

    public double getTotal() {
        return items.stream().mapToDouble(ItemSolicitud::getSubtotal).sum();
    }
}

