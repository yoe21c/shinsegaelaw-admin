package com.tbm.admin.model.param;

import lombok.Data;

@Data
public class ResetPassword {
    private String before;
    private String password1;
    private String password2;
}
