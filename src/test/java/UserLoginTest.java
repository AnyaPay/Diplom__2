import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UserLoginTest {

    private User user;
    private String accessToken;
    private User invalidUser;

    @Before
    public void setUp() {
        RestAssured.baseURI = Constants.BASE_URL;

        Random random = new Random();
        String email = "test" + random.nextInt(1000000) + "@yandex.ru";
        String password = "password" + random.nextInt(1000000);
        String name = "User" + random.nextInt(1000000);

        user = new User(email, password, name);
        given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");

        invalidUser = new User("invalid@yandex.ru", "wrongpassword", "Invalid User");
    }

    @Test
    @Description("Test successful login with valid credentials")
    public void testSuccessfulLogin() {
        Response response = loginUser(user);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());

        accessToken = response.path("accessToken");
    }

    @Test
    @Description("Test login with invalid credentials")
    public void testLoginWithInvalidCredentials() {
        Response response = loginUser(invalidUser);
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    @Step("Login user")
    private Response loginUser(User user) {
        return given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/login");
    }

    @After
    public void tearDown() {
        if (accessToken != null) {
            given()
                    .header("Authorization", accessToken)
                    .delete("/api/auth/user");
        }
    }
}