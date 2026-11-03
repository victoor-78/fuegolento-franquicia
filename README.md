# Fuego Lento Franquicia

Aplicación web de pedidos para una franquicia de restaurantes, desarrollada con
Spring Boot y MySQL. Es el sistema operacional que alimenta el Data Warehouse
del Trabajo de Fin de Grado "Fuego Lento", cuyo proceso ETL está en el
repositorio [Etl](https://github.com/victoor-78/Etl).

La aplicación modela una franquicia de 18 sedes (13 en Madrid y 5 en otras
ciudades) con carta compartida y disponibilidad de platos por restaurante.

## Requisitos

- Java 21
- Docker (para MySQL)

## Puesta en marcha

1. Arrancar MySQL en un contenedor:

   ```
   docker run --name fuegolento-franquicia-mysql -e MYSQL_ROOT_PASSWORD=<contraseña> -e MYSQL_DATABASE=fuegolento_franquicia -p 3309:3306 -d mysql:8.0
   ```

2. Crear el archivo `src/main/resources/application-local.properties` (no se
   guarda en el repositorio) con las credenciales de MySQL:

   ```
   spring.datasource.username=root
   spring.datasource.password=<contraseña>
   ```

   También pueden pasarse con las variables de entorno `DB_USERNAME` y
   `DB_PASSWORD`.

3. Arrancar la aplicación desde la carpeta del proyecto:

   ```
   .\mvnw spring-boot:run
   ```

En el primer arranque se crean las tablas y se cargan los datos de muestra: las
18 sedes, la carta de 26 platos, clientes de ejemplo y un histórico de unos
1.500 pedidos entregados repartidos en un año. La aplicación queda disponible
en http://localhost:8080. Al iniciar sesión se elige el restaurante con el que
se va a trabajar; la cuenta de administración de ejemplo es `admin`.