package by.example;


import by.example.config.ConfigParser;
import by.example.example.AppConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    private static final String D_CONFIG_FILE ="application.yaml";
    private static final String PROFILE = "local";

    public static void main(String[] args) {
        ConfigParser parser = new ConfigParser(D_CONFIG_FILE);
        AppConfig config = parser.bind(AppConfig.class);
        log.info("Default config: {}", config);

        ConfigParser parserWithProfile = new ConfigParser(D_CONFIG_FILE, PROFILE);
        AppConfig localConfig = parserWithProfile.bind(AppConfig.class);
        log.info("Local config: {}", localConfig);

        System.setProperty("DB_URL", "jdbc:env");
        ConfigParser envParser = new ConfigParser(D_CONFIG_FILE);
        AppConfig envConfig = envParser.bind(AppConfig.class);
        log.info("With env: {}", envConfig);
    }
}
