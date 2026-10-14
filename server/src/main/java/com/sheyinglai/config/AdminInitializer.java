package com.sheyinglai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.Photo;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.PhotoMapper;
import com.sheyinglai.mapper.UserMapper;
import com.sheyinglai.service.PhotographerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 启动时按 app.admin 配置创建或同步管理员账号；
 * 并把升级前没有归属的相册和照片归到管理员名下（之前是单人作品集）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AppProperties props;
    private final UserMapper userMapper;
    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;
    private final PhotographerService photographerService;
    private final PasswordEncoder encoder;
    private final Environment env;

    /** 本地默认密码，只允许在 h2 / dev 下使用 */
    private static final String DEFAULT_PASSWORD = "admin123";

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin cfg = props.getAdmin();
        if (!StringUtils.hasText(cfg.getPhone()) || !StringUtils.hasText(cfg.getPassword())) {
            throw new IllegalStateException("请配置管理员账号 app.admin.phone / app.admin.password（环境变量 ADMIN_PHONE / ADMIN_PASSWORD）");
        }
        boolean local = env.acceptsProfiles(Profiles.of("h2", "dev"));
        if (!local && DEFAULT_PASSWORD.equals(cfg.getPassword())) {
            throw new IllegalStateException("线上环境的管理员密码不能用默认值，请设置环境变量 ADMIN_PASSWORD");
        }
        if (!local && cfg.getPassword().length() < 10) {
            log.warn("管理员密码少于 10 位，建议在 ADMIN_PASSWORD 里换成更长的密码");
        }

        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, cfg.getPhone()));
        if (admin == null) {
            admin = new User();
            admin.setPhone(cfg.getPhone());
            admin.setPasswordHash(encoder.encode(cfg.getPassword()));
            admin.setNickname("管理员");
            admin.setGender(0);
            admin.setRole(User.ROLE_ADMIN);
            admin.setStatus(User.STATUS_NORMAL);
            admin.setCreatedAt(LocalDateTime.now());
            userMapper.insert(admin);
            log.info("已创建管理员账号 {}", cfg.getPhone());
        } else if (!User.ROLE_ADMIN.equals(admin.getRole()) && !local) {
            // 防止把 ADMIN_PHONE 改成别人的手机号后，启动时直接接管该用户的账号
            throw new IllegalStateException("手机号 " + cfg.getPhone() + " 已被普通用户注册，不能自动设为管理员；请换一个手机号，或先在数据库里手动处理");
        } else if (!encoder.matches(cfg.getPassword(), admin.getPasswordHash())
                || !User.ROLE_ADMIN.equals(admin.getRole()) || !User.STATUS_NORMAL.equals(admin.getStatus())) {
            // 配置文件是管理员密码的唯一来源，改了配置重启即生效
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .set(User::getPasswordHash, encoder.encode(cfg.getPassword()))
                    .set(User::getRole, User.ROLE_ADMIN)
                    .set(User::getStatus, User.STATUS_NORMAL)
                    .eq(User::getId, admin.getId()));
            log.info("已同步管理员账号 {}", cfg.getPhone());
        }
        photographerService.ensure(admin.getId());

        int albums = albumMapper.update(null, new LambdaUpdateWrapper<Album>()
                .set(Album::getUserId, admin.getId()).isNull(Album::getUserId));
        int photos = photoMapper.update(null, new LambdaUpdateWrapper<Photo>()
                .set(Photo::getUserId, admin.getId()).isNull(Photo::getUserId));
        if (albums + photos > 0) {
            log.info("已把 {} 个旧相册、{} 张旧照片归到管理员名下", albums, photos);
        }
    }
}
