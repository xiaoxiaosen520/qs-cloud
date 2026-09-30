package com.qs.takeout.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.user.dto.AddressSaveRequest;
import com.qs.takeout.modules.user.entity.UserAddress;
import com.qs.takeout.modules.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressMapper userAddressMapper;

    public List<UserAddress> list() {
        Long userId = AuthContext.require().getId();
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));
    }

    @Transactional
    public UserAddress create(AddressSaveRequest req) {
        Long userId = AuthContext.require().getId();
        boolean makeDefault = Boolean.TRUE.equals(req.getIsDefault()) || list().isEmpty();
        if (makeDefault) {
            clearDefault(userId);
        }
        UserAddress a = new UserAddress();
        a.setUserId(userId);
        fill(a, req);
        a.setIsDefault(makeDefault ? 1 : 0);
        userAddressMapper.insert(a);
        return a;
    }

    @Transactional
    public UserAddress update(Long id, AddressSaveRequest req) {
        UserAddress a = requireOwn(id);
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            clearDefault(a.getUserId());
            a.setIsDefault(1);
        }
        fill(a, req);
        userAddressMapper.updateById(a);
        return a;
    }

    public void delete(Long id) {
        UserAddress a = requireOwn(id);
        userAddressMapper.deleteById(a.getId());
    }

    public UserAddress requireOwn(Long id) {
        Long userId = AuthContext.require().getId();
        UserAddress a = userAddressMapper.selectById(id);
        if (a == null || !userId.equals(a.getUserId())) {
            throw new BizException("地址不存在");
        }
        return a;
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, 0));
    }

    private void fill(UserAddress a, AddressSaveRequest req) {
        a.setContactName(req.getContactName());
        a.setContactPhone(req.getContactPhone());
        a.setDetail(req.getDetail());
        a.setLat(req.getLat());
        a.setLng(req.getLng());
    }
}
