package by.example;

import lombok.Data;

@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String name;
    private int timeout;
    private DatabaseConfig database;
}


