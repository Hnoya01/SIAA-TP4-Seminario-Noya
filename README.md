# 🩺 Sistema de Autorizaciones de Atención (SIAA)

### TP4 – Seminario de Práctica Profesional

### Licenciatura en Informática – 2025

---

## 📘 Descripción general

El **Sistema de Autorizaciones de Atención (SIAA)** es un prototipo funcional desarrollado en **Java + Swing** que reproduce el circuito completo de autorizaciones médicas utilizado en una obra social u institución de salud.

La aplicación permite:

✔ Registrar solicitudes con ítems obligatorios
✔ Aplicar reglas de negocio (vigencia, topes, umbral de elevación)
✔ Gestionar el flujo de estados con permisos por rol
✔ Mantener una bitácora completa de movimientos
✔ Persistir la información en una base **MySQL** mediante **DAO + JDBC**
✔ Exportar listados a **CSV**

El objetivo del prototipo es validar la arquitectura, el modelo del dominio, las reglas funcionales y el flujo operativo real del proceso de autorizaciones.

---

## 🎯 Objetivos del proyecto

* Implementar un prototipo funcional que ordene el circuito de autorizaciones.
* Reducir rechazos por documentación incompleta mediante ítems obligatorios.
* Estandarizar el flujo PENDIENTE → EVALUACIÓN → DECISIÓN → INFORMADA → ARCHIVADA.
* Aplicar reglas de negocio desacopladas del UI.
* Registrar trazabilidad de cada acción en la bitácora.
* Persistir datos en MySQL con manejo robusto de excepciones.
* Validar permisos por rol: Administrativo, Médico auditor y Subgerencia.

---

## 🧩 Estructura del proyecto

```
src/
├── modelo/                → Entidades del dominio (Solicitud, Item, Movimiento, Usuario, etc.)
│
├── controlador/           → Flujo, reglas, validaciones, transiciones y bitácora
│    └── SolicitudController.java
│
├── persistencia/          → DAO + JDBC + conexión MySQL
│    ├── SolicitudDAO.java
│    ├── ItemSolicitudDAO.java
│    ├── MovimientoDAO.java
│    └── Conexion.java
│
└── vista/                 → Interfaz gráfica Swing (MVC)
     └── VentanaPrincipal.java
```

Además se incluye el archivo:

```
autorizaciones.sql     → Script con el modelo de datos en MySQL
```

---

## 🛠️ Tecnologías utilizadas

* **Java 17**
* **Swing** (interfaz gráfica)
* **MySQL 8.x**
* **JDBC (PreparedStatement + ResultSet)**
* **Patrón MVC**
* **Índices y claves foráneas en base de datos**
* **GitHub para control de versiones**

---

## 🗂️ Modelo de datos (MySQL)

El esquema está compuesto por 3 tablas principales en 3FN:

* **solicitudes** → encabezado
* **item_solicitud** → detalle 1:N
* **movimientos** → bitácora 1:N

Incluye claves foráneas y `ON DELETE RESTRICT` para preservar historial.

✔ El archivo `estructura_siaa.sql` contiene la definición completa de las tablas, índices y FK.

---

## 🚦 Flujo de estados implementado

```
PENDIENTE
   ↓
EN_EVALUACION  ←── EN_CORRECCION
   ↓  ↘
 APROBADA   SOLICITAR_DOC
     ↓           ↓
 INFORMADA    EN_CORRECCION
     ↓
 ARCHIVADA

ELEVADA → { APROBADA | RECHAZADA }
ANULADA → estado terminal
```

Permisos por rol aplicados desde el controlador.

---

## ✔ Validaciones incluidas

* Ítems obligatorios
* DNI numérico (7–10 dígitos)
* Afiliado solo texto (sin números)
* Cantidad y precio > 0
* Vigencia ≤ 30 días
* La fecha no puede ser futura
* Topes por práctica (Kinesiología, Psicología, etc.)
* Umbral de monto para elevar automáticamente
* Transiciones validadas contra tabla de estados
* Manejo de excepciones SQL con mensajes al usuario

---

## 🖥️ Interfaz gráfica (Swing)

La ventana principal permite:

* Alta de solicitudes
* Carga de ítems
* Filtros por DNI y estado
* Cambiar de rol (para probar flujo)
* Ver ítems de cada solicitud
* Ejecutar acciones del médico / administrativo / subgerencia
* Exportar listado a CSV

---

## ⚙️ Configuración del entorno

### Base de datos

Importar el archivo:

```
autorizaciones.sql
```

Configurar en `Conexion.java`:

```java
private static final String URL  = "jdbc:mysql://localhost/siaa";
private static final String USER = "root";     // dejar vacío si corresponde
private static final String PASS = "";         // nunca subir tu clave real
```

### Requisitos

* Java 17
* MySQL 8.x
* IntelliJ IDEA (recomendado)

---

## ▶️ Ejecución

Desde el IDE:

```
Ejecutar: App
```

---

## 🧪 Conjunto mínimo de pruebas funcionales

* Alta con ítems (OK)
* Alta sin ítems (bloquea)
* Fecha vencida / fecha futura (bloquea)
* Tope por práctica (bloquea)
* Flujo normal: pendiente → evaluación → decisión → informar → archivar
* Solicitar documentación → recibir corrección
* Umbral: total > 200.000 → obliga a elevar
* Subgerencia: dictaminar aprobar / rechazar
* Exportación a CSV
* Caída de base: error controlado sin cerrar la aplicación

---

## 📦 Archivos incluidos en este repositorio

* `/src` completo (modelo, controlador, vista, DAO)
* `estructura_siaa.sql` (modelo MySQL)
* `.gitignore` configurado para no subir credenciales ni archivos del IDE
* Este README.md

