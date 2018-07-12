package example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.json.JavalinJson;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Lives to test Javalin 2.0.0-RC1 and Gson 2.8.5 inter-working.
 */
@SuppressWarnings("WeakerAccess")
public class JavalinGsonExampleTests {
  private final Javalin javalin = Javalin.create();

  @BeforeAll
  private static void configureTestClient() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @SuppressWarnings("NullableProblems")
  @BeforeEach
  private void setupJavalinServer() {
    Gson gson = new GsonBuilder().create();
    JavalinJson.setFromJsonMapper(gson::fromJson);
    JavalinJson.setToJsonMapper(gson::toJson);

    javalin.disableStartupBanner().port(8888)
        .get("", context -> { throw new Exception("someMessage"); })
        .exception(Exception.class, (exception, context) -> {
          HashMap<String, String> serializedException = new HashMap<String, String>() {{
            put("message", exception.getMessage());
          }};

          context.json(serializedException);
          context.contentType("application/x.my.custom.error+json");
          context.status(500);
        })
        .start();
  }

  /**
   * Validates that when making a simple GET request to a route that is guaranteed to throw an
   * error that the configured exception handler is able to successfully serialize an error
   * response.
   */
  @Test
  public void rootGetRequestWithExceptionReturnsErrorDocument() {
    given().get("http://localhost:8888").then()
        .statusCode(500)
        .contentType("application/x.my.custom.error+json")
        .body("message", equalTo("someMessage"));
  }

  @AfterEach
  private void stopTestIntegrationEnvironmentAfterEachTest() {
    javalin.stop();
  }
}
