package com.khundadze.model;

public record RequestDto(
                Command command,
                String name,
                Object value,
                Double score) {

}
