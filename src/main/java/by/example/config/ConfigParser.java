package by.example.config;

import by.example.exception.ConfigParsingException;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class ConfigParser {
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]+))?\\}");
    private final Map<String, Object> properties = new HashMap<>();

    public ConfigParser(String defaultFile) {
        loadProperties(defaultFile, "");
    }

    public ConfigParser(String defaultFile, String profile) {
        loadProperties(defaultFile, profile);
    }

    private void loadProperties(String defaultFile, String profile) {
        try {
            log.info("Loading properties from {}", defaultFile);
            log.info("Loading properties from: " + defaultFile);
            Map<String, Object> baseProperties = loadYaml(defaultFile);
            if (baseProperties == null || baseProperties.isEmpty()) {
                throw new ConfigParsingException("Default configuration file not found: " + defaultFile);
            }
            flatToMap("", baseProperties, properties);

            if (profile != null && !profile.trim().isEmpty()) {
                String profileFile = defaultFile.replace(".yaml", "-" + profile + ".yaml");
                log.info("Loading profile from " + profileFile);
                Map<String, Object> profileProperties = loadYaml(profileFile);
                if (profileProperties != null) {
                    flatToMap("", profileProperties, properties);
                    log.info("Properties after loading profile file {}: {}" + profileFile + properties);
                } else log.info("Profile file not found: " + profileFile + ", using default only");
            }
            resolveEnvVariables(properties);
        } catch (Exception e) {
            throw new ConfigParsingException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> loadYaml(String filePath) {
        try (InputStream input = ConfigParser.class.getClassLoader().getResourceAsStream(filePath)) {
            if (input == null) {
                return Collections.emptyMap();
            }
            Yaml yaml = new Yaml();
            return yaml.load(input);
        } catch (Exception e) {
            throw new ConfigParsingException("Error reading YAML file: " + filePath, e);
        }
    }

    private void flatToMap(String prefix, Map<String, Object> source, Map<String, Object> target) {
                source
                .entrySet()
                .stream()
                .forEach(entry -> {
                    String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof Map) {
                        flatToMap(key, (Map<String, Object>) value, target);
                    } else target.put(key, value);
                });
    }

    private void resolveEnvVariables(Map<String, Object> props) {
        props.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String)
                .forEach(entry -> {
                    String value = (String) entry.getValue();
                    Matcher matcher = ENV_PATTERN.matcher(value);
                    if (matcher.matches()) {
                        String envVar = matcher.group(1);
                        String defaultValue = matcher.group(2);
                        String envValue = System.getenv(envVar);
                        props.put(entry.getKey(), envValue != null ? envValue : defaultValue);
                        System.err.println(props);
                    }
                });
    }

    public <T> T bind(Class<T> clasz) {
        try {
            ConfigurationProperties annotation = clasz.getAnnotation(ConfigurationProperties.class);
            String prefix = annotation != null ? annotation.prefix() : "";
            T instance = clasz.getDeclaredConstructor().newInstance();
            bindProperties(instance, prefix);
            return instance;
        } catch (Exception e) {
            throw new ConfigParsingException("Failed to bind properties to " + clasz.getName(), e);
        }
    }

    private void bindProperties(Object instance, String prefix){
        Stream.of(instance.getClass().getDeclaredFields())
                .forEach(field -> {
                    try {
                        field.setAccessible(true);
                        String propKey = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
                        Object value = properties.get(propKey);

                        log.debug("Binding property field  {} to {}", propKey, value);
                        if (whatType(field.getType())) {
                            if (value != null) {
                                field.set(instance, convertValue(value, field.getType()));
                            }
                        } else {
                            Object nestedInstance = field.getType().getDeclaredConstructor().newInstance();
                            bindProperties(nestedInstance, propKey);
                            field.set(instance, nestedInstance);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to bind field " + field.getName(), e);
                    }
                });
    }

    private boolean whatType(Class<?> type) {
        return type.isPrimitive() || type == String.class || type == Integer.class ||
                type == Boolean.class || type == Double.class || type == Long.class;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        String strValue = value.toString();
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(strValue);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(strValue);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(strValue);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(strValue);
        return strValue;
    }
}