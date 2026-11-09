# HorizonTV — proyecto Android inicial

Aplicación Android para celulares y Android TV, con experiencia sencilla tipo decodificador.

## Configuración incluida

- Bienvenida de 15 segundos con barra de progreso azul metalizado.
- Reproducción automática del canal 001: Caracol TV.
- Reproductor HLS/M3U8 basado en Android Media3 ExoPlayer.
- Pantalla completa y orientación horizontal.
- Cambio de canal con CH+ / CH− del control remoto.
- Botones táctiles CH+ / CH− en la pantalla.
- Oculta la información del canal después de cinco segundos.
- Parrilla depurada: 47 canales de usuario a partir de las 71 entradas originales.
- Archivo `parrilla_limpia.m3u` y configuración `channels.json`.
- URL EPG/XMLTV detectada en la lista original, preparada en la configuración.
- Flujo de GitHub Actions para compilar un APK de prueba.

## Depuración realizada

Se excluyeron entradas técnicas MPTS, listas genéricas sin nombre de canal y etiquetas genéricas (`playlist.m3u8`, `index.m3u8`, `antena`, `caracol` y `TV-101`). Se unificaron identificadores EPG repetidos, conservando una sola entrada por `tvg-id`. Caracol TV quedó como canal 001 y RCN como canal 002.

La selección es una depuración por metadatos y nombres, no una validación de disponibilidad. Los 47 enlaces deben probarse desde la red donde se utilizará HorizonTV. Los enlaces HTTP pueden fallar si el proveedor cambia direcciones, exige autenticación o restringe el acceso.

## Abrir y compilar

1. Descomprime el ZIP.
2. Sube la carpeta del proyecto a un repositorio GitHub nuevo.
3. Conserva la estructura de carpetas y el archivo `.github/workflows/build-apk.yml`.
4. En GitHub abre **Actions**.
5. Ejecuta **Compilar HorizonTV APK** (o haz un `push` a `main`/`master`).
6. Al finalizar, descarga el artefacto **HorizonTV-debug**.

También puedes abrir la carpeta con Android Studio (JDK 17 y Android SDK 35).

## Editar canales

Edita `app/src/main/assets/channels.json`. Cada canal contiene:

- `number`: número que se muestra al usuario.
- `name`: nombre visible.
- `url`: dirección M3U8.
- `category`: categoría informativa.
- `tvg_id`: identificador para enlazar la guía EPG.

El canal de inicio se define con `startup_channel` (actualmente 1). Mantén números únicos y direcciones M3U8 válidas.

## EPG

La lista original declara esta guía XMLTV:

`https://iptv-org.github.io/epg/guides/latam.epg.xml.gz`

En esta primera entrega queda registrada la fuente y los `tvg-id`, pero **la interfaz de programación EPG todavía no está implementada**. Se incorporará en una fase posterior, asociando la programación por `tvg-id` y mostrando programa actual y siguiente cuando exista coincidencia.

## Importante

- Este ZIP contiene el **código fuente del proyecto**, no un APK ya compilado.
- La primera compilación es `debug`, destinada a pruebas; antes de distribuirla ampliamente se debe preparar firma `release`.
- La app reproduce las direcciones de la parrilla; no aloja ni retransmite contenido.
- Utiliza únicamente señales y listas para las que tengas autorización de acceso y distribución.
- Se habilitó tráfico HTTP sin cifrar porque los enlaces recibidos usan HTTP. Para producción es preferible que el proveedor entregue HTTPS.
