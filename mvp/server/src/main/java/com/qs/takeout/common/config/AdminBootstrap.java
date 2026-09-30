package com.qs.takeout.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.modules.auth.entity.AdminAccount;
import com.qs.takeout.modules.auth.mapper.AdminAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AdminAccountMapper adminAccountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        AdminAccount admin = adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getUsername, "admin"));
        if (admin == null) {
            admin = new AdminAccount();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("123456"));
            admin.setStatus(1);
            adminAccountMapper.insert(admin);
            log.info("已初始化管理员 admin / 123456");
            return;
        }
        if (admin.getPasswordHash() != null && admin.getPasswordHash().contains("replace_me")) {
            admin.setPasswordHash(passwordEncoder.encode("123456"));
            adminAccountMapper.updateById(admin);
            log.info("已将占位管理员密码重置为 123456");
        }
    }
}
