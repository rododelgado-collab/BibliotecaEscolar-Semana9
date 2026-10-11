package util;

import java.util.regex.Pattern;

/**
 * Validaciones de formato que comparten los controladores. Lanzan IllegalArgumentException con un mensaje
 * listo para mostrar al usuario.
 *
 * @author Rodolfo Delgado
 */
public final class Validador {

    private static final Pattern RUT = Pattern.compile("^\\d{7,8}-[\\dK]$");
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern ISBN = Pattern.compile("^(\\d{13}|\\d{9}[\\dX])$");

    private Validador() {
    }

    /** Texto obligatorio, sin espacios sobrantes y con largo máximo. @return el texto limpio */
    public static String texto(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar " + campo + ".");
        }
        String limpio = valor.trim().replaceAll("\\s+", " ");
        if (limpio.length() < minimo) {
            throw new IllegalArgumentException(capitalizar(campo) + " debe tener al menos " + minimo + " caracteres.");
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(capitalizar(campo) + " no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }

    /** RUT chileno con guion (12345678-9); no valida el dígito verificador. @return el RUT normalizado */
    public static String rut(String valor) {
        String limpio = texto(valor, "el RUT", 1, 12).replace(".", "").toUpperCase();
        if (!RUT.matcher(limpio).matches()) {
            throw new IllegalArgumentException("El RUT no es válido. Use el formato 12345678-9 (sin puntos y con guion).");
        }
        return limpio;
    }

    public static String correo(String valor) {
        String limpio = texto(valor, "el correo", 5, 100).toLowerCase();
        if (!CORREO.matcher(limpio).matches()) {
            throw new IllegalArgumentException("El correo no es válido. Ejemplo: nombre@correo.cl");
        }
        return limpio;
    }

    /** ISBN-10 o ISBN-13; acepta guiones y espacios. @return el ISBN sin separadores */
    public static String isbn(String valor) {
        String limpio = texto(valor, "el ISBN", 1, 30).replace("-", "").replace(" ", "").toUpperCase();
        if (!ISBN.matcher(limpio).matches()) {
            throw new IllegalArgumentException("El ISBN no es válido. Debe tener 10 o 13 dígitos (se aceptan guiones).");
        }
        return limpio;
    }

    /** Entero entre minimo y maximo, escrito como texto en un campo de la ventana. */
    public static int entero(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar " + campo + ".");
        }
        int numero;
        try {
            numero = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(capitalizar(campo) + " debe ser un número entero.");
        }
        if (numero < minimo || numero > maximo) {
            throw new IllegalArgumentException(capitalizar(campo) + " debe estar entre " + minimo + " y " + maximo + ".");
        }
        return numero;
    }

    private static String capitalizar(String texto) {
        // "el RUT" -> "El RUT"
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
