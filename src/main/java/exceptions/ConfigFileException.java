package exceptions;



public class ConfigFileException extends Exception{
    ConfigFileException(){
        super("Error loading config file");
    }
}
