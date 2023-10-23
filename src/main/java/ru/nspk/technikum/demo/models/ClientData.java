package ru.nspk.technikum.demo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientData {
    private String id;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private int orderHistory;
}
