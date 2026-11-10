package dev.miles.sounddetector;

import java.util.ArrayDeque;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

public final class SoundDetector implements ClientModInitializer {
    private record Hit(String name, double x, double y, double z, long time) {}
    private final ArrayDeque<Hit> hits = new ArrayDeque<>();
    private boolean enabled = true;
    private boolean listenerRegistered;
    private Object lastWorld;
    private static final long LIFETIME = 5_000_000_000L;

    @Override public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (!listenerRegistered && c.getSoundManager() != null) {
                listenerRegistered = true;
                c.getSoundManager().registerListener((sound, set, range) -> {
            // Capture immutable event data before passing it to the client thread.
            if (sound.isRelative() || sound.getAttenuationType() == SoundInstance.AttenuationType.NONE
                    || sound.getCategory() == SoundCategory.MUSIC || sound.getVolume() <= 0) return;
            Hit hit = new Hit(sound.getId().getPath(), sound.getX(), sound.getY(), sound.getZ(), System.nanoTime());
            Object world = client.world;
            client.execute(() -> {
                if (!enabled || world == null || world != client.world || client.player == null) return;
                if (!Double.isFinite(hit.x) || !Double.isFinite(hit.y) || !Double.isFinite(hit.z)) return;
                double dx = hit.x - client.player.getX(), dy = hit.y - client.player.getEyeY(), dz = hit.z - client.player.getZ();
                if (dx*dx + dy*dy + dz*dz > 128*128) return;
                hits.removeIf(old -> old.name.equals(hit.name) && Math.abs(old.x-hit.x)<1 && Math.abs(old.y-hit.y)<1 && Math.abs(old.z-hit.z)<1);
                hits.addFirst(hit);
                while (hits.size() > 8) hits.removeLast();
            });
                });
            }
            if (lastWorld != c.world) { hits.clear(); lastWorld = c.world; }
            long now = System.nanoTime();
            hits.removeIf(h -> now - h.time > LIFETIME);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
            dispatcher.register(literal("sounddetector").executes(ctx -> {
                enabled = !enabled;
                hits.clear();
                ctx.getSource().sendFeedback(Text.literal("Sound Detector: " + (enabled ? "ON" : "OFF")));
                return 1;
            }).then(literal("clear").executes(ctx -> { hits.clear(); return 1; }))));
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private void render(DrawContext context) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (!enabled || c.player == null || c.options.hudHidden) return;
        int width = Math.min(360, context.getScaledWindowWidth()-16);
        int rows = Math.min(hits.size(), Math.max(0, (context.getScaledWindowHeight()-44)/26));
        context.fill(6, 6, 6+width, 30+rows*26, 0xB0182029);
        context.drawText(c.textRenderer, "SOUND DETECTOR  |  /sounddetector", 12, 12, 0xFF66D9CC, true);
        int y = 30, index = 0;
        for (Hit h : hits) {
            if (index++ >= rows) break;
            double dx=h.x-c.player.getX(), dz=h.z-c.player.getZ(), dy=h.y-c.player.getEyeY();
            double bearing=Math.toDegrees(Math.atan2(-dx, dz));
            double angle=((bearing-c.player.getYaw())%360+540)%360-180;
            String[] directions={"FRONT", "FRONT-RIGHT", "RIGHT", "BACK-RIGHT", "BACK", "BACK-LEFT", "LEFT", "FRONT-LEFT"};
            int sector = Math.floorMod((int)Math.round(angle/45), 8);
            String detail=String.format(Locale.ROOT, "%s%s  %.1fm  [%.0f, %.0f, %.0f]", directions[sector], dy>2?" UP":dy< -2?" DOWN":"", Math.sqrt(dx*dx+dy*dy+dz*dz), h.x,h.y,h.z);
            context.drawText(c.textRenderer, c.textRenderer.trimToWidth(h.name, width-12), 12,y,0xFFFFFFFF,true);
            context.drawText(c.textRenderer, c.textRenderer.trimToWidth(detail, width-12),12,y+11,0xFFB2C3D1,true);
            y+=26;
        }
    }
}
