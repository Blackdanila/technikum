package ru.nspk.technikum.demo.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.nspk.technikum.demo.models.Client;
import ru.nspk.technikum.demo.models.Order;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
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

    ObjectMapper objectMapper = new ObjectMapper();
    WireMockServer wireMockServer;
    OkHttpClient okHttpClient = new OkHttpClient();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Запускаем WireMock-сервер на localhost:8080
        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();

        // Конфигурируем wiremock для работы с нашим сервером
        configureFor("localhost", 8080);

        // Делаем дефолтный стаб для Клиента с id = 123
        wireMockServer.stubFor(
                get(urlEqualTo("/client/123"))
                        .atPriority(9999)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(objectMapper.writeValueAsString(buildStubClientWithSpecificNumOrders(6)))
                                .withFixedDelay(1_000)
                        )
        );

        System.out.println("Дефолтный стаб зарегистрирован. Скидка: " + 10);
    }

    @AfterEach
    void tearDown() {
        // Очистка всех стабов
        wireMockServer.resetAll();
        // Остановка сервера wiremock
        wireMockServer.stop();
    }

    @Test
    public void getDiscountTest() throws IOException {
        // Значение предустановленное в моке соответствует скидке 10%
        assertThat(10, equalTo(getDiscount(123)));

        // Переопределяем стаб с более высоким приоритетом
        MappingBuilder newStub = get(urlEqualTo("/client/123"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(buildStubClientWithSpecificNumOrders(1)))
                        .withFixedDelay(1_000)
                );

        stubFor(newStub);

        // Проверяем, что в новом стабе по запросу возвращается скидка = 0%
        int newDiscount = getDiscount(123);
        assertThat(0, equalTo(newDiscount));
        System.out.println("Стаб переопределен. Скидка: " + newDiscount);

        // Удаляем переопределенный стаб
        wireMockServer.removeStub(newStub);

        // Проверяем что скидка снова стала 10%
        int oldDiscount = getDiscount(123);
        assertThat(10, equalTo(oldDiscount));
        System.out.println("Стаб удален. Скидка: " + oldDiscount);
    }

    private int getDiscount(int clientId) throws IOException {
        // Делаем запрос на wiremock
        Request request = new Request.Builder()
                .url("http://localhost:8080/client/" + clientId)
                .get()
                .build();

        Call call = okHttpClient.newCall(request);
        Response response = call.execute();

        // десериализуем ответ в объект Client
        Client responseAsClient = objectMapper.readValue(response.body().string(), Client.class);
        return responseAsClient.getOrders().size() > 5 ? 10 : 0;
    }

    // Создаем тестового клиента с заданным количеством заказов
    private Client buildStubClientWithSpecificNumOrders(int numOrders) {
        Client stubClient = new Client();
        stubClient.setId(123);
        stubClient.setName("Тест");
        stubClient.setSurname("Тестовый");
        stubClient.setBirthday("01-01-1970");
        stubClient.setOrders(buildStubOrders(numOrders));

        return stubClient;
    }

    // Создаем тестовые заказы
    private List<Order> buildStubOrders(int numOrders) {
        List<Order> stubOrders = new ArrayList<>(numOrders);
        for (int i = 1; i <= numOrders; i++) {
            stubOrders.add(new Order(i));
        }

        return stubOrders;
    }
}
