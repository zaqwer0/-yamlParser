package by.example;


import by.example.example.AppConfig;
import by.example.config.ConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {

        ConfigParser parser = new ConfigParser("application.yaml");
        AppConfig config = parser.bind(AppConfig.class);
        logger.info("Default config: {}", config);

        ConfigParser parserWithProfile = new ConfigParser("application.yaml", "local");
        AppConfig localConfig = parserWithProfile.bind(AppConfig.class);
        logger.info("Local config: {}", localConfig);

        System.setProperty("DB_URL", "jdbc:env");
        ConfigParser envParser = new ConfigParser("application.yaml");
        AppConfig envConfig = envParser.bind(AppConfig.class);
        logger.info("With env: {}" ,envConfig);
    }
}
