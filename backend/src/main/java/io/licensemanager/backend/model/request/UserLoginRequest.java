package io.licensemanager.backend.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserLoginRequest {
    private String username;
    private String password;

    public boolean isValid() {
        return Stream.of(username, password)
                .noneMatch(StringUtils::isEmpty);
    }
}
