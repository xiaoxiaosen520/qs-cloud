package com.qs.takeout.modules.user;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.user.dto.AddressSaveRequest;
import com.qs.takeout.modules.user.entity.UserAddress;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequireRole({Roles.USER})
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResult<List<UserAddress>> list() {
        return ApiResult.ok(addressService.list());
    }

    @PostMapping
    public ApiResult<UserAddress> create(@Valid @RequestBody AddressSaveRequest request) {
        return ApiResult.ok(addressService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<UserAddress> update(@PathVariable Long id, @Valid @RequestBody AddressSaveRequest request) {
        return ApiResult.ok(addressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ApiResult.ok();
    }
}
