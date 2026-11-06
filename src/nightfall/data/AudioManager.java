// Developer: V.Viswa Vamsi
package nightfall.data;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sound effects manager. WAV bytes are loaded asynchronously from the classpath
 * (assets/audio) or the working directory, then each playback opens its own
 * short-lived Clip. Every failure is silent so the game never crashes over audio.
 */
public class AudioManager {

    public static final String SFX_CLICK = "button_click";
    public static final String SFX_LAUNCH = "laser_gun_shot";
    public static final String SFX_EXPLOSION = "blast";
    public static final String SFX_GAME_OVER = "game_over";
    public static final String SFX_VICTORY = "victory";
    public static final String SFX_MENU_OPEN = "main_page_opening";
    public static final String SFX_WAVE = "wave";

    private static final String[] PRELOAD = {
            SFX_CLICK, SFX_LAUNCH, SFX_EXPLOSION, SFX_GAME_OVER,
            SFX_VICTORY, SFX_MENU_OPEN, SFX_WAVE
    };

    private final Map<String, byte[]> clips = new ConcurrentHashMap<>();
    private final SettingsManager settings;
    private volatile boolean loading = false;

    public AudioManager(SettingsManager settings) {
        this.settings = settings;
    }

    /** Kicks off a daemon thread that preloads every known sound effect. */
    public void loadAsync() {
        if (loading || GraphicsEnvironment.isHeadless()) {
            return;
        }
        loading = true;
        Thread t = new Thread(() -> {
            for (String name : PRELOAD) {
                byte[] data = readSound(name);
                if (data != null) {
                    clips.put(name, data);
                }
            }
            loading = false;
        }, "nightfall-audio-loader");
        t.setDaemon(true);
        t.start();
    }

    public boolean isLoaded() {
        return !loading && clips.size() == PRELOAD.length;
    }

    /** Plays a sound effect once; ignores missing/disabled/broken audio. */
    public void play(String key) {
        try {
            if (settings == null || !settings.sfxOn || key == null) {
                return;
            }
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            byte[] data = clips.get(key);
            if (data == null) {
                data = readSound(key);
                if (data == null) {
                    return;
                }
                clips.put(key, data);
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new java.io.ByteArrayInputStream(data));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            applyGain(clip);
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Throwable ignored) {
            // Sound is decorative; never let it break the game loop.
        }
    }

    private void applyGain(Clip clip) {
        float vol = settings == null ? 0.8f : settings.volume;
        vol = Math.max(0f, Math.min(1f, vol));
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) (20.0 * Math.log10(Math.max(1e-4, vol)));
            ctrl.setValue(Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), db)));
        }
    }

    /** Loads raw bytes for a named effect from classpath, then filesystem. */
    private byte[] readSound(String name) {
        String file = name + ".wav";
        try {
            InputStream in = AudioManager.class.getClassLoader()
                    .getResourceAsStream("assets/audio/" + file);
            if (in == null) {
                in = AudioManager.class.getClassLoader().getResourceAsStream(file);
            }
            if (in != null) {
                byte[] data = drain(in);
                if (data != null && data.length > 0) {
                    return data;
                }
            }
        } catch (Throwable ignored) {
            // fall through to filesystem lookup
        }
        try {
            Path p = Paths.get("assets", "audio", file);
            if (Files.isRegularFile(p)) {
                byte[] data = Files.readAllBytes(p);
                if (data.length > 0) {
                    return data;
                }
            }
        } catch (Throwable ignored) {
            // missing sound is fine
        }
        return null;
    }

    private byte[] drain(InputStream in) {
        try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
