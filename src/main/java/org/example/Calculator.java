package org.example;

public class Calculator {
    public double calculateLimit(double balance, double reserve, int days) {
        return (balance - reserve) / days;
    }
}