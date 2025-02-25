package by.example;

public class Main {
    public static void main(String[] args) {

        ConfigParser parser = new ConfigParser("application.yaml");
        AppConfig config = parser.bind(AppConfig.class);
        System.out.println("Default config: " + config);

        ConfigParser parserWithProfile = new ConfigParser("application.yaml", "local");
        AppConfig localConfig = parserWithProfile.bind(AppConfig.class);
        System.out.println("Local config: " + localConfig);

        System.setProperty("DB_URL", "jdbc:env");
        ConfigParser envParser = new ConfigParser("application.yaml");
        AppConfig envConfig = envParser.bind(AppConfig.class);
        System.out.println("With env: " + envConfig);
    }
}
