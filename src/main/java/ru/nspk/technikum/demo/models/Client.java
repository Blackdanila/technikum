package ru.nspk.technikum.demo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    private String name;
    private String secondName;
    private LocalDate birthDay;
    private List<Sale> sales;
}
