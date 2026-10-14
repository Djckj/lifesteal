# Lifesteal + 3 Axes (Paper 26.2)

Build (JDK 25 + Maven): `mvn package` -> `target/lifesteal-1.0.0.jar` -> `plugins/`.
If you had an older config.yml, delete it so the new one is generated.

Commands
- `/withdraw [n]`  turn hearts into Heart items (right-click to use)
- `/lifesteal get|set|add|remove <player> [n]`, `revive <player>`
- `/lifesteal give <weapon> <player>` / `reset <weapon>` / `status`

Recipes (D = diamond block, E = enchanted golden apple, N = netherite ingot, A = netherite axe)
- Heart:         D N D / N T N / D N D  (T = totem of undying)
- Lifestealer:   D E D / E A E / D E D
- Sword Axe:     D D D / S A T / D D D  (S = netherite sword, T = upgrade template)
- Armor Breaker: D N D / E A E / D N D

Only one of each weapon can exist at a time. If one is destroyed (broken, burned, despawned,
void) it can be crafted again. Weapons drop on death and can be thrown / picked up.
