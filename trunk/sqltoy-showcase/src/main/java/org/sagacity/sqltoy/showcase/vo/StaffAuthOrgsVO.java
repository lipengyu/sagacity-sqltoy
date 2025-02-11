/**
 *@Generated by sagacity-quickvo 4.0
 */
package org.sagacity.sqltoy.showcase.vo;

import org.sagacity.sqltoy.config.annotation.SqlToyEntity;
import java.util.Date;
import org.sagacity.sqltoy.showcase.vo.base.AbstractStaffAuthOrgsVO;

/**
 * @project sqltoy-showcase
 * @author zhongxuchen
 * @version 1.0.0
 * Table: sqltoy_staff_auth_orgs,Remark:员工机构授权表 	
 */
@SqlToyEntity
public class StaffAuthOrgsVO extends AbstractStaffAuthOrgsVO {	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8604210154499093526L;
	
	/** default constructor */
	public StaffAuthOrgsVO() {
		super();
	}
	
	/*---begin-constructor-area---don't-update-this-area--*/
	/** pk constructor */
	public StaffAuthOrgsVO(String authId)
	{
		this.authId=authId;
	}


	/** full constructor */
	public StaffAuthOrgsVO(String authId,String staffId,String organId,Integer showIndex,String createBy,Date createTime,String updateBy,Date updateTime,Integer status)
	{
		this.authId=authId;
		this.staffId=staffId;
		this.organId=organId;
		this.showIndex=showIndex;
		this.createBy=createBy;
		this.createTime=createTime;
		this.updateBy=updateBy;
		this.updateTime=updateTime;
		this.status=status;
	}

	/*---end-constructor-area---don't-update-this-area--*/
	
	/**
     *@todo vo columns to String
     */
	public String toString() {
		return super.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public StaffAuthOrgsVO clone() {
		try {
			// TODO Auto-generated method stub
			return (StaffAuthOrgsVO) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
