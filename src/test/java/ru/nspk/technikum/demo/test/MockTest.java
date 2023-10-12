package ru.nspk.technikum.demo.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import lombok.SneakyThrows;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import ru.nspk.technikum.demo.models.Person;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@com.github.tomakehurst.wiremock.junit5.WireMockTest
public class MockTest {

    // Заглушка
    WireMock wireMock = new WireMock("http", "localhost", 8080);
    StubMapping stub = null;

    // Парсер
    ObjectMapper objectMapper = new ObjectMapper();

    // Клиент
    OkHttpClient client = new OkHttpClient();

    @Test
    void testSomethingWithMock() throws JsonProcessingException {

        try {
            stub = wireMock.register(
                    WireMock.get(
                                    WireMock.urlMatching("/nspk/technikum/students"))
                            .willReturn(ResponseDefinitionBuilder.okForJson(objectMapper.writeValueAsString(getStudents())))
                            .atPriority(1)
            );

            System.out.println(" 1. " + wireMock.allStubMappings().getMappings());
        } finally {
            wireMock.removeStubMapping(stub);
        }

        System.out.println("2. " + wireMock.allStubMappings().getMappings());

    }

    /**
     * Работа с шаблонами
     */
    @Test
    public void wiremockTemplatesVariablesTest() throws IOException {
        // Подготавливаем мок для приема запроса на добавление студента
        stub = wireMock.register(
                WireMock.post(
                                WireMock.urlMatching("/nspk/technikum/student/add"))
                        .willReturn(aResponse()
                                .withBody("{{jsonPath request.body '$.secondName'}} added successfully")
                                .withTransformers("response-template")));

        // Добавляем студента
        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(new Person(
                        UUID.randomUUID().toString(),
                        "Elena",
                        "Orlova",
                        "Student")),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("http://localhost:8080/nspk/technikum/student/add")
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();


        // Проверяем, что ответ соответствует заданному шаблону
        assertThat(response.code(), equalTo(200));
        assertThat(response.body().string(), equalTo("Orlova added successfully"));
    }

    @Test
    public void wiremockFunctionTest() throws IOException {
        stub = wireMock.register(
                WireMock.get(WireMock.urlMatching("/nspk/technikum/diceroller"))
                        .willReturn(aResponse()
                                .withBody("{{pickRandom '1' '2' '3' '4' '5' '6'}}")
                                .withTransformers("response-template")));

        Request request = new Request.Builder().
                url("http://localhost:8080/nspk/technikum/diceroller")
                .get()
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        String s = response.body().string();
        System.out.println(s);

        assertThat(Integer.valueOf(s), greaterThan(0));
        assertThat(Integer.valueOf(s), lessThanOrEqualTo(6));
    }

    @Test
    public void wiremockProxyTest() throws IOException {
        stub = wireMock.register(
                WireMock.get(WireMock.anyUrl())
                        .willReturn(aResponse().proxiedFrom("https://google.com")));

        Request request = new Request.Builder().
                url("http://localhost:8080/nspk?query=wrong")
                .get()
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        System.out.println(response.body().string());
    }

    @Test
    public void wiremockDelayTest() throws IOException {
        stub = wireMock.register(
                WireMock.get(WireMock.urlPathMatching("/nspk.*"))
                        .willReturn(aResponse()
                                .withBody(objectMapper.writeValueAsString(getStudents()))
                                .withFixedDelay(1000))
//                                .withFault(Fault.EMPTY_RESPONSE)
                        .atPriority(1));

        Request request = new Request.Builder().
                url("http://localhost:8080/nspk?query=wrong")
                .get()
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        long tx = response.sentRequestAtMillis();
        long rx = response.receivedResponseAtMillis();
        System.out.println("response time : "+(rx - tx)+" ms");
        assertThat(rx - tx, greaterThan(1000L));
    }

    private List<Person> getStudents() {

        Person p1 = new Person(UUID.randomUUID().toString(), "Noah", "Anderson", "student");
        Person p2 = new Person(UUID.randomUUID().toString(), "Liam", "Thomas", "student");
        Person p3 = new Person(UUID.randomUUID().toString(), "Mason", "Jackson", "student");
        Person p4 = new Person(UUID.randomUUID().toString(), "Jacob", "White", "student");
        Person t1 = new Person(UUID.randomUUID().toString(), "Martin", "Fowler", "teacher");

        return List.of(p1, p2, p3, p4, t1);
    }
}
