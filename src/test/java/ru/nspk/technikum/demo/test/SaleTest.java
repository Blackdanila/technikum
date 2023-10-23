package ru.nspk.technikum.demo.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.nspk.technikum.demo.models.ClientData;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.UUID;


/**
 * Домашнее задание
 * Поднять wiremock с дефолтной конфигурацией, где по запросу с низким приоритетом будет отдаваться информация по определенному клиенту.
 * В рамках теста переопределить ответ на тот же запрос, получить новые данные и удалить переопределенный ответ после завершения
 * <p>
 * Шаги
 * Wiremock поднят и работает.
 * В тесте запрашиваем из мока информацию по запросу GET /client/123 и получаем предустановленную информацию (имя, фамилия, день рождения, история заказов).
 * У пользователя изначально более 5 заказов, нашим методом возвращается скидка 10%.
 * Переопределяем в моке кол-во заказов пользователя на 1 и скидка будет отсутствовать. Убеждаемся в этом.
 * Удаляем заглушку с 1 заказом пользователя. У пользователя вернется скидка 10%
 */

public class SaleTest {

    private static final int CLIENT_ID = 123;
    private static final String FIRST_NAME = "Ivan";
    private static final String LAST_NAME = "Pokupkin";
    private static final String DATE_OF_BIRTH = "1999-02-15";

    private WireMockServer wireMockServer;
    private ObjectMapper objectMapper;

    StubMapping stub = null;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        objectMapper = new ObjectMapper();
        configureFor("localhost", wireMockServer.port());

        stub = stubClientDataResponse(CLIENT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, 10, 10);
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void getDiscountTest() throws IOException {
        //Check default response
        assertThat(10, equalTo(getDiscount(123)));

        //Check override response
        try {
            stub = stubClientDataResponse(CLIENT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, 1, 10);
            assertThat(0, equalTo(getDiscount(123)));
        } finally {
            wireMockServer.removeStubMapping(stub);
        }

        try {
            stub = stubClientDataResponse(CLIENT_ID, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, 10, 10);
            assertThat(10, equalTo(getDiscount(123)));
        } finally {
            wireMockServer.removeStubMapping(stub);
        }
    }

    private StubMapping stubClientDataResponse(final int clientId, final String firstName, final String lastName, final String dateOfBirth, final int orderHistory, final int priority) throws JsonProcessingException {

        return wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/client/" + clientId))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(new ClientData(UUID.randomUUID().toString(), firstName, lastName, dateOfBirth, orderHistory))))
                .atPriority(priority));
    }
    private int getDiscount(final int id) throws IOException {
        String responseBody = makeSuccessHttpRequest("/client/" + id);
        ClientData clientData = objectMapper.readValue(responseBody, ClientData.class);

        return clientData.getOrderHistory() > 5 ? 10 : 0;
    }

    private String makeSuccessHttpRequest(final String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://localhost:" + wireMockServer.port() + url).get().build();

        Call call = client.newCall(request);
        Response response = call.execute();

        assertThat(response.code(), Matchers.equalTo(200));

        return response.body().string();
    }

}
