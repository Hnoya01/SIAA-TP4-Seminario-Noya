package vista;

import controlador.SolicitudController;
import controlador.ValidacionException;
import modelo.*;
import persistencia.SolicitudDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


public class VentanaPrincipal extends JFrame {

    // ----------------- Controller -----------------
    private final SolicitudController controller;

    // ----------------- Encabezado (alta) -----------------
    private final JTextField txtAfiliado    = new JTextField();
    private final JTextField txtDni         = new JTextField();
    private final JTextField txtTipo        = new JTextField();
    private final JSpinner   spCantidad     = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JTextField txtFechaOrden  = new JTextField();
    private final JComboBox<String> cmbRol  = new JComboBox<>(new String[]{"Administrativo","Médico","Subgerencia"});

    // ----------------- Filtros / Tabla solicitudes -----------------
    private final JTextField txtFiltroDni = new JTextField();
    private final JComboBox<String> cmbFiltroEstado =
            new JComboBox<>(new String[]{"Todos",
                    SolicitudController.PENDIENTE,
                    SolicitudController.EN_EVALUACION,
                    SolicitudController.SOLICITAR_DOC,
                    SolicitudController.EN_CORRECCION,
                    SolicitudController.ELEVADA,
                    SolicitudController.APROBADA,
                    SolicitudController.RECHAZADA,
                    SolicitudController.INFORMADA,
                    SolicitudController.ARCHIVADA,
                    SolicitudController.ANULADA});

    private final JTable tablaSolicitudes = new JTable(
            new DefaultTableModel(
                    new Object[]{"ID","Afiliado","DNI","Tipo","Cant.","Fecha","Estado","Obs."}, 0
            ) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            }
    );

    // ----------------- Ítems: snapshot + vista -----------------
    private final JTextField txtItemCodigo = new JTextField();
    private final JTextField txtItemDesc   = new JTextField();
    private final JSpinner   spItemCant    = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JSpinner   spItemPUnit   = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10_000_000.0, 0.5));

    private final JTable tablaItems = new JTable(
            new DefaultTableModel(new Object[]{"Código","Descripción","Cant.","P.Unit","Subtotal"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            }
    );
    // Ítems en memoria para el alta
    private final List<ItemSolicitud> itemsSnapshot = new ArrayList<>();

    // ----------------- Botones -----------------
    private final JButton btnGuardar      = new JButton("Guardar");
    private final JButton btnVer          = new JButton("Ver Solicitudes");
    private final JButton btnIniciarEval  = new JButton("Iniciar Eval.");
    private final JButton btnSolicitarDoc = new JButton("Solicitar Doc.");
    private final JButton btnRecibirCorr  = new JButton("Recibir Corr.");
    private final JButton btnAutorizar    = new JButton("Autorizar");
    private final JButton btnRechazar     = new JButton("Rechazar");
    private final JButton btnElevar       = new JButton("Elevar");
    private final JButton btnDictaminarOk = new JButton("Dictaminar ✓");
    private final JButton btnDictaminarNo = new JButton("Dictaminar ✗");
    private final JButton btnInformar     = new JButton("Informar");
    private final JButton btnArchivar     = new JButton("Archivar");
    private final JButton btnExportCsv    = new JButton("Exportar CSV...");
    private final JButton btnAgregarItem  = new JButton("Agregar ítem");

    // ----------------------------------------------------------
    // Constructor (sin parámetros: arma su propio Controller/DAO)
    // ----------------------------------------------------------
    public VentanaPrincipal() throws Exception {
        this.controller = new SolicitudController(new SolicitudDAO());

        setTitle("SIAA · Sistema de Autorizaciones Médicas (MVP++)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUi();
        pack();
        setLocationRelativeTo(null);

        // Rol por defecto
        controller.setUsuarioActual(new Administrativo("admin.demo","Autorizaciones"));
        cmbRol.setSelectedItem("Administrativo");
        refrescarHabilitacionBotones();

        // Cargar datos
        cargarSolicitudes();
    }

    // ===================== UI =====================
    private void initUi() {
        setLayout(new BorderLayout());

        // ---------- Encabezado ----------
        JPanel header = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        header.add(new JLabel("Afiliado:"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        header.add(txtAfiliado, gc);

        ((AbstractDocument) txtAfiliado.getDocument())
                .setDocumentFilter(new SoloTextoSinNumerosFilter());



        row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        header.add(new JLabel("DNI:"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        header.add(txtDni, gc);
        ((AbstractDocument) txtDni.getDocument()).setDocumentFilter(new SoloDigitosFilter());

        row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        header.add(new JLabel("Tipo de práctica:"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        header.add(txtTipo, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        header.add(new JLabel("Fecha de orden (AAAA-MM-DD):"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        txtFechaOrden.setText(LocalDate.now().toString());
        header.add(txtFechaOrden, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        header.add(new JLabel("Cantidad:"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        header.add(spCantidad, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        header.add(new JLabel("Rol actual:"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        header.add(cmbRol, gc);

        cmbRol.addActionListener(e -> cambiarRol());

        // ---------- Centro: filtros + tabla ----------
        JPanel centro = new JPanel(new BorderLayout());
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("DNI:"));
        txtFiltroDni.setColumns(10);
        filtros.add(txtFiltroDni);
        filtros.add(new JLabel("Estado:"));
        filtros.add(cmbFiltroEstado);
        centro.add(filtros, BorderLayout.NORTH);

        tablaSolicitudes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        centro.add(new JScrollPane(tablaSolicitudes), BorderLayout.CENTER);

        tablaSolicitudes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarItemsDeSolicitudSeleccionada();
                refrescarHabilitacionBotones();
            }
        });

        // ---------- Panel ítems (snapshot) ----------
        JPanel panelItems = new JPanel(new BorderLayout());
        panelItems.setBorder(BorderFactory.createTitledBorder("Ítems de la solicitud"));

        JPanel formItem = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtItemCodigo.setColumns(10);
        txtItemDesc.setColumns(24);
        formItem.add(new JLabel("Código:"));     formItem.add(txtItemCodigo);
        formItem.add(new JLabel("Descripción:"));formItem.add(txtItemDesc);
        formItem.add(new JLabel("Cant.:"));      formItem.add(spItemCant);
        formItem.add(new JLabel("P.Unit:"));     formItem.add(spItemPUnit);
        formItem.add(btnAgregarItem);
        btnAgregarItem.addActionListener(e -> agregarItemSnapshot());

        panelItems.add(formItem, BorderLayout.NORTH);
        panelItems.add(new JScrollPane(tablaItems), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centro, panelItems);
        split.setResizeWeight(0.60);

        // ---------- Botonera ----------
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(btnGuardar);
        botones.add(btnVer);
        botones.add(btnIniciarEval);
        botones.add(btnSolicitarDoc);
        botones.add(btnRecibirCorr);
        botones.add(btnAutorizar);
        botones.add(btnRechazar);
        botones.add(btnElevar);
        botones.add(btnDictaminarOk);
        botones.add(btnDictaminarNo);
        botones.add(btnInformar);
        botones.add(btnArchivar);
        botones.add(btnExportCsv);

        btnGuardar.addActionListener(e -> onGuardar());
        btnVer.addActionListener(e -> { cargarSolicitudes(); refrescarHabilitacionBotones(); });
        btnIniciarEval.addActionListener(e -> accionSoloIdSeleccionado(() -> controller.iniciarEvaluacion(idSeleccionado())));
        btnSolicitarDoc.addActionListener(e -> solicitarDocs());
        btnRecibirCorr.addActionListener(e -> accionSoloIdSeleccionado(() -> controller.recibirCorreccion(idSeleccionado())));
        btnAutorizar.addActionListener(e -> accionSoloIdSeleccionado(() -> controller.autorizar(idSeleccionado())));
        btnRechazar.addActionListener(e -> rechazarConMotivo());
        btnElevar.addActionListener(e -> elevarConMonto());
        btnDictaminarOk.addActionListener(e -> accionSoloIdSeleccionado(() -> controller.dictaminarAprobar(idSeleccionado())));
        btnDictaminarNo.addActionListener(e -> rechazarDesdeSubgerencia());
        btnInformar.addActionListener(e -> accionSoloIdSeleccionado(() -> controller.informarAfiliado(idSeleccionado())));
        btnArchivar.addActionListener(e -> accionSoloIdSeleccionado(() -> controller.archivar(idSeleccionado())));
        btnExportCsv.addActionListener(e -> exportarCsv());

        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    // ===================== Lógica UI =====================

    private void cambiarRol() {
        String rol = (String) cmbRol.getSelectedItem();
        Usuario u;
        if ("Médico".equals(rol)) {
            u = new Medico("medico.demo","Auditoría Médica");
        } else if ("Subgerencia".equals(rol)) {
            u = new Subgerencia("subgerencia.demo","Subgerencia Médica");
        } else {
            u = new Administrativo("admin.demo","Autorizaciones");
        }
        controller.setUsuarioActual(u);
        refrescarHabilitacionBotones();
    }

    private void agregarItemSnapshot() {
        String cod  = txtItemCodigo.getText().trim();
        String des  = txtItemDesc.getText().trim();
        int    cant = (Integer) spItemCant.getValue();
        double p    = ((Number) spItemPUnit.getValue()).doubleValue();

        if (cod.isEmpty() || des.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completá código y descripción del ítem.");
            return;
        }
        if (cant <= 0) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }
        ItemSolicitud it = new ItemSolicitud(null, cod, des, cant, p);
        itemsSnapshot.add(it);

        DefaultTableModel m = (DefaultTableModel) tablaItems.getModel();
        m.addRow(new Object[]{cod, des, cant, p, cant * p});

        // limpiar form item
        txtItemCodigo.setText("");
        txtItemDesc.setText("");
        spItemCant.setValue(1);
        spItemPUnit.setValue(0.0);
    }

    private void onGuardar() {
        try {
            if (itemsSnapshot.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Agregá al menos un ítem antes de guardar.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // ---- Obtener y validar fecha de orden ----
            LocalDate fechaOrden;
            String txtFecha = txtFechaOrden.getText().trim();
            if (txtFecha.isEmpty()) {
                // si la deja vacía, uso la fecha de hoy
                fechaOrden = LocalDate.now();
            } else {
                try {
                    fechaOrden = LocalDate.parse(txtFecha); // formato ISO: AAAA-MM-DD
                } catch (DateTimeParseException dtpe) {
                    JOptionPane.showMessageDialog(this,
                            "Fecha de orden inválida. Usá el formato AAAA-MM-DD.",
                            "Validación",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            controller.crearConItems(
                    txtAfiliado.getText(),
                    txtDni.getText(),
                    txtTipo.getText(),
                    (Integer) spCantidad.getValue(),
                    fechaOrden,                                // <-- usamos la fecha ingresada
                    new ArrayList<>(itemsSnapshot)
            );

            JOptionPane.showMessageDialog(this, "Solicitud guardada con éxito.");
            itemsSnapshot.clear();
            ((DefaultTableModel) tablaItems.getModel()).setRowCount(0);
            limpiarFormAlta();
            cargarSolicitudes();
            refrescarHabilitacionBotones();
        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormAlta() {
        txtAfiliado.setText("");
        txtDni.setText("");
        txtTipo.setText("");
        spCantidad.setValue(1);
        txtFechaOrden.setText(LocalDate.now().toString());
    }

    private void cargarSolicitudes() {
        try {
            List<Solicitud> datos = controller.listar();
            String fDni = txtFiltroDni.getText().trim();
            String fEst = (String) cmbFiltroEstado.getSelectedItem();

            DefaultTableModel m = (DefaultTableModel) tablaSolicitudes.getModel();
            m.setRowCount(0);

            for (Solicitud s : datos) {
                if (!fDni.isEmpty() && !String.valueOf(s.getDni()).contains(fDni)) continue;
                if (!"Todos".equals(fEst) && !s.getEstado().equals(fEst)) continue;

                m.addRow(new Object[]{
                        s.getId(), s.getAfiliado(), s.getDni(),
                        s.getTipoPractica(), s.getCantidad(),
                        s.getFecha(), s.getEstado(), s.getObservacion()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las solicitudes: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarItemsDeSolicitudSeleccionada() {
        try {
            ((DefaultTableModel) tablaItems.getModel()).setRowCount(0);

            int row = tablaSolicitudes.getSelectedRow();
            if (row == -1) return;

            int id = (Integer) tablaSolicitudes.getValueAt(row, 0);
            var items = controller.listarItems(id);

            DefaultTableModel m = (DefaultTableModel) tablaItems.getModel();
            for (var it : items) {
                double subtotal = it.getCantidad() * it.getPrecioUnit();
                m.addRow(new Object[]{ it.getCodigo(), it.getDescripcion(), it.getCantidad(), it.getPrecioUnit(), subtotal });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los ítems: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------- Helpers selección/acciones --------

    private int idSeleccionado() throws ValidacionException {
        int row = tablaSolicitudes.getSelectedRow();
        if (row == -1) throw new ValidacionException("Seleccioná una solicitud.");
        return (Integer) tablaSolicitudes.getValueAt(row, 0);
    }

    private String estadoSeleccionado() {
        int row = tablaSolicitudes.getSelectedRow();
        if (row == -1) return null;
        Object v = tablaSolicitudes.getValueAt(row, 6);
        return v == null ? null : v.toString();
    }

    private void accionSoloIdSeleccionado(Action accion) {
        try {
            idSeleccionado(); // valida
            accion.ejecutar();
            cargarSolicitudes();
            refrescarHabilitacionBotones();
        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solicitarDocs() {
        try {
            int id = idSeleccionado();
            String det = JOptionPane.showInputDialog(this, "Detalle de la documentación a solicitar:", "Solicitar documentación", JOptionPane.QUESTION_MESSAGE);
            if (det != null) {
                controller.solicitarDocumentacion(id, det);
                cargarSolicitudes();
                refrescarHabilitacionBotones();
            }
        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rechazarConMotivo() {
        try {
            int id = idSeleccionado();
            String motivo = JOptionPane.showInputDialog(this, "Motivo del rechazo:", "Rechazar", JOptionPane.QUESTION_MESSAGE);
            if (motivo != null) {
                controller.rechazar(id, motivo);
                cargarSolicitudes();
                refrescarHabilitacionBotones();
            }
        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void elevarConMonto() {
        try {
            int id = idSeleccionado();
            String txt = JOptionPane.showInputDialog(this, "Monto total estimado:", "Elevar a Subgerencia", JOptionPane.QUESTION_MESSAGE);
            if (txt != null) {
                // Permitir coma o punto
                double monto = Double.parseDouble(txt.replace(".", "").replace(",", "."));
                controller.elevar(id, monto);
                cargarSolicitudes();
                refrescarHabilitacionBotones();
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Monto inválido.", "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rechazarDesdeSubgerencia() {
        try {
            int id = idSeleccionado();
            String motivo = JOptionPane.showInputDialog(this, "Motivo del dictamen desfavorable:", "Dictaminar ✗", JOptionPane.QUESTION_MESSAGE);
            if (motivo != null) {
                controller.dictaminarRechazar(id, motivo);
                cargarSolicitudes();
                refrescarHabilitacionBotones();
            }
        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------- Habilitar/Deshabilitar por rol + estado --------
    private void refrescarHabilitacionBotones() {
        String rol    = controller.getUsuarioActual().getRol();
        boolean esAdmin = "Administrativo".equalsIgnoreCase(rol);
        boolean esMed   = "Médico".equalsIgnoreCase(rol);
        boolean esSub   = "Subgerencia".equalsIgnoreCase(rol);

        // ¿Hay una solicitud seleccionada en la tabla?
        int rowSel   = tablaSolicitudes.getSelectedRow();
        boolean haySeleccion = (rowSel != -1);

        // ---- Controles de carga de nueva solicitud ----
        // Solo cuando NO hay selección (modo "alta nueva")
        btnGuardar.setEnabled(esAdmin && !haySeleccion);
        btnAgregarItem.setEnabled(esAdmin && !haySeleccion);

        // Si no hay selección, deshabilitamos todo lo demás y salimos
        if (!haySeleccion) {
            btnIniciarEval.setEnabled(false);
            btnSolicitarDoc.setEnabled(false);
            btnRecibirCorr.setEnabled(false);
            btnAutorizar.setEnabled(false);
            btnRechazar.setEnabled(false);
            btnElevar.setEnabled(false);
            btnDictaminarOk.setEnabled(false);
            btnDictaminarNo.setEnabled(false);
            btnInformar.setEnabled(false);
            btnArchivar.setEnabled(false);
            return;
        }

        // A partir de acá ya sabemos que hay una solicitud seleccionada
        String estado = estadoSeleccionado();
        if (estado == null) {
            // Por seguridad, igual que el caso sin selección
            btnIniciarEval.setEnabled(false);
            btnSolicitarDoc.setEnabled(false);
            btnRecibirCorr.setEnabled(false);
            btnAutorizar.setEnabled(false);
            btnRechazar.setEnabled(false);
            btnElevar.setEnabled(false);
            btnDictaminarOk.setEnabled(false);
            btnDictaminarNo.setEnabled(false);
            btnInformar.setEnabled(false);
            btnArchivar.setEnabled(false);
            return;
        }

        boolean enPend    = estado.equals(SolicitudController.PENDIENTE);
        boolean enEval    = estado.equals(SolicitudController.EN_EVALUACION);
        boolean enDoc     = estado.equals(SolicitudController.SOLICITAR_DOC);
        boolean enCorr    = estado.equals(SolicitudController.EN_CORRECCION);
        boolean enElev    = estado.equals(SolicitudController.ELEVADA);
        boolean aprobada  = estado.equals(SolicitudController.APROBADA);
        boolean rechazada = estado.equals(SolicitudController.RECHAZADA);
        boolean informada = estado.equals(SolicitudController.INFORMADA);

        // Iniciar evaluación: desde PENDIENTE o EN_CORRECCION
        btnIniciarEval.setEnabled((esAdmin || esMed) && (enPend || enCorr));

        // Solicitar docs: Médico desde PENDIENTE o EN_EVALUACION
        btnSolicitarDoc.setEnabled(esMed && (enEval || enPend));

        // Recibir corrección: Admin cuando está SOLICITAR_DOC
        btnRecibirCorr.setEnabled(esAdmin && enDoc);

        // Triage del médico SOLO en EN_EVALUACION
        btnAutorizar.setEnabled(esMed && enEval);
        btnRechazar.setEnabled(esMed && enEval);
        btnElevar.setEnabled(esMed && enEval);

        // Subgerencia dicta SOLO en ELEVADA
        btnDictaminarOk.setEnabled(esSub && enElev);
        btnDictaminarNo.setEnabled(esSub && enElev);

        // Informar: Admin si está aprobada o rechazada
        btnInformar.setEnabled(esAdmin && (aprobada || rechazada));

        // Archivar: Admin si ya fue informada
        btnArchivar.setEnabled(esAdmin && informada);
    }


    private void exportarCsv() {
        DefaultTableModel m = (DefaultTableModel) tablaSolicitudes.getModel();
        if (m.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar solicitudes a CSV");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File out = fc.getSelectedFile();
        // Asegurar extensión .csv
        if (!out.getName().toLowerCase().endsWith(".csv")) {
            out = new File(out.getParentFile(), out.getName() + ".csv");
        }

        try (BufferedWriter bw = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
            // encabezados
            for (int c = 0; c < m.getColumnCount(); c++) {
                if (c > 0) bw.append(';');
                bw.append(m.getColumnName(c));
            }
            bw.append('\n');
            // filas
            for (int r = 0; r < m.getRowCount(); r++) {
                for (int c = 0; c < m.getColumnCount(); c++) {
                    if (c > 0) bw.append(';');
                    Object v = m.getValueAt(r, c);
                    bw.append(v == null ? "" : v.toString());
                }
                bw.append('\n');
            }
            bw.flush();
            JOptionPane.showMessageDialog(this, "Exportado a: " + out.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== Utiles =====================
    // ===================== Utiles =====================
    @FunctionalInterface
    private interface Action { void ejecutar() throws Exception; }

    private static class SoloDigitosFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offs, String str, javax.swing.text.AttributeSet a)
                throws javax.swing.text.BadLocationException {
            if (str != null && str.matches("\\d+")) super.insertString(fb, offs, str, a);
        }
        @Override
        public void replace(FilterBypass fb, int offs, int length, String str, javax.swing.text.AttributeSet a)
                throws javax.swing.text.BadLocationException {
            if (str != null && str.matches("\\d*")) super.replace(fb, offs, length, str, a);
        }
    }

    private static class SoloTextoSinNumerosFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offs, String str,
                                 javax.swing.text.AttributeSet a)
                throws javax.swing.text.BadLocationException {
            if (str != null && str.matches("[\\p{L} .'-]+")) {
                super.insertString(fb, offs, str, a);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offs, int length, String str,
                            javax.swing.text.AttributeSet a)
                throws javax.swing.text.BadLocationException {
            if (str != null && str.matches("[\\p{L} .'-]*")) {
                super.replace(fb, offs, length, str, a);
            }
        }
    }
}

