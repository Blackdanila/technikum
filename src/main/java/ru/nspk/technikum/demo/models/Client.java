package ru.nspk.technikum.demo.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    private int id;
    private String name;
    private String surname;
    private String birthday;
    private List<Order> orders;
}
