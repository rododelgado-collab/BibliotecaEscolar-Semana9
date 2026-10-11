package controlador;

import modelo.Estudiante;
import modelo.Usuario;

/**
 * Quién inició sesión: el usuario y, si su rol es estudiante, su ficha de estudiante (la que se usa en los préstamos).
 *
 * @param usuario    bibliotecario o usuario estudiante
 * @param estudiante ficha del estudiante vinculada por RUT; null para el bibliotecario
 * @author Rodolfo Delgado
 */
public record Sesion(Usuario usuario, Estudiante estudiante) {
}
