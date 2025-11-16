import controlador.SolicitudController;
import controlador.ValidacionException;
import persistencia.SolicitudDAO;
import java.time.LocalDate;

public class TestController {
    public static void main(String[] args) {
        try {
            SolicitudController controller = new SolicitudController(new SolicitudDAO());
            controller.crear(
                    "María López",
                    "36500222",          // DNI NUMÉRICO
                    "Ecografía",
                    1,                   // cantidad
                    LocalDate.now(),
                    "PENDIENTE"
            );
            System.out.println("Alta OK. Total registros: " + controller.listar().size());
        } catch (ValidacionException ve) {
            System.out.println("Validación: " + ve.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

