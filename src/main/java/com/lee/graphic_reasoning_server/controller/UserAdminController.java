package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.UserQueryDTO;
import com.lee.graphic_reasoning_server.service.UserAdminService;
import com.lee.graphic_reasoning_server.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping("/page")
    public R<PageVO<AdminUserVO>> page(@RequestBody UserQueryDTO dto) {
        return R.ok(userAdminService.page(dto));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userAdminService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userAdminService.delete(id);
        return R.ok();
    }

    /** 更新用户权限 */
    @PutMapping("/{id}/permission")
    public R<Void> updatePermission(
            @PathVariable Long id,
            @RequestParam(required = false) Integer canComment,
            @RequestParam(required = false) Integer showComment,
            @RequestParam(required = false) Integer status) {
        userAdminService.updatePermission(id, canComment, showComment, status);
        return R.ok();
    }
}
