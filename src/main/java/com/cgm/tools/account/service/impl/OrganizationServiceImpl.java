package com.cgm.tools.account.service.impl;

import com.cgm.tools.account.entity.Organization;
import com.cgm.tools.account.mapper.OrganizationMapper;
import com.cgm.tools.account.service.IOrganizationService;
import com.cgm.tools.account.service.ISysUserService;
import com.cgm.tools.base.ErrorCode;
import com.cgm.tools.util.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * @author cgm
 */
@Service
public class OrganizationServiceImpl implements IOrganizationService {
    @Resource
    private OrganizationMapper organizationMapper;
    @Resource
    private ISysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrganization(Organization organization) {
        Assert.isTrue(UserUtils.isSystemAdmin(), ErrorCode.USER_PERMISSION_DENIED);

        organizationMapper.insertSelective(organization);

        // 如果需要后续操作, 需要检查组织是否创建成功
        Assert.notNull(organization.getId(), ErrorCode.SYS_ORG_ADD_FAILED);
    }
}
