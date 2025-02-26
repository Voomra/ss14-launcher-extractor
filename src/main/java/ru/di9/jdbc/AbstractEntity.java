package ru.di9.jdbc;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbstractEntity<T> {

    protected T id;
}
