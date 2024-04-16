# Software Distribuït 2023-2024

## Pràctica 1: Client-Server

## Pasos de Ejecución

En este package Practica 1.
```bash 
mvn clean package
```

-p <port number> Es el puerto que se abrirá para escuchar conexiones
En el package Server primero
```bash 
java -jar target/Server-1.0-SNAPSHOT-jar-with-dependencies.jar -p 8080
```

-h <IP/localhost> Es la IP de la máquina a la que desea conectarse (siempre que esté en la misma red) o localhost en caso de que sea su propia máquina.
-p <port number> Es el puerto con que quieres conectar, tiene que ser el mismo que el que esta escuchando el Server.
En el package Client
```bash 
java -jar target/Client-1.0-SNAPSHOT-jar-with-dependencies.jar -h localhost-p 8080
```
