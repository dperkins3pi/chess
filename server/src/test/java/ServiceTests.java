import chess.ChessGame;
import dataAccess.*;
import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;
import passoffTests.obfuscatedTestClasses.TestServerFacade;
import passoffTests.testClasses.TestException;
import passoffTests.testClasses.TestModels;
import response.ResponseClass;
import server.Server;
import service.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.net.HttpURLConnection;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceTests{

    private static AuthDAO authDAO;
    private static GameDAO gameDAO;
    private static UserDAO userDAO;

    @BeforeEach
    public void setup() throws TestException {
        authDAO = new MemoryAuthDAO();
        gameDAO = new MemoryGameDAO();
        userDAO = new MemoryUserDAO();
    }
    @AfterEach
    public void clearAll() throws DataAccessException {
        ClearService service = new ClearService(authDAO, gameDAO, userDAO);
        service.clear();
    }

    @Test
    public void clearService() throws Exception {
        // Arbitrarily add stuff to each DAO
        authDAO.createAuth("Frank");
        authDAO.createAuth("John");
        authDAO.createAuth("Cena");
        userDAO.createUser("Username", "Password", "Email");
        userDAO.createUser("Username2", "Password2", "Email2");
        gameDAO.createGame("Game1");
        gameDAO.createGame("Game2");

        // Call remove service
        ClearService service = new ClearService(authDAO, gameDAO, userDAO);
        service.clear();

        // See if things were removed
        Assertions.assertEquals(authDAO.getAuthTokens(), new HashMap<>());
        Assertions.assertEquals(gameDAO.getGames(), new HashMap<>());
        Assertions.assertEquals(userDAO.getUsers(), new HashMap<>());
    }

    @Test
    public void registerServicePositive() throws BadRequestException, DataAccessException, AlreadyTakenException {
        // Call register service and store authtoken
        RegisterService service = new RegisterService(authDAO, gameDAO, userDAO);
        ResponseClass response = service.register("username", "password", "email");
        ResponseClass response2 = service.register("username2", "password2", "email2");
        String authToken = response.getAuthToken();
        String authToken2 = response2.getAuthToken();

        // Create UserData objects
        UserData theUserData = new UserData("username", "password", "email");
        UserData theUserData2 = new UserData("username2", "password2", "email2");

        // See if users are correctly registered
        Assertions.assertEquals(userDAO.getUsers().get("username"), theUserData);
        Assertions.assertEquals(userDAO.getUsers().get("username2"), theUserData2);

        // Create authdata objects that should be stored
        AuthData theAuthData = new AuthData(authToken, "username");
        AuthData theAuthData2 = new AuthData(authToken2, "username2");

        // See if users are correctly logged in
        Assertions.assertEquals(authDAO.getAuthTokens().get(authToken), theAuthData);
        Assertions.assertEquals(authDAO.getAuthTokens().get(authToken2), theAuthData2);
    }

    @Test
    public void registerServiceNegative() throws BadRequestException, DataAccessException, AlreadyTakenException {
        // Call register service that should work
        RegisterService service = new RegisterService(authDAO, gameDAO, userDAO);
        ResponseClass response = service.register("username", "password", "email");

        // See if an error comes from registering the same username twice
        Assertions.assertThrows(AlreadyTakenException.class, () -> service.register("username", "password2", "email2"));
    }

    @Test
    public void loginServicePositive() throws BadRequestException, DataAccessException, AlreadyTakenException, UnauthorizedException {
        // Register new users
        RegisterService registerer = new RegisterService(authDAO, gameDAO, userDAO);
        registerer.register("username", "password", "email");
        registerer.register("username2", "password2", "email2");

        // Log in
        LoginService service = new LoginService(authDAO, gameDAO, userDAO);
        ResponseClass response = service.login("username", "password");
        ResponseClass response2 = service.login("username2", "password2");

        // Get authtokens from responses and create authrokens from them
        String authToken = response.getAuthToken();
        String authToken2 = response2.getAuthToken();
        AuthData theAuthData = new AuthData(authToken, "username");
        AuthData theAuthData2 = new AuthData(authToken2, "username2");

        // See if users are logged in with there authtokens
        Assertions.assertEquals(authDAO.getAuthTokens().get(authToken), theAuthData);
        Assertions.assertEquals(authDAO.getAuthTokens().get(authToken2), theAuthData2);
    }

    @Test
    public void logininServiceNegative() throws BadRequestException, DataAccessException, AlreadyTakenException {
        // Logging in without valid username
        LoginService service = new LoginService(authDAO, gameDAO, userDAO);
        Assertions.assertThrows(UnauthorizedException.class, () -> service.login("username", "password"));

        // Register new user and try logging in with the wrong password
        RegisterService registerer = new RegisterService(authDAO, gameDAO, userDAO);
        registerer.register("username", "password", "email");
        Assertions.assertThrows(UnauthorizedException.class, () -> service.login("username", "password2"));

    }

    @Test
    public void logoutServicePositive() throws BadRequestException, DataAccessException, AlreadyTakenException, UnauthorizedException {
        // Register a new user
        RegisterService registerer = new RegisterService(authDAO, gameDAO, userDAO);
        ResponseClass response = registerer.register("username", "password", "email");

        // Get authtoken
        String authToken = response.getAuthToken();

        // Logout
        LogoutService service = new LogoutService(authDAO, gameDAO, userDAO);
        service.logout(authToken);

        // See if users is no longer logged in
        Assertions.assertNull(authDAO.getAuthTokens().get(authToken));
    }

    @Test
    public void logoutServiceNegative(){
        // Logout with invalid authToken (since no one is logged in)
        LogoutService service = new LogoutService(authDAO, gameDAO, userDAO);
        Assertions.assertThrows(UnauthorizedException.class, () -> service.logout("cheese"));
    }

    @Test
    public void listGameServicePositive() throws BadRequestException, DataAccessException, AlreadyTakenException, UnauthorizedException {
        // Register a new user
        RegisterService registerer = new RegisterService(authDAO, gameDAO, userDAO);
        ResponseClass response = registerer.register("username", "password", "email");
        String authToken = response.getAuthToken();           // Get authtoken

        // Call the service
        ListGamesService service = new ListGamesService(authDAO, gameDAO, userDAO);
        Collection<GameData> games = service.listGames(authToken);

        // See if it returns an empty collection of games
        Assertions.assertArrayEquals(gameDAO.listGames().toArray(), games.toArray());

        // Add two games
        CreateGameService addgame = new CreateGameService(authDAO, gameDAO, userDAO);
        addgame.createGame(authToken, "Game1");
        addgame.createGame(authToken, "Game2");
        games = service.listGames(authToken);

        // Verify if the game was added
        Assertions.assertArrayEquals(gameDAO.listGames().toArray(), games.toArray());
    }
    @Test
    public void listGameServiceNegative(){
        // Logout with invalid authToken (since no one is logged in)
        ListGamesService service = new ListGamesService(authDAO, gameDAO, userDAO);
        Assertions.assertThrows(UnauthorizedException.class, () -> service.listGames("cheese"));
    }

    @Test
    public void createGameServicePositive() throws BadRequestException, DataAccessException, AlreadyTakenException, UnauthorizedException {
        // Register a new user
        RegisterService registerer = new RegisterService(authDAO, gameDAO, userDAO);
        ResponseClass response = registerer.register("username", "password", "email");
        String authToken = response.getAuthToken();           // Get authtoken

        // Call Service
        CreateGameService service = new CreateGameService(authDAO, gameDAO, userDAO);
        Integer id = service.createGame(authToken, "Game1");

        // Verify if the game was added
        ChessGame chessGame = new ChessGame();
        GameData game = new GameData(id, "", "", "Game1", chessGame);
        Assertions.assertEquals(gameDAO.getGame(id), game);

        // Call Service again
        Integer id2 = service.createGame(authToken, "Game2");

        // Verify if the game was added
        ChessGame chessGame2 = new ChessGame();
        GameData game2 = new GameData(id2, "", "", "Game2", chessGame2);
        Assertions.assertEquals(gameDAO.getGame(id2), game2);
    }
    @Test
    public void createGameServiceNegative() throws BadRequestException, DataAccessException, AlreadyTakenException {
        // listGameService with invalid authToken (since no one is logged in)
        CreateGameService service = new CreateGameService(authDAO, gameDAO, userDAO);
        Assertions.assertThrows(UnauthorizedException.class, () -> service.createGame("cheese", "Game1"));

        // Register a new user
        RegisterService registerer = new RegisterService(authDAO, gameDAO, userDAO);
        ResponseClass response = registerer.register("username", "password", "email");
        String authToken = response.getAuthToken();           // Get authtoken
        // See if an error results for not putting in gameName
        Assertions.assertThrows(BadRequestException.class, () -> service.createGame(authToken, null));
    }
}
