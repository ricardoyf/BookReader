# BookReader v1

Lector Android offline orientado a una experiencia tipo Kindle.

## Funciones

- Biblioteca de carpetas y subcarpetas mediante el selector seguro de Android.
- Lectura de TXT, Markdown y PDF.
- Markdown renderizado con tablas.
- PDF por páginas, con navegación anterior/siguiente.
- Memoria independiente de la posición de cada libro:
  - posición exacta y contexto de texto en TXT/Markdown;
  - última página en PDF.
- Reapertura automática del último libro.
- Ajuste del tamaño de letra.
- Selección y copia de texto.
- Marcado leído/no leído.
- Importación y exportación JSON de posiciones y estados.
- Lectura en voz alta de TXT/Markdown mediante el TTS del sistema.
- Sin traducción, vocabulario, historial de traducciones ni modelos asociados.

## Identidad Android

- Paquete: `com.ricardo.bookreader`
- Version code: `1`
- Version name: `v1`
- Android mínimo: API 24
- Target: API 34

## Compilar

```bash
./gradlew assembleDebug
```

El APK de entrega se guarda como `BookReader-v1.apk` en la raíz del proyecto.
