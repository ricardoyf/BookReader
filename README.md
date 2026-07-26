# BookReader v3

Lector Android offline con una experiencia de páginas inspirada en un Kindle.

## Funciones

- Biblioteca de carpetas y subcarpetas mediante el selector seguro de Android.
- Lectura de TXT, Markdown y PDF.
- TXT y Markdown paginados según el tamaño real de pantalla y de letra.
- Avance y retroceso por botones o gesto horizontal; no se usa scroll para leer.
- PDF mostrado como página completa ajustada a la pantalla.
- Memoria independiente de la posición de cada libro.
- Cambiar al libro anterior o siguiente conserva y restaura la página exacta, aunque el libro esté sin terminar.
- Un libro solo se marca automáticamente como leído al avanzar desde su última página.
- Reapertura automática del último libro.
- Botón post-it para marcar o desmarcar la página actual.
- Vista «solo páginas marcadas» para repasar los post-it.
- Exportación de las páginas marcadas a un PDF compartible con:
  - nombre del libro;
  - número de página original;
  - imagen de la página original en PDF;
  - texto de la página en TXT y Markdown.
- Las marcas se incluyen en la copia de seguridad JSON.
- Ajuste del tamaño de letra, selección/copia, leído/no leído y TTS de TXT/Markdown.
- Sin traducción, vocabulario, historial de traducciones ni modelos asociados.

## Identidad Android

- Paquete: `com.ricardo.bookreader`
- Version code: `3`
- Version name: `v3`
- Android mínimo: API 24
- Target: API 34

## Compilar

```bash
./gradlew assembleDebug
```

El APK de entrega se guarda como `BookReader-v3.apk` en la raíz del proyecto.
