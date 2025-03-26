package com.aresus.cliper.model.token;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TokenValidate {
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("expires_in")
    private int expiresIn;
    @JsonProperty("login")
    private String login;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("scopes")
    private List<String> scopes;

}

