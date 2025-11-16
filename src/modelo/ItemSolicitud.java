package modelo;

public class ItemSolicitud {
    private Integer id;
    private Integer solicitudId;
    private String  codigo;
    private String  descripcion;
    private int     cantidad;
    private double  precioUnit;


    public ItemSolicitud(Integer solicitudId, String codigo, String descripcion, int cantidad, double precioUnit) {
        this.solicitudId = solicitudId;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnit = precioUnit;
    }

    // Constructor completo
    public ItemSolicitud(Integer id, Integer solicitudId, String codigo, String descripcion, int cantidad, double precioUnit) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnit = precioUnit;
    }

    // Getters / Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Integer solicitudId) { this.solicitudId = solicitudId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnit() { return precioUnit; }
    public void setPrecioUnit(double precioUnit) { this.precioUnit = precioUnit; }

    public double getSubtotal() { return cantidad * precioUnit; }
}
