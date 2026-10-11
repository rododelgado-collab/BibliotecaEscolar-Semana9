package controlador;

import dao.DAOException;
import modelo.Estudiante;
import modelo.Libro;
import modelo.Prestamo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del controlador de préstamos con DAO falsos (no necesitan MySQL): reglas, atrasos, hilos y concurrencia. */
class ControladorPrestamosTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 11);

    private DaosFalsos.Libros libros;
    private DaosFalsos.Estudiantes estudiantes;
    private DaosFalsos.Prestamos prestamos;
    private ControladorPrestamos controlador;
    private final List<String> bitacora = new ArrayList<>();

    @BeforeEach
    void preparar() {
        DaosFalsos.caidos = false;
        DaosFalsos.latenciaMs = 0;
        libros = new DaosFalsos.Libros();
        estudiantes = new DaosFalsos.Estudiantes(new DaosFalsos.Usuarios());
        prestamos = new DaosFalsos.Prestamos(libros, estudiantes);
        controlador = new ControladorPrestamos(prestamos, estudiantes, libros, () -> HOY, 0);
        controlador.setBitacora(bitacora::add);
    }

    @AfterEach
    void restaurar() {
        DaosFalsos.caidos = false;
        DaosFalsos.latenciaMs = 0;
    }

    private String mensajeDe(Runnable accion) {
        return assertThrows(IllegalArgumentException.class, accion::run).getMessage();
    }

    @Test
    @DisplayName("prestar guarda el préstamo, calcula el vencimiento a 7 días y descuenta el stock")
    void prestar() {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        Libro l = libros.agregar("Dune", "9780441013593", 5);

        ResultadoPrestamo r = controlador.registrarPrestamo(e.getId(), l.getId());

        assertEquals(HOY, r.prestamo().getFechaPrestamo());
        assertEquals(HOY.plusDays(7), r.prestamo().getFechaVencimiento());
        assertFalse(r.prestamo().isDevuelto());
        assertEquals(4, r.stockRestante());
        assertEquals(1, r.totalHistorial());
        assertEquals(4, libros.datos.get(l.getId()).getStock());
    }

    @Test
    @DisplayName("no se puede prestar sin stock, sin estudiante, sin libro, ni el mismo libro dos veces")
    void prestarValida() {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        Libro sinStock = libros.agregar("Agotado", "9780441013593", 0);
        Libro l = libros.agregar("Dune", "9788497594256", 5);

        assertTrue(mensajeDe(() -> controlador.registrarPrestamo(e.getId(), sinStock.getId())).contains("No hay stock"));
        assertTrue(mensajeDe(() -> controlador.registrarPrestamo(99, l.getId())).contains("estudiante"));
        assertTrue(mensajeDe(() -> controlador.registrarPrestamo(e.getId(), 99)).contains("libro"));

        controlador.registrarPrestamo(e.getId(), l.getId());
        assertTrue(mensajeDe(() -> controlador.registrarPrestamo(e.getId(), l.getId())).contains("ya tiene"));
        assertEquals(4, libros.datos.get(l.getId()).getStock());
    }

    @Test
    @DisplayName("un estudiante no puede tener más de 3 libros sin devolver")
    void limiteDePrestamos() {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        for (int i = 1; i <= 3; i++) {
            controlador.registrarPrestamo(e.getId(), libros.agregar("Libro " + i, "97800000000" + i + "0", 2).getId());
        }
        Libro cuarto = libros.agregar("Libro 4", "9780000000040", 2);
        assertTrue(mensajeDe(() -> controlador.registrarPrestamo(e.getId(), cuarto.getId())).contains("máximo"));
        assertEquals(2, libros.datos.get(cuarto.getId()).getStock());
    }

    @Test
    @DisplayName("devolver marca el préstamo, repone el stock y no deja constancia si fue a tiempo")
    void devolverATiempo() {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        Libro l = libros.agregar("Dune", "9780441013593", 5);
        ResultadoPrestamo r = controlador.registrarPrestamo(e.getId(), l.getId());

        ResultadoDevolucion d = controlador.registrarDevolucion(r.prestamo().getId());

        assertFalse(d.tuvoAtraso());
        assertTrue(prestamos.datos.get(r.prestamo().getId()).isDevuelto());
        assertEquals(5, libros.datos.get(l.getId()).getStock());
        assertTrue(bitacora.isEmpty());
        assertTrue(mensajeDe(() -> controlador.registrarDevolucion(r.prestamo().getId())).contains("ya fue devuelto"));
    }

    @Test
    @DisplayName("devolver un préstamo vencido detecta el atraso y deja constancia en la bitácora")
    void devolverConAtraso() {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        Libro l = libros.agregar("Dune", "9780441013593", 5);
        // Préstamo antiguo: pedido el 1 de octubre, vencía el 8 (3 días de atraso al 11)
        Prestamo viejo = new Prestamo(0, e.getId(), l.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8), false);
        prestamos.create(viejo);
        l.setStock(4);

        assertTrue(viejo.estaAtrasado(HOY));
        assertEquals("ATRASADO (3 d)", viejo.estado(HOY));

        ResultadoDevolucion d = controlador.registrarDevolucion(viejo.getId());

        assertTrue(d.tuvoAtraso());
        assertEquals(3, d.diasDeAtraso());
        assertEquals(1, bitacora.size());
        assertTrue(bitacora.get(0).contains("3 día(s) de atraso"));
        assertEquals(5, l.getStock());
    }

    @Test
    @DisplayName("el vencimiento exacto no es atraso: se está atrasado solo después de la fecha")
    void vencimientoExactoNoEsAtraso() {
        Prestamo p = new Prestamo(1, 1, 1, HOY.minusDays(7), HOY, false);
        assertFalse(p.estaAtrasado(HOY));
        assertEquals("Vigente", p.estado(HOY));
        assertTrue(p.estaAtrasado(HOY.plusDays(1)));
        assertEquals("Devuelto", new Prestamo(1, 1, 1, HOY, HOY, true).estado(HOY.plusDays(30)));
    }

    @Test
    @DisplayName("con la base de datos caída el préstamo falla con DAOException y no cambia el stock")
    void bdCaida() {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        Libro l = libros.agregar("Dune", "9780441013593", 5);
        DaosFalsos.caidos = true;
        assertThrows(DAOException.class, () -> controlador.registrarPrestamo(e.getId(), l.getId()));
        DaosFalsos.caidos = false;
        assertEquals(5, libros.datos.get(l.getId()).getStock());
    }

    @Test
    @DisplayName("el préstamo en segundo plano corre en otro hilo y avisa el resultado")
    void prestamoEnSegundoPlano() throws InterruptedException {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        Libro l = libros.agregar("Dune", "9780441013593", 5);
        String hiloLlamador = Thread.currentThread().getName();
        List<String> hiloDeTrabajo = new ArrayList<>();
        List<ResultadoPrestamo> resultados = new ArrayList<>();

        Thread hilo = controlador.registrarPrestamoEnSegundoPlano(e.getId(), l.getId(),
                r -> { hiloDeTrabajo.add(Thread.currentThread().getName()); resultados.add(r); },
                ex -> fail("no debía fallar: " + ex.getMessage()));
        hilo.join(5000);

        assertEquals(1, resultados.size());
        assertEquals("Hilo-Prestamo", hiloDeTrabajo.get(0));
        assertNotEquals(hiloLlamador, hiloDeTrabajo.get(0));
        assertEquals(4, libros.datos.get(l.getId()).getStock());
    }

    @Test
    @DisplayName("el hilo informa los errores de negocio con el callback de error")
    void errorEnSegundoPlano() throws InterruptedException {
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        Libro agotado = libros.agregar("Agotado", "9780441013593", 0);
        List<String> errores = new ArrayList<>();

        Thread hilo = controlador.registrarPrestamoEnSegundoPlano(e.getId(), agotado.getId(),
                r -> fail("no debía prestar"), ex -> errores.add(ex.getMessage()));
        hilo.join(5000);

        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("No hay stock"));
    }

    @Test
    @DisplayName("CONCURRENCIA: 30 hilos piden el mismo libro (stock 3) y se prestan exactamente 3")
    void condicionDeCarrera() throws InterruptedException {
        final int hilosTotales = 30;
        final int stockInicial = 3;
        DaosFalsos.latenciaMs = 5;   // la "base de datos" tarda: sin synchronized, varios hilos verían el mismo stock
        Libro libro = libros.agregar("Dune", "9780441013593", stockInicial);
        List<Estudiante> alumnos = new ArrayList<>();
        for (int i = 0; i < hilosTotales; i++) {
            alumnos.add(estudiantes.agregar("Alumno" + i + " Prueba", String.format("%08d-1", i + 1)));
        }

        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();
        CountDownLatch salida = new CountDownLatch(1);
        List<Thread> hilos = new ArrayList<>();
        for (Estudiante alumno : alumnos) {
            Thread t = new Thread(() -> {
                try {
                    salida.await();   // todos parten al mismo tiempo
                    controlador.registrarPrestamo(alumno.getId(), libro.getId());
                    exitos.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    rechazos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            hilos.add(t);
            t.start();
        }
        salida.countDown();
        for (Thread t : hilos) {
            t.join(20000);
        }

        assertEquals(stockInicial, exitos.get(), "solo pueden prestarse tantos libros como stock había");
        assertEquals(hilosTotales - stockInicial, rechazos.get());
        assertEquals(0, libros.datos.get(libro.getId()).getStock(), "el stock nunca debe quedar negativo");
        assertEquals(stockInicial, prestamos.datos.size());
    }
}
