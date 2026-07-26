# BookReader v2 — estado de trabajo

Objetivo cerrado: entregar una APK v2 que conserve las funciones de la v1 y añada lectura sin desplazamiento vertical, navegación página a página en TXT, Markdown y PDF, post-it persistente por página, modo de repaso de páginas marcadas y exportación PDF con el libro y número de página original.

## Criterios de cierre

- TXT y Markdown se dividen según el tamaño real de la pantalla y de la fuente.
- PDF muestra la página completa ajustada a pantalla, sin scroll.
- La posición de lectura se restaura por libro.
- Las marcas sobreviven al cierre de la app.
- El modo «solo marcadas» recorre únicamente las páginas señaladas.
- La exportación genera un PDF compartible; conserva visualmente las páginas PDF y transcribe las páginas de texto.
- No hay funciones ni cadenas de traducción.
- Lint sin errores, tests unitarios aprobados y APK v2 firmada.

## Estado

Completado el 26/07/2026.

- `lintDebug`: 0 errores; 14 avisos informativos de versiones disponibles.
- Tests unitarios: 6 aprobados, 0 fallos.
- APK: `BookReader-v2.apk`.
- Paquete: `com.ricardo.bookreader`.
- Versión: code 2 / name v2.
- Firma APK Signature Scheme v2 verificada.
- SHA-256: `13a5523175abd35e1d1d2567c15774a1ab0ef00c20574b56607b2e6f05e23281`.
- Auditoría de traducción/vocabulario: 0 coincidencias funcionales.
