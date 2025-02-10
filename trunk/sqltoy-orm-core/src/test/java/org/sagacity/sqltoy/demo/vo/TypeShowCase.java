package org.sagacity.sqltoy.demo.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class TypeShowCase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7631445621290397940L;

	private String name;

	private char charValue;

	private LocalDateTime createTime;

	private LocalDate createDate;

	private LocalTime localTimeType;

	private Time timeType;

	private Timestamp systemTime;

	private int count;

	private Integer status;

	private boolean isTrue;
	
	private boolean sexType;

	private Boolean enabled;

	private BigInteger sallary;

	private BigDecimal price;
	private BigDecimal[] priceAry;

	private Double totalMoney;

	private double money;

	private Date systemDate;

	private float floatCnt;

	private Float floatTotalCnt;

	private Clob clobType;

	private Blob blobType;

	private byte byteType;

	private Byte bigType;

	private long longType;

	private Long bigLongType;

	private int[] intValues;

	private char[] charValues;

	private byte[] byteValues;

	private Byte[] bigByteValues;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public LocalDate getCreateDate() {
		return createDate;
	}

	public void setCreateDate(LocalDate createDate) {
		this.createDate = createDate;
	}

	public Timestamp getSystemTime() {
		return systemTime;
	}

	public void setSystemTime(Timestamp systemTime) {
		this.systemTime = systemTime;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public boolean isTrue() {
		return isTrue;
	}

	public void setTrue(boolean isTrue) {
		this.isTrue = isTrue;
	}

	public boolean isSexType() {
		return sexType;
	}

	public void setSexType(boolean sexType) {
		this.sexType = sexType;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public BigInteger getSallary() {
		return sallary;
	}

	public void setSallary(BigInteger sallary) {
		this.sallary = sallary;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Double getTotalMoney() {
		return totalMoney;
	}

	public void setTotalMoney(Double totalMoney) {
		this.totalMoney = totalMoney;
	}

	public double getMoney() {
		return money;
	}

	public void setMoney(double money) {
		this.money = money;
	}

	public Date getSystemDate() {
		return systemDate;
	}

	public void setSystemDate(Date systemDate) {
		this.systemDate = systemDate;
	}

	public float getFloatCnt() {
		return floatCnt;
	}

	public void setFloatCnt(float floatCnt) {
		this.floatCnt = floatCnt;
	}

	public Float getFloatTotalCnt() {
		return floatTotalCnt;
	}

	public void setFloatTotalCnt(Float floatTotalCnt) {
		this.floatTotalCnt = floatTotalCnt;
	}

	public Clob getClobType() {
		return clobType;
	}

	public void setClobType(Clob clobType) {
		this.clobType = clobType;
	}

	public Blob getBlobType() {
		return blobType;
	}

	public void setBlobType(Blob blobType) {
		this.blobType = blobType;
	}

	public byte getByteType() {
		return byteType;
	}

	public void setByteType(byte byteType) {
		this.byteType = byteType;
	}

	public long getLongType() {
		return longType;
	}

	public void setLongType(long longType) {
		this.longType = longType;
	}

	public Long getBigLongType() {
		return bigLongType;
	}

	public void setBigLongType(Long bigLongType) {
		this.bigLongType = bigLongType;
	}

	public int[] getIntValues() {
		return intValues;
	}

	public void setIntValues(int[] intValues) {
		this.intValues = intValues;
	}

	public char[] getCharValues() {
		return charValues;
	}

	public void setCharValues(char[] charValues) {
		this.charValues = charValues;
	}

	public byte[] getByteValues() {
		return byteValues;
	}

	public void setByteValues(byte[] byteValues) {
		this.byteValues = byteValues;
	}

	public Byte getBigType() {
		return bigType;
	}

	public void setBigType(Byte bigType) {
		this.bigType = bigType;
	}

	public Byte[] getBigByteValues() {
		return bigByteValues;
	}

	public void setBigByteValues(Byte[] bigByteValues) {
		this.bigByteValues = bigByteValues;
	}

	public LocalTime getLocalTimeType() {
		return localTimeType;
	}

	public void setLocalTimeType(LocalTime localTimeType) {
		this.localTimeType = localTimeType;
	}

	public Time getTimeType() {
		return timeType;
	}

	public void setTimeType(Time timeType) {
		this.timeType = timeType;
	}

	public char getCharValue() {
		return charValue;
	}

	public void setCharValue(char charValue) {
		this.charValue = charValue;
	}

	public BigDecimal[] getPriceAry() {
		return priceAry;
	}

	public void setPriceAry(BigDecimal[] priceAry) {
		this.priceAry = priceAry;
	}

}
