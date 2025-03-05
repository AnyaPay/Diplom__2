import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class OrderCreateTest {

    private User user;
    private String accessToken;
    private List<String> ingredients;
    private List<String> invalidIngredients;
    private String requestBody;

    @Before
    public void setUp() {
        RestAssured.baseURI = Constants.BASE_URL;

        Random random = new Random();
        String email = "test" + random.nextInt(1000000) + "@yandex.ru";
        String password = "password" + random.nextInt(1000000);
        String name = "User" + random.nextInt(1000000);

        user = new User(email, password, name);
        Response loginResponse = loginUser(user);
        accessToken = loginResponse.path("accessToken");

        ingredients = Arrays.asList("\"61c0c5a71d1f82001bdaaa6d\"", "\"61c0c5a71d1f82001bdaaa71\"");
        invalidIngredients = Arrays.asList("\"invalid_hash_1\"", "\"invalid_hash_2\"");
        requestBody = "{\"ingredients\": " + ingredients + "}";
    }

    @Test
    @Description("Test creating an order with authorization and ingredients")
    public void testCreateOrderWithAuthorizationAndIngredients() {
        Response response = createOrder(ingredients, accessToken);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("order.number", notNullValue());
    }

    @Test
    @Description("Test creating an order without authorization")
    public void testCreateOrderWithoutAuthorization() {
        Response response = createOrder(ingredients, null);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("order.number", notNullValue());
    }

    @Test
    @Description("Test creating an order with authorization but without ingredients")
    public void testCreateOrderWithAuthorizationButWithoutIngredients() {
        Response response = createOrder(Arrays.asList(), accessToken);
        response.then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @Description("Test creating an order with invalid ingredient hash")
    public void testCreateOrderWithInvalidIngredientHash() {
        Response response = createOrder(invalidIngredients, accessToken);
        response.then()
                .statusCode(500);
    }

    @Test
    @Description("Test creating an order with authorization and valid ingredients")
    public void testCreateOrderWithAuthorizationAndValidIngredients() {
        Response response = createOrder(ingredients, accessToken);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("order.number", notNullValue());
    }

    @Step("Create order")
    private Response createOrder(List<String> ingredients, String accessToken) {

        String requestBody = "{\"ingredients\": " + ingredients + "}";

        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken != null ? accessToken : "")
                .body(requestBody)
                .post("/api/orders");
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
            System.out.println("Deleted user: " + user.getEmail());
        }
    }
}