package request;

import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;

public class RegisterRequest {
    String username;
    String password;
    String email;

    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername(){
        return username;
    }
    public String getPassword(){
        return password;
    }
    public String getEmail(){
        return email;
    }
}
