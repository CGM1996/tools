package com.cgm.tools.account.service;

import com.cgm.tools.account.entity.Organization;

/**
 * @author cgm
 */
public interface IOrganizationService {
    /**
     * 添加组织
     *
     * @param organization 组织
     */
    void addOrganization(Organization organization);
}
