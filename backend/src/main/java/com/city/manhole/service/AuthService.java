package com.city.manhole.service;

import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.config.TokenStore;
import com.city.manhole.model.User;
import com.city.manhole.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final TokenStore tokenStore;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public User register(String username, String password, String name, String phone) {
        if (username == null || username.isBlank() || password == null || password.length() < 6) {
            throw new BizException("用户名不能为空，密码至少 6 位");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new BizException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(encoder.encode(password));
        u.setName(name == null || name.isBlank() ? username.trim() : name.trim());
        u.setPhone(phone);
        u.setRole(Constants.Role.CITIZEN); // 自助注册仅开放市民角色，其余角色由管理员创建
        u.setOrgName("市民");
        return userRepo.save(u);
    }

    public Map<String, Object> login(String username, String password) {
        User u = userRepo.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!encoder.matches(password == null ? "" : password, u.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = tokenStore.create(u.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", u);
        return result;
    }

    public String encode(String raw) {
        return encoder.encode(raw);
    }
}
