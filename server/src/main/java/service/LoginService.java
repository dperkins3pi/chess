package service;

import dataAccess.*;
import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import model.UserData;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import response.ResponseClass;

public class LoginService {
    AuthDAO authDAO;
    GameDAO gameDAO;
    UserDAO userDAO;

    public LoginService(AuthDAO authDAO, GameDAO gameDAO, UserDAO userDAO) {
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.userDAO = userDAO;
    }

    public ResponseClass login(String username, String password) throws DataAccessException, UnauthorizedException, AlreadyTakenException, BadRequestException {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();  //Hash the password
        UserData user = userDAO.getUser(username);
        if (user != null){  // If the user already exists
            if (encoder.matches(password, user.password())) {  // If the password is correct
                String authToken = authDAO.createAuth(username);
                return new ResponseClass(username, authToken);    // Return response object
            }
        }
        throw new UnauthorizedException("Error: unauthorized");  // If it didn't work, throw exception
    }
}
