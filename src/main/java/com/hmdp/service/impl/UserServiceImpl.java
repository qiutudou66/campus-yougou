package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.如果不符合返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3.符合生成验证码
        String code = RandomUtil.randomNumbers(6);

        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("发送验证码：" + code + "给手机号：" + phone);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机号格式错误");
        //校验验证码
        String cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        //不一致报错
        if(cachecode == null || !cachecode.equals(code))
            return Result.fail("验证码错误");
        //一致，查询用户手机号
        User user = query().eq("phone", phone).one();
        //不存在创建新用户并保存
        if(user == null){
            user = createNewUser(phone);
        }
        //存在，保存redis中
        //随机生成token
        String token = UUID.randomUUID().toString(true);
        //将user转成hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        stringRedisTemplate.opsForValue().set(LOGIN_USER_KEY + token, JSONUtil.toJsonStr(userDTO));
        //设置token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //获取当前用户
        Long userid = UserHolder.getUser().getId();
        //获取日期
        LocalDate now = LocalDate.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        //拼接key
        String key = USER_SIGN_KEY + userid + ":" + date;
        //获取今天是本月的第几天
        int day = now.getDayOfMonth();
        //写入redis
        stringRedisTemplate.opsForValue().setBit(key, day - 1, true);

        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取当前用户
        Long userid = UserHolder.getUser().getId();
        //获取日期
        LocalDate now = LocalDate.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        //拼接key
        String key = USER_SIGN_KEY + userid + ":" + date;
        //获取今天是本月的第几天
        int day = now.getDayOfMonth();
        //获取本月所有签到记录，返回一个十进制的数字
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if(result != null){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == 0){
            return Result.ok(0);
        }
        //循环遍历
        int count = 0;
        while(true){
            //把每一位数字与1进行与运算
            if((num & 1) == 0){
                //如果结果为0，表示未签到，结束循环
                break;
            }else{
                //如果结果为1，表示已签到，计数器加一
                count++;
            }
            num >>>= 1;
            //如果结果为1，表示已签到，计数器加一
            //指针向前一位，抛弃最后一位数字
        }

        return Result.ok(count);
    }

    private User createNewUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("用户" + RandomUtil.randomString(4));
        save(user);
        return user;
    }
}
