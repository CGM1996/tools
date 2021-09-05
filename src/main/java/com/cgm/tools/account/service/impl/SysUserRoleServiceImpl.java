package com.cgm.tools.account.service.impl;

import com.cgm.tools.account.entity.SysUserRole;
import com.cgm.tools.account.mapper.SysUserRoleMapper;
import com.cgm.tools.account.service.ISysUserRoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author cgm
 */
@Service
public class SysUserRoleServiceImpl implements ISysUserRoleService {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public void allocateUserRole(Integer userId, Integer roleId) {
        SysUserRole sysUserRole = new SysUserRole(userId, roleId);
        sysUserRoleMapper.insertSelective(sysUserRole);
    }
}
