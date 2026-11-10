# Sound Detector — Fabric 1.21.11

Source prototype; not compiled or tested in Minecraft. Dependency downloads were blocked by DNS in the authoring environment.

Shows up to eight recent positional sound events, with name, direction relative to your view, vertical direction, distance and source coordinates. Entries expire after five seconds. Repeated nearby sounds are merged. History resets when the world changes. Relative UI sounds and non-positional sounds are excluded. Detection is capped at 128 blocks and cannot extend what the server sends.

The coordinates belong to the sound event, not necessarily a player's current position. Your own footsteps can appear. Moving sources are not tracked after the event. This does not listen to your microphone or analyze audio recordings.

## Build and install

1. Install JDK 21 and Gradle 9.2.1 (https://gradle.org/releases/).
2. Open a terminal in this folder and run `gradle build`.
3. Copy `build/libs/sound-detector-0.1.0.jar` into your Minecraft `mods` folder. Do not use the sources JAR.
4. Launch Minecraft 1.21.11 with Fabric Loader 0.18.4 or newer and Fabric API for 1.21.11.

No Gradle wrapper is included. The first build needs internet access to download dependencies.

## Controls

- `/sounddetector`: toggle the HUD and collection on/off.
- `/sounddetector clear`: clear recent history.
- F1 hides the HUD along with the normal Minecraft HUD.

## Manual check after building

In a creative test world with commands enabled, run `/playsound minecraft:block.anvil.land master @s ~5 ~ ~ 1 1`. Turn around to check that the direction changes while the source coordinates stay fixed. Verify history disappears after five seconds, toggle works, and changing worlds clears the list.

## Technical references

- https://maven.fabricmc.net/docs/yarn-1.21.11+build.4/net/minecraft/client/sound/SoundInstanceListener.html
- https://maven.fabricmc.net/docs/yarn-1.21.11+build.4/net/minecraft/client/sound/SoundManager.html
- https://fabricmc.net/2025/12/05/12111.html
