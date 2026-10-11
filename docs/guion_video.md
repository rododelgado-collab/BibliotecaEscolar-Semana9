# Guion del video (3–5 min) – Evaluación Final Transversal

Graba con tu rostro visible (Kaltura), compartiendo pantalla. Tiempos orientativos.

**0:00–0:20 Presentación.** "Hola, soy Rodolfo Delgado, esta es mi Evaluación Final de Desarrollo Orientado a Objetos II: una aplicación de biblioteca escolar en Java con Swing y MySQL."

**0:20–1:30 Demo como bibliotecario** (antonia@correo.cl / clave123)
- Login → pestaña Libros: crear un libro, editarlo, intentar guardar uno con datos inválidos (muestra la validación).
- Estudiantes: registrar un estudiante nuevo.
- Préstamos: registrar un préstamo, mostrar que el stock baja; intentar prestar un libro sin stock y uno repetido; intentar un 4.º préstamo (límite de 3).
- Devolver el libro (aparece la constancia).
- Reportes: disponibles, activos, atrasados, más prestados.

**1:30–2:00 Demo como estudiante** (carlos@correo.cl): solo ve catálogo, sus préstamos y sus reportes; no puede administrar.

**2:00–4:15 Código en el IDE**
- Estructura de paquetes: MVC (vista / controlador / modelo) y DAO.
- `DatabaseConnection`: Singleton (una conexión, `getInstance`, reconexión).
- `CrudDAO<T>` y `LibroDAOImpl`: CRUD con `PreparedStatement`.
- `Persona`/`Usuario` abstractas, `Bibliotecario`/`UsuarioEstudiante` (polimorfismo), interfaces `Autenticable` y `Prestable`.
- `TareaSegundoPlano` + `ControladorPrestamos.registrarPrestamo`: hilo para no congelar la interfaz, `synchronized` y el `UPDATE ... stock>0` en transacción para evitar stock negativo.
- Mostrar la prueba de concurrencia (`mvn test`) pasando.

**4:15–4:45 Cierre.** Repositorio en GitHub, reflexión breve de lo aprendido (separar capas, concurrencia segura) y despedida.
