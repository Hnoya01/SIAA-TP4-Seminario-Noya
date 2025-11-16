package modelo;

public abstract class Usuario {
    protected String nombre;
    protected String rol; // "Administrativo", "Médico", "Subgerencia"

    public Usuario(String nombre, String rol) {
        this.nombre = nombre;
        this.rol = rol;
    }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public abstract void mostrarDatos();
}
