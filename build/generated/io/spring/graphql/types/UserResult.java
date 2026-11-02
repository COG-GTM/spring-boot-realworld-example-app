package io.spring.graphql.types;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "__typename"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserPayload.class, name = "UserPayload"),
    @JsonSubTypes.Type(value = Error.class, name = "Error")
})
public interface UserResult {
}
