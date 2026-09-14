package no.cantara.cs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Application configuration, read from properties files.
 *
 * <p>Replaces Constretto. {@code constretto-spring} never shipped a release
 * compatible with Spring 6 &mdash; 2.2.3 is the only version that exists, and it
 * references {@code InstantiationAwareBeanPostProcessorAdapter}, which Spring 6
 * removed &mdash; so it blocked this service from moving off Spring 5. Nothing
 * here needed Constretto's tagged/environment features; it was used purely to
 * layer two properties files, which is a few lines of {@link Properties}.
 *
 * <p>Load order is unchanged, so behaviour is unchanged: the packaged
 * {@code application.properties} first, then
 * {@code ./config_override/application_override.properties} if present, whose
 * values win. A missing override file is normal and not an error.
 *
 * <p>The public API is deliberately identical to what Constretto backed, so
 * callers did not change. The class keeps its old name for the same reason.
 */
public class ConstrettoConfig {

    private static final Logger log = LoggerFactory.getLogger(ConstrettoConfig.class);

    private static final String CLASSPATH_PROPERTIES = "application.properties";
    private static final String OVERRIDE_PROPERTIES =
            "./config_override/application_override.properties";

    private static final Properties configuration = load();

    private ConstrettoConfig() {}

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = ConstrettoConfig.class.getClassLoader()
                .getResourceAsStream(CLASSPATH_PROPERTIES)) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } else {
                log.warn("No {} on the classpath", CLASSPATH_PROPERTIES);
            }
        } catch (IOException e) {
            log.error("Could not read {} from the classpath", CLASSPATH_PROPERTIES, e);
        }

        Path override = Path.of(OVERRIDE_PROPERTIES);
        if (Files.isReadable(override)) {
            try (Reader r = Files.newBufferedReader(override, StandardCharsets.UTF_8)) {
                props.load(r);      // loaded second: overrides win, as before
                log.info("Applied configuration overrides from {}", override);
            } catch (IOException e) {
                log.error("Could not read overrides from {}", override, e);
            }
        }
        return props;
    }

    public static String getString(String key) {
        return configuration.getProperty(key);
    }

    public static Integer getInt(String key) {
        String value = configuration.getProperty(key);
        if (value == null) {
            // Constretto threw when a key it was asked to evaluate was absent.
            // Keep that: a silent null here would surface much later as an NPE
            // somewhere unrelated.
            throw new IllegalStateException("Missing configuration property: " + key);
        }
        return Integer.valueOf(value.trim());
    }

    public static Integer getInt(String key, int defaultValue) {
        String value = configuration.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Property {} is not a number ({}); using default {}",
                    key, value, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(
                configuration.getProperty(key, "false").trim());
    }
}
