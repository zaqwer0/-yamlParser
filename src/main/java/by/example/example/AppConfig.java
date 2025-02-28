package by.example.example;

import by.example.config.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String name;
    private int timeout;
    private DatabaseConfig database = new DatabaseConfig();
}


