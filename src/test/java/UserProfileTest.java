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
import static org.junit.Assert.assertNotNull;

public class UserProfileTest {

    private User user, updatedUser, updatedUser2, secondUser;
    private String accessToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = Constants.BASE_URL;

        Random random = new Random();
        String email = "test" + random.nextInt(1000000) + "@yandex.ru";
        String password = "password" + random.nextInt(1000000);
        String name = "User" + random.nextInt(1000000);

        user = new User(email, password, name);
        secondUser = new User("existingemail@yandex.ru", "password123", "SecondUser");
        updatedUser = new User("newemail@yandex.ru", user.getPassword(), "NewName");
        updatedUser2 = new User("test" + random.nextInt(1000000) + "@yandex.ru", user.getPassword(), "NewName");

        Response registerResponse = registerUser(user);
        registerResponse.then().statusCode(200);

        Response loginResponse = loginUser(user);
        loginResponse.then().statusCode(200);
        accessToken = loginResponse.path("accessToken");
        assertNotNull(accessToken);
    }

    @Test
    @Description("Get user profile with authorization")
    public void testGetUserProfileWithAuthorization() {
        Response response = getUserProfile(accessToken);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(user.getEmail()))
                .body("user.name", equalTo(user.getName()));
    }

    @Test
    @Description("Get user profile without authorization")
    public void testGetUserProfileWithoutAuthorization() {
        Response response = getUserProfile(null);
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @Description("Update user profile with authorization")
    public void testUpdateUserProfileWithAuthorization() {
        Response response = updateUserProfile(accessToken, updatedUser2);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(updatedUser2.getEmail()))
                .body("user.name", equalTo(updatedUser2.getName()));
    }


    @Test
    @Description("Update user profile without authorization")
    public void testUpdateUserProfileWithoutAuthorization() {
        Response response = updateUserProfile(null, updatedUser);
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @Description("Update user profile with an existing email")
    public void testUpdateUserProfileWithExistingEmail() {
        registerUser(secondUser);
        Response response = updateUserProfile(accessToken, updatedUser);
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User with such email already exists"));
    }

    @Step("Register user")
    private Response registerUser(User user) {
        return given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
    }

    @Step("Login user")
    private Response loginUser(User user) {
        return given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/login");
    }

    @Step("Get user profile")
    private Response getUserProfile(String accessToken) {
        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken != null ? accessToken : "")
                .get("/api/auth/user");
    }

    @Step("Update user profile")
    private Response updateUserProfile(String accessToken, User updatedUser) {
        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken != null ? accessToken : "")
                .body(updatedUser)
                .patch("/api/auth/user");
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