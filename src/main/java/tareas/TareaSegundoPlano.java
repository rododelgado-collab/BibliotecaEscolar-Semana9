package tareas;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tarea que se ejecuta en un HILO INDEPENDIENTE para que la interfaz no se congele mientras se registra un préstamo
 * (guardar el préstamo, actualizar el stock y dejar el historial del estudiante), o se registra una devolución.
 *
 * Implementa Runnable: {@link #lanzar} crea un Thread con ella y lo inicia. Al terminar llama a alExito o a alError
 * DESDE ESE HILO; la ventana que la usa es la responsable de pasar al hilo de Swing (SwingUtilities.invokeLater).
 *
 * @param <T> lo que devuelve el trabajo
 * @author Rodolfo Delgado
 */
public class TareaSegundoPlano<T> implements Runnable {

    private final Supplier<T> trabajo;
    private final Consumer<T> alExito;
    private final Consumer<RuntimeException> alError;

    public TareaSegundoPlano(Supplier<T> trabajo, Consumer<T> alExito, Consumer<RuntimeException> alError) {
        this.trabajo = trabajo;
        this.alExito = alExito;
        this.alError = alError;
    }

    @Override
    public void run() {
        T resultado;
        try {
            resultado = trabajo.get();
        } catch (RuntimeException e) {
            alError.accept(e);
            return;
        }
        alExito.accept(resultado);
    }

    /** Crea el hilo con un nombre descriptivo y lo inicia. @return el hilo (por si quien llama quiere esperarlo) */
    public static <T> Thread lanzar(String nombreHilo, Supplier<T> trabajo, Consumer<T> alExito,
                                    Consumer<RuntimeException> alError) {
        Thread hilo = new Thread(new TareaSegundoPlano<>(trabajo, alExito, alError), nombreHilo);
        hilo.setDaemon(true);   // no impide cerrar la aplicación
        hilo.start();
        return hilo;
    }
}
