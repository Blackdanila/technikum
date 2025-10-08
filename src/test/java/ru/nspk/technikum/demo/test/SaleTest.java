package ru.nspk.technikum.demo.test;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Домашнее задание
 * Поднять wiremock с дефолтной конфигурацией, где по запросу с низким приоритетом будет отдаваться информация по определенному клиенту.
 * В рамках теста переопределить ответ на тот же запрос, получить новые данные и удалить переопределенный ответ после завершения
 *
 * Шаги
 * Wiremock поднят и работает.
 * В тесте запрашиваем из мока информацию по запросу GET /client/123 и получаем предустановленную информацию (имя, фамилия, день рождения, история заказов).
 * У пользователя изначально более 5 заказов, нашим методом возвращается скидка 10%.
 * Переопределяем в моке кол-во заказов пользователя на 1 и скидка будет отсутствовать. Убеждаемся в этом.
 * Удаляем заглушку с 1 заказом пользователя. У пользователя вернется скидка 10%
 */
public class SaleTest {

    private int getDiscount(int id) {

        int salesCount = 0; // здесь запрос в мок
        // Больше пяти заказов, скидка 10%
        return salesCount > 5 ? 10 : 0;
    }

    public void getDiscountTest() {
        // Значение предустановленное в моке соответствует скидке 10%
        assertThat(10, equalTo(getDiscount(123)));
        // меняем историю покупок клиента в моке
        assertThat(0, equalTo(getDiscount(123)));
        // меняем историю покупок клиента на исходную
        assertThat(10, equalTo(getDiscount(123)));
    }

    static WireMockServer wm;
    static OkHttpClient client = new OkHttpClient();
    static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupServer() {
        wm = new WireMockServer(); // конфигурация по умолчанию (порт 8080 по умолчанию)
        wm.start();
        configureFor("localhost", wm.port());

        // заглушка по умолчанию для GET /client/123 с заказами > 5
        Map<String, Object> clientInfo = new HashMap<>();
        clientInfo.put("firstName", "Иван");
        clientInfo.put("lastName", "Иванов");
        clientInfo.put("birthday", LocalDate.of(1990, 1, 1).toString());
        clientInfo.put("orders", List.of(101, 102, 103, 104, 105, 106)); // 6 заказов

        try {
            String body = mapper.writeValueAsString(clientInfo);
            wm.stubFor(get(urlEqualTo("/client/123"))
                    .atPriority(10) // большее число = меньший приоритет
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(body)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void teardownServer() {
        if (wm != null) wm.stop();
    }

    // GET /client/123 и парсинг JSON
    Map<String, Object> fetchClient() throws IOException {
        String url = "http://localhost:" + wm.port() + "/client/123";
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = client.newCall(req).execute()) {
            assertEquals(200, resp.code());
            String json = resp.body().string();
            return mapper.readValue(json, Map.class);
        }
    }

    // вычисление скидки на основе количества заказов
    int computeDiscount(Map<String, Object> client) {
        Object ordersObj = client.get("orders");
        int ordersCount = 0;
        if (ordersObj instanceof List) {
            ordersCount = ((List<?>) ordersObj).size();
        }
        return ordersCount > 5 ? 10 : 0;
    }

    @Test
    void testDefaultStubGivesDiscount() throws Exception {
        Map<String, Object> client = fetchClient();
        assertNotNull(client);
        assertEquals("Иван", client.get("firstName"));
        assertEquals(10, computeDiscount(client));
    }

    @Test
    void testOverrideStubTemporarilyAndRemove() throws Exception {
        // Create an overriding stub with higher priority (lower number)
        Map<String, Object> overridden = new HashMap<>();
        overridden.put("firstName", "Иван");
        overridden.put("lastName", "Иванов");
        overridden.put("birthday", LocalDate.of(1990, 1, 1).toString());
        overridden.put("orders", List.of(201)); // only 1 order

        String overriddenBody = mapper.writeValueAsString(overridden);

        // заглушка с более высоким приоритетом для переопределения значения по умолчанию
        var mappingBuilder = get(urlEqualTo("/client/123"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(overriddenBody));

        var created = wm.stubFor(mappingBuilder);

        // Чекаем, переопределение активно
        Map<String, Object> clientAfterOverride = fetchClient();
        assertEquals(0, computeDiscount(clientAfterOverride));
        assertEquals(1, ((List<?>) clientAfterOverride.get("orders")).size());

        // Удаляем переопределяющую заглушку
        wm.removeStub(created);

        // Чекаем заглушку по умолчанию еще раз
        Map<String, Object> clientAfterRemoval = fetchClient();
        assertEquals(10, computeDiscount(clientAfterRemoval));
        assertTrue(((List<?>) clientAfterRemoval.get("orders")).size() > 5);
    }
}
