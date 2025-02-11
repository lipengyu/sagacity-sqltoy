alter table SQLTOY_DICT_DETAIL 
   drop foreign key FK_SQLTOY_D_REFERENCE_SQLTOY_D;

drop table if exists SQLTOY_AREA_INFO;


alter table SQLTOY_DICT_DETAIL 
   drop foreign key FK_SQLTOY_D_REFERENCE_SQLTOY_D;

drop table if exists SQLTOY_DICT_DETAIL;

drop table if exists SQLTOY_DICT_TYPE;

drop table if exists SQLTOY_ORGAN_INFO;

drop table if exists SQLTOY_STAFF_AUTH_ORGS;

drop table if exists SQLTOY_STAFF_INFO;

drop table if exists SQLTOY_TRANS_INFO_15D;

drop table if exists SQLTOY_TRANS_INFO_HIS;

drop table if exists SQLTOY_USER_LOG;

/*==============================================================*/
/* Table: SQLTOY_AREA_INFO                                      */
/*==============================================================*/
create table SQLTOY_AREA_INFO
(
   AREA_CODE            varchar(10) not null  comment '地区代码',
   AREA_NAME            varchar(30) not null  comment '地区名称',
   AREA_TEL             varchar(10)  comment '区号',
   AREA_PID             varchar(10) not null  comment '父地区代码',
   ENGLISH_NAME         varchar(100)  comment '英文简称',
   INITIAL              char(1)  comment '首字母',
   COUNTRY_CODE         varchar(10)  comment '国家代码',
   CITY_CODE            varchar(10)  comment '所在城市',
   CITY_NAME            varchar(30)  comment '城市名称',
   PROVINCE_CODE        varchar(10)  comment '所在省份',
   PROVINCE_NAME        varchar(30)  comment '省份名称',
   FULL_NAME            varchar(100)  comment '完整名称',
   LONGITUDE            decimal(10,5)  comment '经度',
   LATITUDE             decimal(10,5)  comment '维度',
   NODE_ROUTE           varchar(200) not null  comment '节点路径',
   NODE_LEVEL           numeric(1) not null  comment '节点等级',
   IS_LEAF              numeric(1) not null  comment '是否叶子节点',
   SHOW_INDEX           numeric(8) not null default 1  comment '显示顺序',
   STATUS               decimal(1) not null default 1  comment '状态',
   primary key (AREA_CODE)
);

alter table SQLTOY_AREA_INFO comment '地区代码表';

/*==============================================================*/
/* Table: SQLTOY_DICT_DETAIL                                    */
/*==============================================================*/
create table SQLTOY_DICT_DETAIL
(
   DICT_KEY             varchar(50) not null  comment '字典KEY',
   DICT_TYPE            varchar(50) not null  comment '字典类型代码',
   DICT_NAME            varchar(200) not null  comment '字典值',
   SHOW_INDEX           numeric(8) not null default 1  comment '显示顺序',
   UPDATE_BY            varchar(22) not null  comment '最后修改人',
   UPDATE_TIME          datetime not null  comment '最后修改时间',
   STATUS               decimal(1) not null default 1  comment '状态',
   primary key (DICT_KEY, DICT_TYPE)
);

alter table SQLTOY_DICT_DETAIL comment '字典明细表';

/*==============================================================*/
/* Table: SQLTOY_DICT_TYPE                                      */
/*==============================================================*/
create table SQLTOY_DICT_TYPE
(
   DICT_TYPE            varchar(50) not null  comment '字典类型代码',
   DICT_TYPE_NAME       varchar(100) not null  comment '字典类型名称',
   COMMENTS             varchar(500)  comment '说明',
   SHOW_INDEX           numeric(8) not null default 1  comment '显示顺序',
   CREATE_BY            varchar(22) not null  comment '创建人',
   CREATE_TIME          datetime not null  comment '创建时间',
   UPDATE_BY            varchar(22) not null  comment '最后修改人',
   UPDATE_TIME          datetime not null  comment '最后修改时间',
   STATUS               decimal(1) not null default 1  comment '状态',
   primary key (DICT_TYPE)
);

alter table SQLTOY_DICT_TYPE comment '字典分类表';

/*==============================================================*/
/* Table: SQLTOY_ORGAN_INFO                                     */
/*==============================================================*/
create table SQLTOY_ORGAN_INFO
(
   ORGAN_ID             varchar(22) not null  comment '机构ID',
   ORGAN_NAME           varchar(100) not null  comment '机构名称',
   ORGAN_CODE           varchar(20) not null  comment '机构代码',
   COST_NO              varchar(20)  comment '成本中心代码',
   ORGAN_PID            varchar(22) not null  comment '父机构ID',
   NODE_ROUTE           varchar(200) not null  comment '节点路径',
   NODE_LEVEL           numeric(1) not null  comment '节点等级',
   IS_LEAF              numeric(1) not null  comment '是否叶子节点',
   SHOW_INDEX           numeric(8) not null default 1  comment '显示顺序',
   CREATE_BY            varchar(22) not null  comment '创建人',
   CREATE_TIME          datetime not null  comment '创建时间',
   UPDATE_BY            varchar(22) not null  comment '最后修改人',
   UPDATE_TIME          datetime not null  comment '最后修改时间',
   STATUS               decimal(1) not null default 1  comment '状态',
   primary key (ORGAN_ID)
);

alter table SQLTOY_ORGAN_INFO comment '机构信息表';

/*==============================================================*/
/* Table: SQLTOY_STAFF_AUTH_ORGS                                */
/*==============================================================*/
create table SQLTOY_STAFF_AUTH_ORGS
(
   AUTH_ID              varchar(22) not null  comment '授权ID',
   STAFF_ID             varchar(22) not null  comment '员工ID',
   ORGAN_ID             varchar(22) not null  comment '机构ID',
   SHOW_INDEX           numeric(8) not null default 1  comment '显示顺序',
   CREATE_BY            varchar(22) not null  comment '创建人',
   CREATE_TIME          datetime not null  comment '创建时间',
   UPDATE_BY            varchar(22) not null  comment '最后修改人',
   UPDATE_TIME          datetime not null  comment '最后修改时间',
   STATUS               decimal(1) not null default 1  comment '状态',
   primary key (AUTH_ID)
);

alter table SQLTOY_STAFF_AUTH_ORGS comment '员工机构授权表';

/*==============================================================*/
/* Table: SQLTOY_STAFF_INFO                                     */
/*==============================================================*/
create table SQLTOY_STAFF_INFO
(
   STAFF_ID             varchar(22) not null  comment '员工ID',
   STAFF_CODE           varchar(22) not null  comment '工号',
   STAFF_NAME           varchar(30) not null  comment '姓名',
   ORGAN_ID             varchar(22) not null  comment '部门',
   SEX_TYPE             char(1) not null  comment '性别',
   ENTRY_DATE           date not null  comment '入职日期',
   TERM_DATE            date  comment '离职日期',
   PHOTO                longblob  comment '照片',
   COUNTRY              varchar(10)  comment '国家',
   CENSUS_REGISTER      varchar(150)  comment '籍贯',
   ADDRESS              varchar(250)  comment '家庭地址',
   EMAIL                varchar(100)  comment '邮箱',
   TEL_NO               varchar(20)  comment '移动电话',
   POST                 varchar(20)  comment '岗位',
   POST_GRADE           varchar(20)  comment '职位级别',
   CREATE_BY            varchar(22) not null  comment '创建人',
   CREATE_TIME          datetime not null  comment '创建时间',
   UPDATE_BY            varchar(22) not null  comment '最后修改人',
   UPDATE_TIME          datetime not null  comment '最后修改时间',
   STATUS               decimal(1) not null default 1  comment '状态',
   primary key (STAFF_ID)
);

alter table SQLTOY_STAFF_INFO comment '员工信息表';

/*==============================================================*/
/* Table: SQLTOY_TRANS_INFO_15D                                 */
/*==============================================================*/
create table SQLTOY_TRANS_INFO_15D
(
   TRANS_ID             varchar(32) not null  comment '交易ID',
   TRANS_CODE           varchar(20) not null  comment '交易代码',
   TRANS_CHANNEL        varchar(20) not null  comment '交易渠道',
   TRANS_AMT            decimal(14,2) not null  comment '交易金额',
   STATUS               decimal(1) not null  comment '交易状态',
   RESULT_CODE          varchar(20) not null  comment '交易返回码',
   TRANS_TIME           datetime not null  comment '交易时间',
   TRANS_DATE           date not null  comment '交易日期',
   USER_ID              varchar(32) not null  comment '用户ID',
   CARD_NO              varchar(32)  comment '交易卡号',
   primary key (TRANS_ID)
);

alter table SQLTOY_TRANS_INFO_15D comment '支付交易流水表(15天表)';

/*==============================================================*/
/* Table: SQLTOY_TRANS_INFO_HIS                                 */
/*==============================================================*/
create table SQLTOY_TRANS_INFO_HIS
(
   TRANS_ID             varchar(32) not null  comment '交易ID',
   TRANS_CODE           varchar(20) not null  comment '交易代码',
   TRANS_CHANNEL        varchar(20) not null  comment '交易渠道',
   AMT                  decimal(14,2) not null  comment '交易金额',
   STATUS               decimal(1) not null  comment '交易状态',
   RESULT_CODE          varchar(20) not null  comment '交易返回码',
   TRANS_TIME           datetime not null  comment '交易时间',
   TRANS_DATE           date not null  comment '交易日期',
   USER_ID              varchar(32) not null  comment '用户ID',
   CARD_NO              varchar(32)  comment '交易卡号',
   primary key (TRANS_ID)
);

alter table SQLTOY_TRANS_INFO_HIS comment '支付交易流水表';

/*==============================================================*/
/* Table: SQLTOY_USER_LOG                                       */
/*==============================================================*/
create table SQLTOY_USER_LOG
(
   LOG_ID               varchar(32) not null  comment '日志ID',
   USER_ID              varchar(32) not null  comment '用户ID',
   TERMINAL_IP          varchar(32)  comment '请求IP',
   DEVICE_CODE          varchar(32)  comment '设备号',
   LOG_TIME             datetime not null  comment '日志时间',
   LOG_DATE             date not null  comment '日期日期',
   LOG_TYPE             varchar(32) not null  comment '日志类型',
   CHANNEL              varchar(32) not null  comment '应用渠道',
   CONTENTS             text not null  comment '日志内容',
   primary key (LOG_ID)
);

alter table SQLTOY_USER_LOG comment '用户日志表';

alter table SQLTOY_DICT_DETAIL add constraint FK_SQLTOY_D_REFERENCE_SQLTOY_D foreign key (DICT_TYPE)
      references SQLTOY_DICT_TYPE (DICT_TYPE) on delete restrict on update restrict;
