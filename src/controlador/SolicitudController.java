package controlador;

import modelo.IRepositorioSolicitud;
import modelo.ItemSolicitud;
import modelo.Solicitud;
import modelo.Usuario;
import persistencia.ItemSolicitudDAO;
import persistencia.MovimientoDAO;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Controlador principal del flujo de autorizaciones.
 */
public class SolicitudController {

    // ======================
    // Estados y flujo
    // ======================
    public static final String PENDIENTE      = "PENDIENTE";
    public static final String EN_EVALUACION  = "EN_EVALUACION";
    public static final String SOLICITAR_DOC  = "SOLICITAR_DOC";
    public static final String EN_CORRECCION  = "EN_CORRECCION";
    public static final String ELEVADA        = "ELEVADA";
    public static final String APROBADA       = "APROBADA";
    public static final String RECHAZADA      = "RECHAZADA";
    public static final String ANULADA        = "ANULADA";
    public static final String INFORMADA      = "INFORMADA";
    public static final String ARCHIVADA      = "ARCHIVADA";

    public static final String[] ESTADOS = {
            PENDIENTE, EN_EVALUACION, SOLICITAR_DOC, EN_CORRECCION,
            ELEVADA, APROBADA, RECHAZADA, ANULADA, INFORMADA, ARCHIVADA
    };

    // destinos permitidos
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            PENDIENTE,       Set.of(EN_EVALUACION, SOLICITAR_DOC, ANULADA),
            EN_EVALUACION,   Set.of(APROBADA, RECHAZADA, SOLICITAR_DOC, ELEVADA, ANULADA),
            SOLICITAR_DOC,   Set.of(EN_CORRECCION, ANULADA),
            EN_CORRECCION,   Set.of(EN_EVALUACION, ANULADA),
            ELEVADA,         Set.of(APROBADA, RECHAZADA, ANULADA),
            APROBADA,        Set.of(INFORMADA, ANULADA),
            RECHAZADA,       Set.of(INFORMADA),
            INFORMADA,       Set.of(ARCHIVADA),
            ARCHIVADA,       Set.of(),
            ANULADA,         Set.of()
    );


    // ======================
    // Dependencias
    // ======================
    private final IRepositorioSolicitud repo;
    private final MovimientoDAO  movDAO  = new MovimientoDAO();
    private final ItemSolicitudDAO itemDAO = new ItemSolicitudDAO();

    // Sesión (rol por defecto)
    private Usuario usuarioActual = null;
    public void setUsuarioActual(Usuario u) { this.usuarioActual = u; }
    public Usuario getUsuarioActual()       { return usuarioActual; }
    private String nombreUsuario()          { return usuarioActual == null ? "system" : usuarioActual.getNombre(); }
    private String rolUsuario()             { return usuarioActual == null ? "system" : usuarioActual.getRol(); }

    // Reglas de negocio (ejemplo)
    private static final Map<String, Integer> TOPES = Map.of(
            "Kinesiologia", 25,
            "Psicologia",   30
    );
    // si el total alcanza este valor, el médico debe elevar (no aprobar)
    private static final double UMBRAL_ELEVACION = 200_000.0;



    // ======================
    public SolicitudController(IRepositorioSolicitud repo) {
        this.repo = repo;
    }


    // Alta simple
    // ======================
    public void crear(String afiliado, String dniStr, String tipo, int cantidad,
                      LocalDate fecha, String estadoIgnorado)
            throws ValidacionException, Exception {

        validarAlta(afiliado, dniStr, tipo, cantidad, fecha);
        long dni = Long.parseLong(dniStr);

        Solicitud s = new Solicitud(afiliado, dni, tipo, cantidad, fecha, PENDIENTE);
        repo.crear(s);

        if (s.getId() != null) {
            movDAO.log(s.getId(), "CREAR", null, PENDIENTE, nombreUsuario(), null);
        }
    }

    // ======================
    // Alta con ítems (encabezado + detalle)
    // ======================
    public void crearConItems(String afiliado, String dniStr, String tipo, int cantidad,
                              LocalDate fecha, List<ItemSolicitud> items)
            throws ValidacionException, Exception {

        validarAlta(afiliado, dniStr, tipo, cantidad, fecha);

        if (items == null || items.isEmpty())
            throw new ValidacionException("Agregá al menos un ítem a la solicitud antes de guardar.");
        for (var it : items) {
            if (it.getCodigo() == null || it.getCodigo().isBlank())
                throw new ValidacionException("Ítem sin código.");
            if (it.getCantidad() <= 0)
                throw new ValidacionException("Ítem con cantidad inválida.");
            if (it.getPrecioUnit() < 0)
                throw new ValidacionException("Ítem con precio inválido.");
        }

        long dni = Long.parseLong(dniStr);

        var s = new Solicitud(afiliado, dni, tipo, cantidad, fecha, PENDIENTE);
        repo.crear(s);
        if (s.getId() == null) throw new Exception("No se obtuvo ID de la nueva solicitud.");

        movDAO.log(s.getId(), "CREAR", null, PENDIENTE, nombreUsuario(), null);

        for (var it : items) {
            it.setSolicitudId(s.getId());
            itemDAO.crear(it);
            movDAO.log(
                    s.getId(), "AGREGAR_ITEM",
                    PENDIENTE, PENDIENTE,
                    nombreUsuario(),
                    String.format("Código=%s Cant=%d PU=%.2f", it.getCodigo(), it.getCantidad(), it.getPrecioUnit())
            );
        }
    }

    // ======================
    // Listado
    // ======================
    public List<Solicitud> listar() throws Exception {
        return repo.listar();
    }

    // ======================
    // Ítems
    // ======================
    public void agregarItem(int solicitudId, String codigo, String descripcion, int cantidad, double precioUnit)
            throws ValidacionException, Exception {

        exigirRol("Administrativo");
        if (codigo == null || codigo.isBlank())
            throw new ValidacionException("Código del ítem obligatorio.");
        if (cantidad <= 0) throw new ValidacionException("Cantidad inválida.");
        if (precioUnit < 0) throw new ValidacionException("Precio inválido.");

        ItemSolicitud it = new ItemSolicitud(solicitudId, codigo, descripcion, cantidad, precioUnit);
        itemDAO.crear(it);

        String estado = estadoActual(solicitudId);
        movDAO.log(solicitudId, "AGREGAR_ITEM", estado, estado, nombreUsuario(),
                String.format("Código=%s Cant=%d PU=%.2f", codigo, cantidad, precioUnit));
    }

    public List<ItemSolicitud> listarItems(int solicitudId) throws Exception {
        return itemDAO.listarPorSolicitud(solicitudId);
    }

    public double totalDeSolicitud(int solicitudId) throws Exception {
        return itemDAO.totalPorSolicitud(solicitudId);
    }

    // ======================
    // Flujo (acciones)
    // ======================
    public void iniciarEvaluacion(int id) throws Exception {
        exigirRol("Administrativo", "Médico");
        cambiarEstado(id, EN_EVALUACION, null);
    }

    public void solicitarDocumentacion(int id, String detalle) throws Exception {
        exigirRol("Médico", "Administrativo");
        cambiarEstado(id, SOLICITAR_DOC, (detalle == null || detalle.isBlank()) ? "Solicitar docs" : detalle.trim());
    }

    public void recibirCorreccion(int id) throws Exception {
        exigirRol("Administrativo");
        cambiarEstado(id, EN_CORRECCION, null);
    }

    public void volverAEvaluacion(int id) throws Exception {
        exigirRol("Administrativo");
        cambiarEstado(id, EN_EVALUACION, "Reingresa a evaluación");
    }

    /**
     * Autoriza si:
     *  - el rol permite, y
     *  - el total NO supera el umbral. Si lo supera y el rol es Médico, ELEVA automáticamente.
     */
    public void autorizar(int id) throws Exception {
        exigirRol("Médico", "Subgerencia");

        String actual = estadoActual(id);
        if (APROBADA.equals(actual)) {
            throw new ValidacionException("La solicitud ya está APROBADA.");
        }

        if ("Médico".equalsIgnoreCase(getUsuarioActual().getRol())) {
            double total = totalDeSolicitud(id);
            if (total >= UMBRAL_ELEVACION) {
                elevar(id, total);
                throw new ValidacionException(
                        "El total ($" + String.format("%.2f", total) + ") supera el umbral: la solicitud fue ELEVADA."
                );
            }
        }

        cambiarEstado(id, APROBADA, null);
    }

    public void rechazar(int id, String motivo) throws Exception {
        exigirRol("Médico", "Subgerencia");
        if (motivo == null || motivo.isBlank())
            throw new ValidacionException("Indicá el motivo del rechazo.");
        cambiarEstado(id, RECHAZADA, motivo.trim());
    }

    public void anular(int id, String motivo) throws Exception {
        exigirRol("Administrativo", "Médico", "Subgerencia");
        cambiarEstado(id, ANULADA, motivo);
    }

    /**
     * Eleva asegurando transiciones válidas:
     *  - Si está en PENDIENTE, primero pasa a EN_EVALUACION y luego a ELEVADA.
     *  - Si ya está en EN_EVALUACION, va directo a ELEVADA.
     */
    public void elevar(int id, double totalEstimado) throws Exception {
        exigirRol("Médico");

        if (totalEstimado < UMBRAL_ELEVACION) {
            throw new ValidacionException("No supera el umbral de elevación.");
        }

        String actual = estadoActual(id);

        // paso intermedio
        if (PENDIENTE.equals(actual)) {
            repo.actualizarEstado(id, EN_EVALUACION, "Paso auto previo a elevación por monto");
            movDAO.log(id, "EVALUAR", PENDIENTE, EN_EVALUACION, nombreUsuario(),
                    "Auto: previo a ELEVADA por superar umbral");
            actual = EN_EVALUACION;
        }

        validarTransicion(actual, ELEVADA);
        repo.actualizarEstado(id, ELEVADA, "Total estimado: " + totalEstimado);
        movDAO.log(id, "ELEVAR", actual, ELEVADA, nombreUsuario(),
                "Total estimado: " + totalEstimado);
    }

    public void dictaminarAprobar(int id) throws Exception {
        exigirRol("Subgerencia");
        cambiarEstado(id, APROBADA, "Dictamen favorable");
    }

    public void dictaminarRechazar(int id, String motivo) throws Exception {
        exigirRol("Subgerencia");
        if (motivo == null || motivo.isBlank())
            throw new ValidacionException("Indicá el motivo del rechazo.");
        cambiarEstado(id, RECHAZADA, motivo.trim());
    }

    public void informarAfiliado(int id) throws Exception {
        exigirRol("Administrativo");
        cambiarEstado(id, INFORMADA, "Notificado a afiliado");
    }

    public void archivar(int id) throws Exception {
        exigirRol("Administrativo");
        cambiarEstado(id, ARCHIVADA, null);
    }

    // Compatibilidad simple si alguna vista llama esto
    public void actualizarEstado(int id, String nuevo) throws Exception {
        switch (nuevo) {
            case APROBADA -> autorizar(id);
            case RECHAZADA -> throw new ValidacionException("Usá rechazar(id, motivo).");
            case PENDIENTE -> throw new ValidacionException("Volver a PENDIENTE no está en el flujo extendido.");
            default -> cambiarEstado(id, nuevo, null);
        }
    }


    // ======================
    private void validarAlta(String afiliado, String dniStr, String tipo, int cantidad, LocalDate fecha)
            throws ValidacionException {

        if (afiliado == null || afiliado.isBlank())
            throw new ValidacionException("El nombre del afiliado es obligatorio.");
            String afTrim = afiliado.trim();
            if (!afTrim.matches("[\\p{L} .'-]+")) {
            throw new ValidacionException("El nombre del afiliado no puede contener números.");
        }
        if (dniStr == null || !dniStr.matches("\\d{7,10}"))
            throw new ValidacionException("El DNI debe ser numérico (7..10 dígitos).");
        if (tipo == null || tipo.isBlank())
            throw new ValidacionException("Debe especificarse el tipo de práctica.");
        if (cantidad < 1 || cantidad > 99)
            throw new ValidacionException("La cantidad debe estar entre 1 y 99.");
        if (fecha == null)
            throw new ValidacionException("Debe ingresar una fecha válida.");

        LocalDate hoy = LocalDate.now();

        // No se permiten fechas futuras
        if (fecha.isAfter(hoy))
            throw new ValidacionException("La fecha de la orden no puede ser futura.");

        // Más de 30 días de antigüedad → vencida
        if (ChronoUnit.DAYS.between(fecha, hoy) > 30)
            throw new ValidacionException("La orden está vencida (>30 días).");

        Integer tope = TOPES.get(tipo);
        if (tope != null && cantidad > tope)
            throw new ValidacionException("La cantidad supera el tope permitido para "
                    + tipo + " (" + tope + ").");
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD);
        t = t.replaceAll("\\p{M}", "");
        return t.trim().toUpperCase();
    }

    private void exigirRol(String... roles) throws ValidacionException {
        String actual = norm(rolUsuario());
        for (String r : roles) {
            if (norm(r).equals(actual)) return;
        }
        throw new ValidacionException("Acción no permitida para el rol: " + rolUsuario());
    }

    private void validarTransicion(String origen, String destino) throws ValidacionException {
        if (!TRANSICIONES.getOrDefault(origen, Set.of()).contains(destino))
            throw new ValidacionException("Transición inválida: " + origen + " → " + destino);
    }

    private String estadoActual(int solicitudId) throws Exception {
        String sql = "SELECT estado FROM solicitudes WHERE id = ?";
        try (var con = persistencia.Conexion.getConnection();
             var ps  = con.prepareStatement(sql)) {
            ps.setInt(1, solicitudId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
                throw new Exception("Solicitud inexistente id=" + solicitudId);
            }
        }
    }

    private void cambiarEstado(int id, String nuevo, String observacion) throws Exception {
        String anterior = estadoActual(id);
        validarTransicion(anterior, nuevo);
        repo.actualizarEstado(id, nuevo, observacion);
        String accion = switch (nuevo) {
            case APROBADA       -> "APROBAR";
            case RECHAZADA      -> "RECHAZAR";
            case ANULADA        -> "ANULAR";
            case EN_EVALUACION  -> "EVALUAR";
            case SOLICITAR_DOC  -> "SOLICITAR_DOC";
            case EN_CORRECCION  -> "RECIBIR_CORR";
            case ELEVADA        -> "ELEVAR";
            case INFORMADA      -> "INFORMAR";
            case ARCHIVADA      -> "ARCHIVAR";
            default             -> "CAMBIO_ESTADO";
        };
        movDAO.log(id, accion, anterior, nuevo, nombreUsuario(), observacion);
    }
}

