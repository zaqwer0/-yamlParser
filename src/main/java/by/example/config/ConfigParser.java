package by.example.config;

import by.example.exception.ConfigParsingException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConfigParser {
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]+))?\\}");
    private final Map<String, Object> properties = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ConfigParser.class.getName());

    public ConfigParser(String defaultFile) {
        loadProperties(defaultFile, "");
    }

    public ConfigParser(String defaultFile, String profile) {
        loadProperties(defaultFile, profile);
    }

    private void loadProperties(String defaultFile, String profile) {
        try {
            logger.info("Loading properties from: " + defaultFile);
            Map<String, Object> baseProperties = loadYaml(defaultFile);
            if (baseProperties == null || baseProperties.isEmpty()) {
                throw new ConfigParsingException("Default configuration file not found: " + defaultFile);
            }
            flattenMap("", baseProperties, properties);

            if (profile != null && !profile.trim().isEmpty()) {
                String profileFile = defaultFile.replace(".yaml", "-" + profile + ".yaml");
                logger.info("Loading profile from " + profileFile);
                Map<String, Object> profileProperties = loadYaml(profileFile);
                if (profileProperties != null) {
                    flattenMap("", profileProperties, properties);
                    logger.info("Properties after loading profile file {}: {}" + profileFile + properties);
                } else logger.info("Profile file not found: " + profileFile + ", using default only");
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

    private void flattenMap(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, target);
            }
//            else if (value instanceof List) {
//                List<?> list = (List<?>) value;
//                for (int i = 0; i < list.size(); i++) {
//                    Object item = list.get(i);
//                    String newKey = key + "[" + i + "]";
//                    if (item instanceof Map) {
//                        flattenMap(newKey, (Map<String, Object>) item, target);
//                    } else target.put(newKey, item);
//                }
//            }
            else target.put(key, value);

        }
    }

    private void resolveEnvVariables(Map<String, Object> props) {
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                Matcher matcher = ENV_PATTERN.matcher(value);
                if (matcher.matches()) {
                    String envVar = matcher.group(1);
                    String defaultValue = matcher.group(2);
                    String envValue = System.getProperty(envVar, System.getenv(envVar));
                    props.put(entry.getKey(), envValue != null ? envValue : defaultValue);
                }
            }
        }
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

    private void bindProperties(Object instance, String prefix) throws Exception {
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String propKey = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
            Object value = properties.get(propKey);

            if (value != null) {
                if (isSimpleType(field.getType())) {
                    field.set(instance, convertValue(value, field.getType()));
                } else {
                    Object nestedInstance = field.getType().getDeclaredConstructor().newInstance();
                    bindProperties(nestedInstance, propKey);
                    field.set(instance, nestedInstance);
                }
            }
        }
    }

    private boolean isSimpleType(Class<?> type) {
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