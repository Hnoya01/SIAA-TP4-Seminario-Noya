package modelo;

public class Administrativo extends Usuario {
    private String sector;
    public Administrativo(String nombre, String sector) {
        super(nombre, "Administrativo");
        this.sector = sector;
    }
    public String getSector() { return sector; }
    @Override public void mostrarDatos() { /* noop */ }
}
