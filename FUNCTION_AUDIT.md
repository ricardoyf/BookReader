# Auditoría funcional de BookReader v1

## Biblioteca

- Selector de carpeta mediante Storage Access Framework.
- Permiso persistente de acceso.
- Navegación por subcarpetas.
- Filtrado estricto de TXT, MD, Markdown y PDF.
- Apertura automática del último libro al iniciar.
- Marcado leído/no leído.
- Navegación al libro anterior y siguiente.

## TXT y Markdown

- Lectura UTF-8.
- Markdown renderizado con Markwon y soporte de tablas.
- Selección y copia de texto.
- Tamaño de letra entre 14 y 30 sp.
- Posición guardada por URI:
  offset de caracteres, contexto próximo y desplazamiento.
- Restauración centrada en el texto guardado, con búsqueda del contexto si el
  archivo ha cambiado ligeramente.
- Lectura en voz alta mediante el TTS local del sistema.

## PDF

- Renderizado nativo con `PdfRenderer`.
- Navegación página anterior/siguiente.
- Indicador `Página X de N`.
- Última página guardada por libro.
- Apertura directa desde SAF.
- Copia temporal automática como alternativa para proveedores que no entregan
  un descriptor PDF seekable.

## Persistencia y seguridad

- Última carpeta, archivo, tamaño de letra, leídos y posiciones en DataStore.
- Backup JSON importable/exportable.
- Sin permisos clásicos de almacenamiento.
- Funcionamiento offline.
- No contiene API, modelo, interfaz, historial ni código de traducción.
