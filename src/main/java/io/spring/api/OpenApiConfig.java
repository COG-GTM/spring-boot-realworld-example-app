package io.spring.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "RealWorld Example App API",
            version = "1.0.0",
            description =
                "Backend REST API for the RealWorld (Conduit) social blogging platform, "
                    + "implementing the RealWorld API spec. Protected endpoints require a JWT "
                    + "obtained from /users or /users/login, sent in the Authorization header.",
            license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT"),
            contact =
                @Contact(name = "RealWorld", url = "https://github.com/gothinkster/realworld")),
    servers = @Server(url = "/", description = "Default server"))
@SecurityScheme(
    name = OpenApiConfig.SECURITY_SCHEME_NAME,
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "Authorization",
    description =
        "JWT authentication. Provide the value as \"Token <jwt>\" "
            + "(per the RealWorld spec), e.g. \"Token eyJhbGci...\".")
public class OpenApiConfig {
  public static final String SECURITY_SCHEME_NAME = "Token";
}
