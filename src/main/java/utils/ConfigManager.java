package utils;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private final Properties properties;
    private static ConfigManager instance;

    private ConfigManager(){
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")){
            properties.load(input);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static ConfigManager getInstance(){
        if(instance == null){
            instance = new ConfigManager();
        }
        return instance;
    }

    public String getProperty(String key){
        return properties.getProperty(key);
    }

}
