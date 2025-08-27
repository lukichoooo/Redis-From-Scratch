package model;

public record RequestDto(
        Command command,
        Integer key,
        Object value) {

}
