package by.example;


import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigParser {
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]+))?\\}");
    private Map<String, Object> properties = new HashMap<>();
    private String activeProfile = null;

    public ConfigParser(String defaultFile) {
        loadProperties(defaultFile, null);
    }

    public ConfigParser(String defaultFile, String profile) {
        this.activeProfile = profile;
        loadProperties(defaultFile, profile);
    }

    private void loadProperties(String defaultFile, String profile) {
        try {
            Map<String, Object> baseProps = loadYaml(defaultFile);
            if (baseProps != null) {
                flattenMap("", baseProps, properties);
            }


            if (profile != null && !profile.isEmpty()) {
                String profileFile = defaultFile.replace(".yaml", "-" + profile + ".yaml");
                Map<String, Object> profileProps = loadYaml(profileFile);
                if (profileProps != null) {
                    flattenMap("", profileProps, properties);
                }
            }


            resolveEnvVariables(properties);
        } catch (Exception e) {
            throw new ConfigParsingException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> loadYaml(String filePath) {
        if (!Files.exists(Paths.get(filePath))) {
            return null;
        }
        try (InputStream input = new FileInputStream(filePath)) {
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
            } else {
                target.put(key, value);
            }
        }
    }


    private void resolveEnvVariables(Map<String, Object> props) {
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                Matcher matcher = ENV_PATTERN.matcher(value);
                if (matcher.matches()) {
                    String envVar = matcher.group(1);
                    String defaultValue = matcher.group(2) != null ? matcher.group(2) : null;
                    String envValue = System.getenv(envVar);
                    props.put(entry.getKey(), envValue != null ? envValue : defaultValue);
                }
            }
        }
    }

    public <T> T bind(Class<T> clazz) {
        try {
            ConfigurationProperties annotation = clazz.getAnnotation(ConfigurationProperties.class);
            String prefix = annotation != null ? annotation.prefix() : "";
            T instance = clazz.getDeclaredConstructor().newInstance();
            bindProperties(instance, prefix);
            return instance;
        } catch (Exception e) {
            throw new ConfigParsingException("Failed to bind properties to " + clazz.getName(), e);
        }
    }

    private void bindProperties(Object instance, String prefix) throws Exception {
        for (java.lang.reflect.Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String propKey = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
            Object value = properties.get(propKey);

            if (value != null) {
                if (field.getType().isPrimitive() || field.getType() == String.class) {
                    field.set(instance, convertValue(value, field.getType()));
                } else {
                    Object nestedInstance = field.getType().getDeclaredConstructor().newInstance();
                    bindProperties(nestedInstance, propKey);
                    field.set(instance, nestedInstance);
                }
            }
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        String strValue = value.toString();
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(strValue);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(strValue);
        return strValue;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}


