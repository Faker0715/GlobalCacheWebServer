package com.hw.hwbackend.dto;

import lombok.Data;
// 修改密码实体类
@Data
public class UserDTO {
    private String newPassword;
    private String oldPassword;
    private String userName;
}
