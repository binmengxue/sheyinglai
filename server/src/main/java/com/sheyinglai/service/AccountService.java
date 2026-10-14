package com.sheyinglai.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.AlbumLike;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Follow;
import com.sheyinglai.entity.Notification;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.ScheduleOff;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.AlbumLikeMapper;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.FollowMapper;
import com.sheyinglai.mapper.NotificationMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import com.sheyinglai.mapper.ScheduleOffMapper;
import com.sheyinglai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 注销账号（《个人信息保护法》要求提供）：
 * 清除手机号、昵称、头像等个人信息，删除客片、关注、喜欢和消息；
 * 历史订单和评价保留（显示为「已注销用户」），对方的记录不受影响。
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserMapper userMapper;
    private final PhotographerMapper photographerMapper;
    private final BookingMapper bookingMapper;
    private final AlbumMapper albumMapper;
    private final AlbumService albumService;
    private final FollowMapper followMapper;
    private final AlbumLikeMapper likeMapper;
    private final NotificationMapper notificationMapper;
    private final ScheduleOffMapper offMapper;
    private final PasswordEncoder encoder;

    @Transactional
    public void delete(String password) {
        Long uid = Auth.uid();
        User user = userMapper.selectById(uid);
        if (user == null || !encoder.matches(password == null ? "" : password, user.getPasswordHash())) {
            throw new BizException("密码不正确");
        }
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            throw new BizException("管理员账号不能注销");
        }
        boolean active = bookingMapper.exists(new LambdaQueryWrapper<Booking>()
                .in(Booking::getStatus, Booking.PENDING, Booking.CONFIRMED)
                .and(w -> w.eq(Booking::getCustomerId, uid).or().eq(Booking::getPhotographerId, uid)));
        if (active) {
            throw new BizException("你还有待确认或已确认的预约，请先处理完再注销");
        }

        // 客片和照片文件
        albumMapper.selectList(new LambdaQueryWrapper<Album>().eq(Album::getUserId, uid))
                .forEach(a -> albumService.delete(a.getId()));
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, uid).or().eq(Follow::getPhotographerId, uid));
        likeMapper.delete(new LambdaQueryWrapper<AlbumLike>().eq(AlbumLike::getUserId, uid));
        notificationMapper.delete(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, uid));
        offMapper.delete(new LambdaQueryWrapper<ScheduleOff>().eq(ScheduleOff::getPhotographerId, uid));
        photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                .set(Photographer::getVisible, false)
                .set(Photographer::getLat, null).set(Photographer::getLng, null).set(Photographer::getAddress, null)
                .eq(Photographer::getUserId, uid));

        // 手机号有唯一约束，换成占位值，原手机号可以重新注册
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getPhone, "D" + uid)
                .set(User::getNickname, "已注销用户")
                .set(User::getAvatarUrl, null)
                .set(User::getBio, null)
                .set(User::getCity, null)
                .set(User::getWechat, null)
                .set(User::getPasswordHash, encoder.encode(UUID.randomUUID().toString()))
                .set(User::getStatus, User.STATUS_DELETED)
                .eq(User::getId, uid));
        StpUtil.logout(uid);
    }
}
