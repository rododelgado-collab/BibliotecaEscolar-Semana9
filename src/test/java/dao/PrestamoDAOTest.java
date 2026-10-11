package dao;

import controlador.ControladorPrestamos;
import controlador.ResultadoPrestamo;
import modelo.Categoria;
import modelo.Estudiante;
import modelo.Libro;
import modelo.LibroPrestado;
import modelo.Prestamo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de PrestamoDAO (transacciones de stock) y de la concurrencia real contra la base de pruebas. */
class PrestamoDAOTest extends BaseDaoTest {

    private Libro libro(int stock) {
        return guardarLibro("Dune", "9780441013593", stock, guardarCategoria("Ciencia Ficción"));
    }

    @Test
    @DisplayName("prestarLibro guarda el préstamo y descuenta el stock; devolverLibro lo repone")
    void prestarYDevolver() {
        Libro l = libro(2);
        Estudiante e = guardarEstudiante("Carlos Ruiz", "98765432-1");
        Prestamo p = Prestamo.nuevo(e.getId(), l.getId(), LocalDate.now());

        assertTrue(prestamoDAO.prestarLibro(p));
        assertTrue(p.getId() > 0);
        assertEquals(1, libroDAO.readById(l.getId()).getStock());
        Prestamo leido = prestamoDAO.readById(p.getId());
        assertEquals("Carlos Ruiz", leido.getNombreEstudiante());
        assertEquals("Dune", leido.getTituloLibro());
        assertEquals(LocalDate.now().plusDays(7), leido.getFechaVencimiento());
        assertFalse(leido.isDevuelto());

        assertTrue(prestamoDAO.devolverLibro(p.getId()));
        assertEquals(2, libroDAO.readById(l.getId()).getStock());
        assertTrue(prestamoDAO.readById(p.getId()).isDevuelto());
        assertFalse(prestamoDAO.devolverLibro(p.getId()), "no se puede devolver dos veces");
        assertEquals(2, libroDAO.readById(l.getId()).getStock(), "la segunda devolución no suma stock");
    }

    @Test
    @DisplayName("sin stock, prestarLibro devuelve false y no guarda nada")
    void sinStock() {
        Libro l = libro(0);
        Estudiante e = guardarEstudiante("Carlos Ruiz", "98765432-1");
        assertFalse(prestamoDAO.prestarLibro(Prestamo.nuevo(e.getId(), l.getId(), LocalDate.now())));
        assertEquals(0, libroDAO.readById(l.getId()).getStock());
        assertEquals(0, prestamoDAO.readAll().size());
    }

    @Test
    @DisplayName("TRANSACCIÓN: si falla el INSERT del préstamo, el stock vuelve a su valor (rollback)")
    void rollbackDelStock() {
        Libro l = libro(2);
        Prestamo conEstudianteInexistente = Prestamo.nuevo(9999, l.getId(), LocalDate.now());
        DAOException ex = assertThrows(DAOException.class, () -> prestamoDAO.prestarLibro(conEstudianteInexistente));
        assertTrue(ex.getMessage().contains("no existe"));
        assertEquals(2, libroDAO.readById(l.getId()).getStock(), "el descuento debe deshacerse");
    }

    @Test
    @DisplayName("consultas: historial, activos, conteos y existeActivo")
    void consultas() {
        Libro l1 = libro(5);
        Libro l2 = guardarLibro("Fundación", "9788497594256", 5, l1.getCategoria());
        Estudiante e = guardarEstudiante("Carlos Ruiz", "98765432-1");
        Estudiante otro = guardarEstudiante("María Torres", "11222333-4");
        Prestamo p1 = Prestamo.nuevo(e.getId(), l1.getId(), LocalDate.now());
        Prestamo p2 = Prestamo.nuevo(e.getId(), l2.getId(), LocalDate.now());
        prestamoDAO.prestarLibro(p1);
        prestamoDAO.prestarLibro(p2);
        prestamoDAO.prestarLibro(Prestamo.nuevo(otro.getId(), l1.getId(), LocalDate.now()));
        prestamoDAO.devolverLibro(p2.getId());

        assertEquals(2, prestamoDAO.readByEstudiante(e.getId()).size());
        assertEquals(1, prestamoDAO.readActivosByEstudiante(e.getId()).size());
        assertEquals(2, prestamoDAO.readActivos().size());
        assertEquals(1, prestamoDAO.countActivosByEstudiante(e.getId()));
        assertEquals(2, prestamoDAO.countByEstudiante(e.getId()));
        assertEquals(2, prestamoDAO.countByLibro(l1.getId()));
        assertTrue(prestamoDAO.existeActivo(e.getId(), l1.getId()));
        assertFalse(prestamoDAO.existeActivo(e.getId(), l2.getId()));
    }

    @Test
    @DisplayName("reporte: libros más prestados, de mayor a menor")
    void librosMasPrestados() {
        Libro l1 = libro(9);
        Libro l2 = guardarLibro("Fundación", "9788497594256", 9, l1.getCategoria());
        Estudiante e1 = guardarEstudiante("Carlos Ruiz", "98765432-1");
        Estudiante e2 = guardarEstudiante("María Torres", "11222333-4");
        prestamoDAO.prestarLibro(Prestamo.nuevo(e1.getId(), l2.getId(), LocalDate.now()));
        prestamoDAO.prestarLibro(Prestamo.nuevo(e2.getId(), l2.getId(), LocalDate.now()));
        prestamoDAO.prestarLibro(Prestamo.nuevo(e1.getId(), l1.getId(), LocalDate.now()));

        List<LibroPrestado> top = prestamoDAO.readLibrosMasPrestados(5);
        assertEquals(2, top.size());
        assertEquals("Fundación", top.get(0).titulo());
        assertEquals(2, top.get(0).totalPrestamos());
        assertEquals(1, top.get(1).totalPrestamos());
        assertEquals(1, prestamoDAO.readLibrosMasPrestados(1).size());
    }

    @Test
    @DisplayName("CRUD de préstamos: update y delete (si estaba sin devolver, delete repone el stock)")
    void crud() {
        Libro l = libro(3);
        Estudiante e = guardarEstudiante("Carlos Ruiz", "98765432-1");
        Prestamo p = Prestamo.nuevo(e.getId(), l.getId(), LocalDate.now());
        prestamoDAO.prestarLibro(p);

        Prestamo leido = prestamoDAO.readById(p.getId());
        leido.setDevuelto(true);
        assertTrue(prestamoDAO.update(leido));
        assertTrue(prestamoDAO.readById(p.getId()).isDevuelto());

        Prestamo activo = Prestamo.nuevo(e.getId(), l.getId(), LocalDate.now());
        prestamoDAO.prestarLibro(activo);
        assertEquals(1, libroDAO.readById(l.getId()).getStock());
        assertTrue(prestamoDAO.delete(activo.getId()));
        assertEquals(2, libroDAO.readById(l.getId()).getStock(), "eliminar un préstamo sin devolver repone el stock");
        assertFalse(prestamoDAO.delete(activo.getId()));
    }

    @Test
    @DisplayName("CONCURRENCIA REAL: 25 hilos piden el mismo libro (stock 4) contra MySQL y se prestan exactamente 4")
    void concurrenciaContraLaBase() throws InterruptedException {
        final int hilosTotales = 25;
        final int stock = 4;
        Libro l = libro(stock);
        List<Estudiante> alumnos = new ArrayList<>();
        for (int i = 0; i < hilosTotales; i++) {
            alumnos.add(guardarEstudiante("Alumno" + i + " Prueba", String.format("%08d-1", i + 1)));
        }
        ControladorPrestamos controlador = new ControladorPrestamos(prestamoDAO, estudianteDAO, libroDAO, LocalDate::now, 0);

        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();
        CountDownLatch salida = new CountDownLatch(1);
        List<Thread> hilos = new ArrayList<>();
        for (Estudiante alumno : alumnos) {
            Thread t = new Thread(() -> {
                try {
                    salida.await();
                    ResultadoPrestamo r = controlador.registrarPrestamo(alumno.getId(), l.getId());
                    assertTrue(r.stockRestante() >= 0);
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
            t.join(30000);
        }

        assertEquals(stock, exitos.get());
        assertEquals(hilosTotales - stock, rechazos.get());
        assertEquals(0, libroDAO.readById(l.getId()).getStock());
        assertEquals(stock, prestamoDAO.readActivos().size());
    }
}
