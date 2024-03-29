package handler;

import com.google.gson.Gson;
import dataAccess.*;
import exceptions.*;
import request.RegisterRequest;
import response.ResponseClass;
import service.RegisterService;
import spark.Request;
import spark.Response;

public class RegisterHandler {
    AuthDAO authDAO;
    GameDAO gameDAO;
    UserDAO userDAO;
    public RegisterHandler(AuthDAO authDAO, GameDAO gameDAO, UserDAO userDAO){
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.userDAO = userDAO;
    }
    public Object handle(Request request, Response response) throws DataAccessException {
        // Get data from request

        RegisterRequest registerRequest = new Gson().fromJson(request.body(), RegisterRequest.class);
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String email = registerRequest.getEmail();

        // Call service
        RegisterService registerService = new RegisterService(authDAO, gameDAO, userDAO);
        ResponseClass res = null;
        try {  // Catch all errors
            res = registerService.register(username, password, email);
            String authToken = res.getAuthToken();
            response.status(200);  // It worked!!!!
            return new Gson().toJson(res);
        } catch (BadRequestException e) {
            response.status(400);
            res = new ResponseClass(e.getMessage());
            return new Gson().toJson(res);
        } catch (AlreadyTakenException e) {
            response.status(403);
            res = new ResponseClass(e.getMessage());
            return new Gson().toJson(res);
        } catch (Exception e){
            response.status(500);
            res = new ResponseClass(e.getMessage());
            return new Gson().toJson(res);
        }
    }
}
