package com.romeo.paper.services;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Shared business logic for the server list icon. Used by {@code /editicon}
 * and the settings GUI. Network work runs async; any Bukkit-facing result is
 * delivered on the main thread via the provided callback.
 */
public final class IconService {
    public static final String CACHED_KEY = "server-list.cached-icon";
    private static final String USER_AGENT = "RomeoPaperPlugin/1.0";

    private final JavaPlugin plugin;
    private final ConfigService config;

    public IconService(JavaPlugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Human-readable cache status for GUI display. */
    public String status() {
        String path = config.getString(CACHED_KEY, null);
        if (path == null || path.isEmpty()) {
            return "none";
        }
        return iconFile().exists() ? "cached" : "missing";
    }

    /** Absolute path of the cached icon file (may not exist yet). */
    public File iconFile() {
        return new File(plugin.getDataFolder(), "server-icon.png");
    }

    /**
     * Downloads, resizes to 64x64 and stores a server icon. The download and
     * image conversion happen off the main thread; the callback receives
     * success/failure on the main thread and must be safe to show to a player.
     */
    public void downloadAsync(final String url, final Callback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Exception failure = null;
                try {
                    downloadAndConvert(new URL(url), iconFile());
                } catch (Exception exception) {
                    failure = exception;
                }
                final Exception finalFailure = failure;
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (finalFailure == null) {
                            config.set(CACHED_KEY, iconFile().getAbsolutePath());
                            callback.onDone(true, null);
                        } else {
                            callback.onDone(false, friendly(finalFailure));
                        }
                    }
                });
            }
        });
    }

    /** Clears the cached icon path and deletes the file if present. */
    public boolean reset() {
        File icon = iconFile();
        if (icon.exists() && !icon.delete()) {
            plugin.getLogger().warning("Could not delete cached server icon: " + icon.getAbsolutePath());
        }
        return config.clear(CACHED_KEY);
    }

    private void downloadAndConvert(URL url, File target) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try (InputStream input = connection.getInputStream()) {
            BufferedImage source = ImageIO.read(input);
            if (source == null) {
                throw new IllegalArgumentException("URL is not an image");
            }
            BufferedImage output = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            graphics.drawImage(source.getScaledInstance(64, 64, Image.SCALE_SMOOTH), 0, 0, null);
            graphics.dispose();
            target.getParentFile().mkdirs();
            ImageIO.write(output, "PNG", target);
        }
    }

    private static String friendly(Exception exception) {
        String message = exception.getMessage();
        if (exception instanceof IllegalArgumentException) {
            return "Invalid URL (not an image).";
        }
        return message == null ? "Icon download failed." : "Icon download failed: " + message;
    }

    /** Called on the main thread when a download attempt finishes. */
    public interface Callback {
        void onDone(boolean success, String message);
    }
}
