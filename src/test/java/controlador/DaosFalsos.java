package controlador;

import dao.CategoriaDAO;
import dao.DAOException;
import dao.EstudianteDAO;
import dao.LibroDAO;
import dao.PrestamoDAO;
import dao.UsuarioDAO;
import modelo.Categoria;
import modelo.Estudiante;
import modelo.Libro;
import modelo.LibroPrestado;
import modelo.Prestamo;
import modelo.Usuario;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DAO "falsos" que guardan todo en memoria. Sirven para probar los controladores (validaciones, reglas y
 * concurrencia) sin necesitar MySQL: estas pruebas corren siempre, también en un computador sin base de datos.
 */
final class DaosFalsos {

    private DaosFalsos() { }

    /** Cuando vale true, todos los métodos lanzan DAOException (simula MySQL caído). */
    static volatile boolean caidos = false;

    /** Milisegundos que "tarda" la base de datos al prestar (hace más probable una condición de carrera). */
    static volatile long latenciaMs = 0;

    static void revisar() {
        if (caidos) {
            throw new DAOException("No se pudo conectar con el servidor MySQL (simulado).");
        }
    }

    static void dormir(long ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Categorias implements CategoriaDAO {
        final Map<Integer, Categoria> datos = new LinkedHashMap<>();
        final Libros libros;
        int siguiente = 1;

        Categorias(Libros libros) { this.libros = libros; }

        @Override public void create(Categoria c) { revisar(); c.setId(siguiente++); datos.put(c.getId(), new Categoria(c.getId(), c.getNombre())); }
        @Override public List<Categoria> readAll() { revisar(); return new ArrayList<>(datos.values()); }
        @Override public Categoria readById(int id) { revisar(); return datos.get(id); }
        @Override public boolean update(Categoria c) {
            revisar();
            if (!datos.containsKey(c.getId())) return false;
            datos.put(c.getId(), new Categoria(c.getId(), c.getNombre()));
            return true;
        }
        @Override public boolean delete(int id) { revisar(); return datos.remove(id) != null; }
        @Override public int countLibros(int id) {
            revisar();
            return (int) libros.datos.values().stream().filter(l -> l.getCategoria() != null && l.getCategoria().getId() == id).count();
        }
    }

    static class Libros implements LibroDAO {
        final Map<Integer, Libro> datos = new LinkedHashMap<>();
        int siguiente = 1;

        @Override public void create(Libro l) { revisar(); l.setId(siguiente++); datos.put(l.getId(), l); }
        @Override public List<Libro> readAll() { revisar(); return new ArrayList<>(datos.values()); }
        @Override public Libro readById(int id) { revisar(); return datos.get(id); }
        @Override public Libro readByIsbn(String isbn) {
            revisar();
            return datos.values().stream().filter(l -> l.getIsbn().equals(isbn)).findFirst().orElse(null);
        }
        @Override public List<Libro> buscar(String texto) {
            revisar();
            String t = texto == null ? "" : texto.trim().toLowerCase();
            return datos.values().stream()
                    .filter(l -> (l.getTitulo() + l.getAutor() + l.getIsbn() + l.getEditorial()).toLowerCase().contains(t))
                    .collect(Collectors.toList());
        }
        @Override public List<Libro> readDisponibles() {
            revisar();
            return datos.values().stream().filter(Libro::estaDisponible).collect(Collectors.toList());
        }
        @Override public boolean update(Libro l) { revisar(); if (!datos.containsKey(l.getId())) return false; datos.put(l.getId(), l); return true; }
        @Override public boolean delete(int id) { revisar(); return datos.remove(id) != null; }

        Libro agregar(String titulo, String isbn, int stock) {
            Libro l = new Libro(titulo, "Autor Prueba", isbn, "Editorial", stock, new Categoria(1, "General"));
            create(l);
            return l;
        }
    }

    static class Usuarios implements UsuarioDAO {
        final Map<Integer, Usuario> datos = new LinkedHashMap<>();
        int siguiente = 1;

        @Override public void create(Usuario u) { revisar(); u.setId(siguiente++); datos.put(u.getId(), u); }
        @Override public List<Usuario> readAll() { revisar(); return new ArrayList<>(datos.values()); }
        @Override public Usuario readById(int id) { revisar(); return datos.get(id); }
        @Override public Usuario readByCorreo(String correo) {
            revisar();
            return datos.values().stream().filter(u -> u.getCorreo().equalsIgnoreCase(correo)).findFirst().orElse(null);
        }
        @Override public Usuario readByRut(String rut) {
            revisar();
            return datos.values().stream().filter(u -> u.getRut().equals(rut)).findFirst().orElse(null);
        }
        @Override public boolean update(Usuario u) { revisar(); if (!datos.containsKey(u.getId())) return false; datos.put(u.getId(), u); return true; }
        @Override public boolean delete(int id) { revisar(); return datos.remove(id) != null; }
    }

    static class Estudiantes implements EstudianteDAO {
        final Map<Integer, Estudiante> datos = new LinkedHashMap<>();
        final Usuarios usuarios;
        int siguiente = 1;

        Estudiantes(Usuarios usuarios) { this.usuarios = usuarios; }

        @Override public void create(Estudiante e) { revisar(); e.setId(siguiente++); datos.put(e.getId(), e); }
        @Override public void registrarConUsuario(Estudiante e, String clave) {
            revisar();
            create(e);
            usuarios.create(Usuario.crear(0, e.getNombre(), e.getRut(), e.getCorreo(), clave, modelo.Rol.ESTUDIANTE));
        }
        @Override public List<Estudiante> readAll() { revisar(); return new ArrayList<>(datos.values()); }
        @Override public List<Estudiante> buscar(String texto) {
            revisar();
            String t = texto == null ? "" : texto.trim().toLowerCase();
            return datos.values().stream()
                    .filter(e -> (e.getNombre() + e.getRut() + e.getCurso() + e.getCorreo()).toLowerCase().contains(t))
                    .collect(Collectors.toList());
        }
        @Override public Estudiante readById(int id) { revisar(); return datos.get(id); }
        @Override public Estudiante readByRut(String rut) {
            revisar();
            return datos.values().stream().filter(e -> e.getRut().equals(rut)).findFirst().orElse(null);
        }
        @Override public boolean update(Estudiante e) {
            revisar();
            if (!datos.containsKey(e.getId())) return false;
            datos.put(e.getId(), e);
            return true;
        }
        @Override public boolean delete(int id) {
            revisar();
            Estudiante e = datos.remove(id);
            if (e == null) return false;
            usuarios.datos.values().removeIf(u -> u.getRut().equals(e.getRut()));
            return true;
        }

        Estudiante agregar(String nombre, String rut) {
            Estudiante e = new Estudiante(nombre, rut, "3ro Medio A", nombre.split(" ")[0].toLowerCase() + "@correo.cl");
            create(e);
            return e;
        }
    }

    static class Prestamos implements PrestamoDAO {
        final Map<Integer, Prestamo> datos = new LinkedHashMap<>();
        final Libros libros;
        final Estudiantes estudiantes;
        int siguiente = 1;

        Prestamos(Libros libros, Estudiantes estudiantes) {
            this.libros = libros;
            this.estudiantes = estudiantes;
        }

        @Override public void create(Prestamo p) { revisar(); p.setId(siguiente++); decorar(p); datos.put(p.getId(), p); }
        @Override public List<Prestamo> readAll() { revisar(); return new ArrayList<>(datos.values()); }
        @Override public Prestamo readById(int id) { revisar(); return datos.get(id); }
        @Override public boolean update(Prestamo p) { revisar(); if (!datos.containsKey(p.getId())) return false; datos.put(p.getId(), p); return true; }
        @Override public boolean delete(int id) { revisar(); return datos.remove(id) != null; }

        /**
         * A propósito NO es atómico: lee el stock, "tarda" y recién después lo descuenta. Si el controlador no
         * sincronizara, varios hilos verían el mismo stock y se prestarían más libros de los que existen.
         */
        @Override public boolean prestarLibro(Prestamo p) {
            revisar();
            Libro libro = libros.datos.get(p.getIdLibro());
            if (libro == null || libro.getStock() <= 0) return false;
            int stockLeido = libro.getStock();
            dormir(latenciaMs);
            libro.setStock(stockLeido - 1);
            create(p);
            return true;
        }

        @Override public boolean devolverLibro(int id) {
            revisar();
            Prestamo p = datos.get(id);
            if (p == null || p.isDevuelto()) return false;
            p.setDevuelto(true);
            Libro libro = libros.datos.get(p.getIdLibro());
            libro.setStock(libro.getStock() + 1);
            return true;
        }

        @Override public List<Prestamo> readByEstudiante(int id) {
            revisar();
            return datos.values().stream().filter(p -> p.getIdEstudiante() == id)
                    .sorted(Comparator.comparingInt(Prestamo::getId).reversed()).collect(Collectors.toList());
        }
        @Override public List<Prestamo> readActivos() {
            revisar();
            return datos.values().stream().filter(p -> !p.isDevuelto()).collect(Collectors.toList());
        }
        @Override public List<Prestamo> readActivosByEstudiante(int id) {
            return readActivos().stream().filter(p -> p.getIdEstudiante() == id).collect(Collectors.toList());
        }
        @Override public int countActivosByEstudiante(int id) { return readActivosByEstudiante(id).size(); }
        @Override public boolean existeActivo(int idEst, int idLibro) {
            return readActivosByEstudiante(idEst).stream().anyMatch(p -> p.getIdLibro() == idLibro);
        }
        @Override public int countByEstudiante(int id) { revisar(); return (int) datos.values().stream().filter(p -> p.getIdEstudiante() == id).count(); }
        @Override public int countByLibro(int id) { revisar(); return (int) datos.values().stream().filter(p -> p.getIdLibro() == id).count(); }
        @Override public List<LibroPrestado> readLibrosMasPrestados(int limite) {
            revisar();
            Map<Integer, Long> cuenta = datos.values().stream()
                    .collect(Collectors.groupingBy(Prestamo::getIdLibro, Collectors.counting()));
            return cuenta.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .limit(limite)
                    .map(en -> new LibroPrestado(libros.datos.get(en.getKey()).getTitulo(), "Autor Prueba", en.getValue().intValue()))
                    .collect(Collectors.toList());
        }

        private void decorar(Prestamo p) {
            Estudiante e = estudiantes.datos.get(p.getIdEstudiante());
            Libro l = libros.datos.get(p.getIdLibro());
            p.setNombreEstudiante(e == null ? null : e.getNombre());
            p.setTituloLibro(l == null ? null : l.getTitulo());
        }
    }
}
