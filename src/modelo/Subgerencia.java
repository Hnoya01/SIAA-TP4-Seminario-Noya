package modelo;

public class Subgerencia extends Usuario {
    private String unidad;
    public Subgerencia(String nombre, String unidad) {
        super(nombre, "Subgerencia");
        this.unidad = unidad;
    }
    public String getUnidad() { return unidad; }
    @Override public void mostrarDatos() { /* noop */ }
}
