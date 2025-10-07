package ru.nspk.technikum.demo.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.nspk.technikum.demo.models.Client;
import ru.nspk.technikum.demo.models.Order;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
    OkHttpClient httpClient = new OkHttpClient();
    ObjectMapper mapper = new ObjectMapper();
    WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8080);


        Client client1 = new Client(123, "Ivan", "Ivanov", "10-10-1990", List.of(
                new Order(1), new Order(2), new Order(3),
                new Order(4), new Order(5), new Order(6)
        ));

        try {
            wireMockServer.stubFor(get(urlEqualTo("/client/123"))
                    .atPriority(9999)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(mapper.writeValueAsString(client1))
                            .withFixedDelay(1000))

            );
            System.out.println("дефолтный стаб создан");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void getDiscountTest() throws JsonProcessingException {
        int initDiscount = getDiscount(123);
        //  Шаг 1 Проверяем, что дефолтный стаб (6 заказов) дает скидку 10%
        assertThat("Скидка с 6 заказами должна быть 10%", initDiscount, equalTo(10));
        // Шаг 2: Переопределяем стаб с более высоким приоритетом (1 заказ)
        Client client2 = new Client();
        client2.setId(123);
        client2.setName("Arya");
        client2.setSurname("Stark");
        client2.setBirthday("30-11-1994");
        client2.setOrders(List.of(new Order(1)));
        StubMapping overrideStub = wireMockServer.stubFor(get(urlEqualTo("/client/123"))
                .atPriority(1) // Высокий приоритет "побеждает"
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(client2))));

        int overrideDiscount = getDiscount(123);
        assertThat("Скидка с 1 заказом должна быть 0%", overrideDiscount, equalTo(0));
        // Шаг 3: Удаляем переопределенный стаб
        wireMockServer.removeStub(overrideStub);
        int restoredDiscount = getDiscount(123);
        System.out.println("Стаб удален. Скидка: " + restoredDiscount);
        assertThat("После удаления переопределения скидка должна быть 10%", restoredDiscount, equalTo(10));
    }


    private int getDiscount(int id) {
        String url = "http://localhost:8080/client/" + id;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return 0;
            }
            String body = response.body().string();
            System.out.println("Получен ответ " + body);
            Client client = mapper.readValue(body, Client.class);
            int salesCount = (client.getOrders() != null) ? client.getOrders().size() : 0;
            return salesCount >= 5 ? 10 : 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        // Остановить WireMock-сервер
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetAll();
            wireMockServer.stop();
            System.out.println("WireMock сервер остановлен.");
        }
    }
}
