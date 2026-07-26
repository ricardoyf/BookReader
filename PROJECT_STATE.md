# BookReader v3 — estado de trabajo

Objetivo: corregir la pérdida de posición al cambiar de libro antes de terminarlo, conservando todas las funciones de BookReader v2.

## Criterios de cierre

- TXT y Markdown se dividen según el tamaño real de la pantalla y de la fuente.
- PDF muestra la página completa ajustada a pantalla, sin scroll.
- La posición de lectura se restaura por libro.
- El cambio mediante las flechas de libro guarda primero la posición actual y restaura siempre la del libro de destino.
- Un libro incompleto no se marca ni se renombra automáticamente como leído.
- Si un libro terminado se renombra como leído, su posición se migra a la nueva URI.
- Las marcas sobreviven al cierre de la app.
- El modo «solo marcadas» recorre únicamente las páginas señaladas.
- La exportación genera un PDF compartible; conserva visualmente las páginas PDF y transcribe las páginas de texto.
- No hay funciones ni cadenas de traducción.
- Lint sin errores, tests unitarios aprobados y APK v3 firmada.

## Estado

Completado el 26/07/2026.

## Corrección principal de v3

- Al cambiar al documento siguiente o anterior se guarda primero la página actual.
- Al volver a abrir cualquier documento se restaura siempre su posición guardada.
- Un libro solo se renombra como leído al alcanzar realmente su última página.
- Si el archivo se renombra como leído, su posición y sus páginas marcadas migran al nuevo URI.
- Las escrituras de posición se encadenan para evitar que una actualización antigua sobrescriba otra más reciente.

## Verificación

- Tests unitarios: 9/9 correctos.
- Android Lint: 0 errores; 14 avisos informativos de versiones de dependencias.
- APK: `BookReader-v3.apk`.
- Paquete: `com.ricardo.bookreader`.
- Versión: `versionCode 3`, `versionName v3`.
- APK alineado correctamente.
- Firma APK Signature Scheme v2 verificada.
- SHA-256: `780c54ebd49cb79080806d38d80fc69810e2da81dde6e36ff8fcf721d02bc529`.
- Auditoría de traducción: sin funciones ni referencias funcionales de traducción.
- No había dispositivo Android conectado para realizar una prueba física.
