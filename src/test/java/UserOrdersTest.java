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
import static org.junit.Assert.assertNotNull;

public class UserOrdersTest {

    private User user;
    private String accessToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = Constants.BASE_URL;

        Random random = new Random();
        String email = "test" + random.nextInt(1000000) + "@yandex.ru";
        String password = "password" + random.nextInt(1000000);
        String name = "User" + random.nextInt(1000000);

        user = new User(email, password, name);

        Response registerResponse = given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
        registerResponse.then().statusCode(200);
    }

    @Test
    @Description("Test getting orders for an authorized user")
    public void testGetOrdersForAuthorizedUser() {
        Response loginResponse = loginUser(user);
        loginResponse.then().statusCode(200);

        accessToken = loginResponse.path("accessToken");
        assertNotNull(accessToken);

        Response response = getOrders(accessToken);

        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("orders", notNullValue());
    }

    @Test
    @Description("Test getting orders for an unauthorized user")
    public void testGetOrdersForUnauthorizedUser() {

        Response response = getOrders(null);
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Get orders")
    private Response getOrders(String accessToken) {
        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken != null ? accessToken : "")
                .get("/api/orders");
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