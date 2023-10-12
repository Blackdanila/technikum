package ru.nspk.technikum.demo.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Person {

    private String id;
    private String name;
    private String secondName;
    private String role;
}
