# CR-V Launcher · Eduardo (v2)

Proyecto Android Studio en orientación horizontal, diseñado para la pantalla 1280×720 del estéreo.

## Funciones integradas
- **Waze Live Map** embebido e interactivo, centrado con la ubicación del estéreo cuando se concede permiso. El botón abre Waze para navegación giro a giro.
- **Música**: selector de YouTube Music, Spotify, Apple Music u otra aplicación. El código nativo consulta sesiones Android MediaSession, muestra título/artista/álbum y envía anterior, reproducir/pausar y siguiente. Requiere habilitar el acceso a notificaciones para CR-V Launcher.
- **Clima en vivo**: Open-Meteo, con ubicación del dispositivo y sin clave API.
- **Radio FM**: detecta aplicaciones instaladas cuyo nombre contenga Radio o FM y permite abrir una.
- **Android Auto**: intenta iniciar el paquete de Android Auto si ofrece una actividad de inicio. Algunos estéreos lo gestionan como servicio de proyección, por lo que el botón puede no abrir una pantalla independiente.
- Accesos de Apps, Bluetooth, teléfono y ajustes.

## Compilar
1. Abre esta carpeta en Android Studio.
2. Acepta la sincronización de Gradle (requiere internet).
3. Selecciona **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.
5. Instala y selecciona **CR-V Launcher Eduardo** como aplicación de inicio.

Este entorno no tiene Android SDK/Gradle instalados, por lo que entrego el proyecto fuente para compilar; no afirmo que exista un APK compilado.

## Permisos al primer uso
- Permitir ubicación para centrar el mapa y calcular el clima local.
- Al seleccionar reproductor, abrir Acceso a notificaciones y activar **CR-V Launcher · Control multimedia**. Después inicia la música en la app elegida.

## Notas
- El mapa Waze embebido es el Live Map web. Para rutas, búsqueda, indicaciones y alertas completas se abre la app Waze.
- El control musical funciona solo cuando Android publica una sesión multimedia activa y el usuario concede el acceso solicitado.
- El clima depende de internet y GPS.
- La Radio FM del fabricante puede no aparecer en la búsqueda automática si su etiqueta no incluye “Radio” o “FM”; en ese caso habrá que ajustar el paquete de esa app.

### Iconos oficiales de aplicaciones (v2.1)
La interfaz ahora solicita al puente nativo Android los iconos reales de los paquetes instalados (Waze, YouTube Music, Spotify, Android Auto y Radio). La aplicación los convierte a PNG y los muestra en los espacios originales del diseño. En una previsualización web independiente no están disponibles; se muestran dentro del APK instalado.


### Vista previa en teléfono
La interfaz se adapta a pantallas verticales para previsualización móvil sin rotar el contenido. En el estéreo (1280×720 horizontal) conserva el diseño panorámico. La imagen del vehículo está incrustada en el HTML para evitar rutas rotas.


### Iconos visibles en vista previa web (v2.3)
Los iconos de Waze, Spotify, YouTube Music, Android Auto y Radio incluyen SVG de respaldo embebidos. Así se muestran también en Chrome sin AndroidBridge; dentro del APK, si Android devuelve el icono instalado, este sustituye al SVG de respaldo.


## Cajón de aplicaciones v3.0
Se añadió un cajón personalizado de cuatro páginas con las 38 aplicaciones y accesos visibles en las fotografías del equipo. Incluye navegación por puntos y deslizamiento, iconos vectoriales modernos como respaldo y, dentro del APK, consulta de iconos y apertura de aplicaciones por nombre instalado mediante Android PackageManager. Algunos accesos del fabricante (SWC, DAB+, PODOFO, USBTVout, etc.) dependen de que el equipo exponga una actividad iniciable; si el fabricante no publica un acceso directo, Android mostrará un aviso.
