# Estado del proyecto

- Fuente funcional auditada:
  `/home/n95/gDrive/gitHub/BookReaderTranslator`.
- Base Android madura:
  `/home/n95/gDrive/gitHub/gitHub TTS Reader`.
- Proyecto final:
  `/home/n95/gDrive/gitHub/BookReader`.
- Objetivo: lector Android tipo Kindle para TXT, Markdown y PDF, sin ninguna
  función ni referencia de traducción.
- Estado: V1 terminada, compilada y auditada estáticamente; no había un
  dispositivo Android conectado para prueba física.

## Criterios de cierre

- Biblioteca navegable por carpetas y subcarpetas mediante SAF.
- Lectura de `.txt`, `.md`, `.markdown` y `.pdf`.
- Markdown renderizado, no mostrado como texto crudo.
- PDF renderizado por páginas con navegación.
- Restauración automática del último libro.
- Posición independiente por libro:
  - offset y contexto para TXT/Markdown;
  - número de página para PDF.
- Tamaño de letra configurable para TXT/Markdown.
- Selección y copia de texto.
- Backup JSON de posiciones y estados.
- Cero referencias a traducción en código, interfaz y recursos.
- APK `BookReader-v1.apk`, paquete `com.ricardo.bookreader`,
  `versionCode 1` / `versionName v1`.

## Verificación completada

- `lintDebug`: 0 errores.
- Pruebas unitarias: 3/3 correctas para detección TXT/Markdown/PDF.
- Compilación `assembleDebug`: correcta.
- Búsqueda en código, interfaz y recursos:
  0 referencias a traducción.
- Paquete `com.ricardo.bookreader`.
- `versionCode 1` / `versionName v1`.
- SDK mínimo 24 / objetivo 34.
- APK alineada y firma v2 válida.
- SHA-256 APK:
  `1341f586c78391f118362f70d1285137a149eebce058079b6178295b08a8d206`.
