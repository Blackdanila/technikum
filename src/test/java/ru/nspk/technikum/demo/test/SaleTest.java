package ru.nspk.technikum.demo.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import ru.nspk.technikum.demo.models.Client;
import ru.nspk.technikum.demo.models.Sale;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

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

    WireMock wireMock = new WireMock("http", "localhost", 8080);

    ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    OkHttpClient httpClient = new OkHttpClient();

    @BeforeEach
    public void setupWireMock() {
        this.wireMock.register(
                WireMock.get(WireMock.urlPathMatching("/client/([0-9]+)"))
                        .willReturn(ResponseDefinitionBuilder.okForJson(
                                new Client(
                                        "Ivan",
                                        "Ivanov",
                                        LocalDate.of(1970, 6, 1),
                                        List.of(
                                                new Sale(1),
                                                new Sale(2),
                                                new Sale(3),
                                                new Sale(4),
                                                new Sale(5),
                                                new Sale(6)
                                        )
                                )
                        ))
                        .atPriority(1000)
        );
    }

    private int getDiscount(int id) {
        int salesCount = getSalesCount(id); // здесь запрос в мок
        // Больше пяти заказов, скидка 10%
        return salesCount > 5 ? 10 : 0;
    }

    private int getSalesCount(int id) {
        int salesCount = 0;

        Request request = new Request.Builder().
                url("http://localhost:8080/client/"+id)
                .get()
                .build();
        Call call = httpClient.newCall(request);
        try (Response response = call.execute()){
            var body = response.body();
            if (body != null) {
                var client = objectMapper.readValue(response.body().bytes(), Client.class);
                salesCount = client == null ? 0 : client.getSales().size();
            }
        } catch (IOException e) {
            System.out.println("[DEBUG] " + e.getMessage());
            System.out.println("Service unavailable. No discount.");
        }

        return salesCount;
    }

    @Test
    public void getDiscountTest() {
        // Значение предустановленное в моке соответствует скидке 10%
        assertThat(10, equalTo(getDiscount(123)));

        // меняем историю покупок клиента в моке
        var stub = this.wireMock.register(
                WireMock.get(WireMock.urlMatching("/client/([0-9]+)"))
                        .willReturn(ResponseDefinitionBuilder.okForJson(
                                new Client(
                                        "Ivan",
                                        "Ivanov",
                                        LocalDate.of(1970, 6, 1),
                                        List.of(new Sale(1))
                                )
                        ))
                        .atPriority(1)
        );

        assertThat(0, equalTo(getDiscount(123)));

        // меняем историю покупок клиента на исходную
        this.wireMock.removeStubMapping(stub);

        assertThat(10, equalTo(getDiscount(123)));
    }
}
