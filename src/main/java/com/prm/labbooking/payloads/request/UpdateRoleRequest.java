package com.prm.labbooking.payloads.request;

import com.prm.labbooking.emus.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateRoleRequest {
    @NotNull(message = "Role is required") private Role role;
}
