# NPC Mod (Forge 1.20.1)

NPCs editables con menú de fantasía: ítem para invocarlos, ítem para editarlos, modelos `.geo.json` (BBS mod / Blockbench), diálogos con preguntas, voz, sonidos, comandos y equipamiento.

## Descargar el .jar
1. Sube todo este proyecto a un repositorio de GitHub (rama `main`).
2. En la pestaña **Actions** se compila solo (tarda ~3-5 min).
3. El `.jar` queda en **Releases** (lado derecho del repo) y también como artefacto `npcmod-jar` dentro de la ejecución.
4. Ponlo en la carpeta `mods` de Forge 1.20.1 (versión 47.x o superior).

## Uso
- **Invocador de NPC**: clic derecho en el suelo.
- **Varita de edición**: clic derecho sobre un NPC abre el editor (requiere ser OP / trucos).
- Clic derecho en el NPC con la mano vacía muestra el diálogo.

## Carpetas del cliente (`.minecraft/npcmod/`)
- `models/`  → `nombre.geo.json`
- `textures/` → `nombre.png` (texturas de los modelos)
- `images/`  → `nombre.png` (fondos de la caja y retratos)
