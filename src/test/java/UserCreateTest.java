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

public class UserCreateTest {

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
        invalidUser = new User(user.getEmail(), user.getPassword(), null);
    }

    @Test
    @Description("Test creating a unique user")
    public void testCreateUniqueUser() {
        Response response = createUser(user);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", notNullValue());

        accessToken = response.path("accessToken");
    }

    @Test
    @Description("Test creating an existing user. Expected status code: 403")
    public void testCreateExistingUser() {
        createUser(user);

        Response response = createUser(user);
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Test
    @Description("Test creating a user without a required field")
    public void testCreateUserWithoutRequiredField() {
        Response response = createUser(invalidUser);
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Step("Create a user")
    @Description("Send a POST request to create a user with the provided data")
    private Response createUser(User user) {
        return given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
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
