package ru.nspk.technikum.demo.models;

import lombok.Getter;

/**
 * Модель заказа.
 *
 * Имеет уникальный идентификатор (id).
 */
@Getter
public class Order {

    private int id;

    /**
     * Конструктор заказа с заданным идентификатором.
     * @param id уникальный идентификатор заказа
     */
    public Order(int id) {
        this.id = id;
    }

    /**
     * Пустой конструктор (необходим для сериализации/десериализации).
     */
    public Order() { }
}

