package modelo;

public class Medico extends Usuario {
    private String matricula;
    public Medico(String nombre, String matricula) {
        super(nombre, "Médico");
        this.matricula = matricula;
    }
    public String getMatricula() { return matricula; }
    @Override public void mostrarDatos() { /* noop */ }
}
