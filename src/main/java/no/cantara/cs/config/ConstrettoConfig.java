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
 * Application configuration, read from properties files and system properties.
 *
 * <p>Replaces Constretto. {@code constretto-spring} never shipped a release
 * compatible with Spring 6 &mdash; 2.2.3 is the only version that exists, and it
 * references {@code InstantiationAwareBeanPostProcessorAdapter}, which Spring 6
 * removed &mdash; so it blocked this service from moving off Spring 5. Nothing
 * here needed Constretto's tagged/environment features; it was used to layer
 * properties, which is a few lines of {@link Properties}.
 *
 * <p>Precedence is Constretto's, measured against constretto-core 2.2.3 rather
 * than assumed, so behaviour is unchanged:
 * <ol>
 *   <li>a JVM system property ({@code -Dkey=value}) wins,</li>
 *   <li>then {@code ./config_override/application_override.properties}, if present,</li>
 *   <li>then the packaged {@code application.properties}.</li>
 * </ol>
 * A missing override file is normal and not an error. Asking for a key that is
 * set nowhere throws, as Constretto did, rather than handing back a null that
 * would surface much later somewhere unrelated.
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

    /** The effective value for a key, or null if it is set nowhere. */
    private static String lookup(String key) {
        // Read at call time, so a system property set after startup (as tests
        // do) is seen, exactly as it was when Constretto backed this class.
        String sys = System.getProperty(key);
        return sys != null ? sys : configuration.getProperty(key);
    }

    private static String required(String key) {
        String value = lookup(key);
        if (value == null) {
            throw new IllegalStateException("Missing configuration property: " + key);
        }
        return value;
    }

    public static String getString(String key) {
        return required(key);
    }

    public static Integer getInt(String key) {
        return Integer.valueOf(required(key).trim());
    }

    public static Integer getInt(String key, int defaultValue) {
        String value = lookup(key);
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
        return Boolean.parseBoolean(required(key).trim());
    }
}
