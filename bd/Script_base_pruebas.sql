-- Base EXCLUSIVA para las pruebas unitarias de los DAO (no tiene datos de ejemplo).
-- Misma estructura que Script_crea_tablas_biblioteca.sql, pero en la base biblioteca_test.
-- Las pruebas borran todas las filas antes de cada test, por eso nunca deben apuntar a la base "biblioteca".

CREATE DATABASE IF NOT EXISTS biblioteca_test;
USE biblioteca_test;

CREATE TABLE usuarios (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100),
    rut VARCHAR(12) UNIQUE,
    correo VARCHAR(100),
    contraseña VARCHAR(100),
    rol ENUM('bibliotecario', 'estudiante') NOT NULL
);

CREATE TABLE estudiantes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100),
    rut VARCHAR(12) UNIQUE,
    curso VARCHAR(20),
    correo VARCHAR(100)
);

CREATE TABLE categorias (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100)
);

CREATE TABLE libros (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titulo VARCHAR(200),
    autor VARCHAR(100),
    isbn VARCHAR(20) UNIQUE,
    editorial VARCHAR(100),
    stock INT,
    id_categoria INT,
    FOREIGN KEY (id_categoria) REFERENCES categorias(id)
);

CREATE TABLE prestamos (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_estudiante INT,
    id_libro INT,
    fecha_prestamo DATE,
    fecha_devolucion DATE,
    devuelto BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id),
    FOREIGN KEY (id_libro) REFERENCES libros(id)
);
