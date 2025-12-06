-- we don't know how to generate root <with-no-name> (class Root) :(

grant alter, alter routine, create, create routine, create temporary tables, create view, delete, drop, event, execute, index, insert, lock tables, references, select, show view, trigger, update on muse_v2.* to muse_v2@'192.168.%.%';

create table device_driver_bindings
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    device_id   varchar(50)                                  not null comment '设备ID',
    driver_id   varchar(50)                                  not null comment '驾驶员ID',
    bind_time   bigint                                       not null comment '绑定时间（毫秒时间戳）',
    unbind_time bigint                                       null comment '解绑时间（毫秒时间戳）',
    status      enum ('ACTIVE', 'INACTIVE') default 'ACTIVE' null comment '绑定状态',
    created_at  bigint                                       not null comment '创建时间（毫秒时间戳）',
    updated_at  bigint                                       not null comment '更新时间（毫秒时间戳）'
)
    comment '设备-驾驶员绑定表' charset = utf8mb4;

create index idx_bind_time
    on device_driver_bindings (bind_time);

create index idx_device_id
    on device_driver_bindings (device_id);

create index idx_driver_id
    on device_driver_bindings (driver_id);

create index idx_status
    on device_driver_bindings (status);

create table devices
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    device_id         varchar(50)                                           not null comment '设备ID（唯一）',
    device_name       varchar(100)                                          null comment '设备名称',
    device_type       varchar(50)                                           null comment '设备类型',
    version           varchar(20)                                           null comment '固件版本',
    first_report_time bigint                                                null comment '首次上报时间（毫秒时间戳）',
    last_report_time  bigint                                                null comment '最后上报时间（毫秒时间戳）',
    status            enum ('ONLINE', 'OFFLINE', 'ERROR') default 'OFFLINE' null comment '设备状态',
    created_at        bigint                                                not null comment '创建时间（毫秒时间戳）',
    updated_at        bigint                                                not null comment '更新时间（毫秒时间戳）',
    deleted_at        bigint                                                null comment '删除时间（毫秒时间戳，软删除）',
    constraint uk_device_id
        unique (device_id)
)
    comment '设备表' charset = utf8mb4;

create index idx_last_report_time
    on devices (last_report_time);

create index idx_status
    on devices (status);

create table drivers
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    driver_id      varchar(50)                                  not null comment '驾驶员ID（唯一）',
    driver_name    varchar(100)                                 not null comment '驾驶员姓名',
    phone          varchar(20)                                  null comment '联系电话',
    email          varchar(100)                                 null comment '邮箱',
    license_number varchar(50)                                  null comment '驾驶证号',
    license_type   varchar(10)                                  null comment '驾驶证类型（A1/A2/B1/B2等）',
    license_expire date                                         null comment '驾驶证到期日期',
    avatar_url     varchar(255)                                 null comment '头像URL',
    team_id        varchar(50)                                  null comment '所属车队ID',
    status         enum ('ACTIVE', 'INACTIVE') default 'ACTIVE' null comment '状态',
    created_at     bigint                                       not null comment '创建时间（毫秒时间戳）',
    updated_at     bigint                                       not null comment '更新时间（毫秒时间戳）',
    deleted_at     bigint                                       null comment '删除时间（毫秒时间戳，软删除）',
    constraint uk_driver_id
        unique (driver_id)
)
    comment '驾驶员表' charset = utf8mb4;

create index idx_status
    on drivers (status);

create index idx_team_id
    on drivers (team_id);

create table event_data
(
    id               bigint auto_increment comment '主键ID'
        primary key,
    event_id         varchar(100)   not null comment '事件ID（唯一）',
    device_id        varchar(50)    not null comment '设备ID',
    driver_id        varchar(50)    null comment '驾驶员ID（通过绑定关系获取）',
    timestamp        bigint         not null comment '事件时间戳（毫秒）',
    level            varchar(20)    not null comment '告警级别（Normal/Level 1/Level 2/Level 3）',
    score            decimal(5, 2)  not null comment '疲劳分数（0-100）',
    behavior         varchar(50)    not null comment '检测到的行为',
    event_type       varchar(20)    null comment '事件类型（FATIGUE/DISTRACTION/EMERGENCY，后端计算）',
    severity         varchar(20)    null comment '严重程度（CRITICAL/HIGH/MEDIUM/LOW，后端计算）',
    confidence       decimal(4, 3)  null comment '检测置信度（0-1）',
    duration         decimal(8, 2)  null comment '行为持续时间（秒）',
    location_lat     decimal(10, 6) null comment 'GPS纬度',
    location_lng     decimal(11, 6) null comment 'GPS经度',
    location_address varchar(255)   null comment '地址（逆地理编码）',
    location_region  varchar(100)   null comment '行政区域（如：南京市建邺区）',
    distracted_count int default 0  null comment '累计分心次数',
    created_at       bigint         not null comment '创建时间（毫秒时间戳）',
    constraint uk_event_id
        unique (event_id)
)
    comment '事件数据表' charset = utf8mb4;

create index idx_behavior
    on event_data (behavior);

create index idx_device_id
    on event_data (device_id);

create index idx_driver_id
    on event_data (driver_id);

create index idx_event_type
    on event_data (event_type);

create index idx_level
    on event_data (level);

create index idx_location
    on event_data (location_lat, location_lng);

create index idx_timestamp
    on event_data (timestamp);

create table gps_data
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    device_id    varchar(50)    not null comment '设备ID',
    driver_id    varchar(50)    null comment '驾驶员ID（通过绑定关系获取）',
    trip_id      varchar(50)    null comment '行程ID（行程识别后关联）',
    timestamp    bigint         not null comment 'GPS时间戳（毫秒）',
    location_lat decimal(10, 6) not null comment 'GPS纬度',
    location_lng decimal(11, 6) not null comment 'GPS经度',
    speed        decimal(6, 2)  null comment '速度（km/h）',
    direction    decimal(6, 2)  null comment '方向角（度）',
    altitude     decimal(8, 2)  null comment '海拔（米）',
    satellites   int            null comment '卫星数量',
    created_at   bigint         not null comment '创建时间（毫秒时间戳）'
)
    comment 'GPS数据表' charset = utf8mb4;

create index idx_device_id
    on gps_data (device_id);

create index idx_device_timestamp
    on gps_data (device_id, timestamp);

create index idx_driver_id
    on gps_data (driver_id);

create index idx_location
    on gps_data (location_lat, location_lng);

create index idx_timestamp
    on gps_data (timestamp);

create index idx_trip_id
    on gps_data (trip_id);

create table status_data
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    device_id    varchar(50)    not null comment '设备ID',
    driver_id    varchar(50)    null comment '驾驶员ID（通过绑定关系获取）',
    timestamp    bigint         not null comment '状态时间戳（毫秒）',
    level        varchar(20)    not null comment '当前告警级别',
    score        decimal(5, 2)  not null comment '当前疲劳分数（0-100）',
    location_lat decimal(10, 6) null comment 'GPS纬度',
    location_lng decimal(11, 6) null comment 'GPS经度',
    cpu_usage    decimal(5, 2)  null comment 'CPU使用率（%）',
    memory_usage decimal(5, 2)  null comment '内存使用率（%）',
    temperature  decimal(5, 2)  null comment '设备温度（℃）',
    created_at   bigint         not null comment '创建时间（毫秒时间戳）'
)
    comment '状态数据表' charset = utf8mb4;

create index idx_device_id
    on status_data (device_id);

create index idx_device_timestamp
    on status_data (device_id, timestamp);

create index idx_driver_id
    on status_data (driver_id);

create index idx_timestamp
    on status_data (timestamp);

create table teams
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    team_id     varchar(50)  not null comment '车队ID（唯一）',
    team_name   varchar(100) not null comment '车队名称',
    description varchar(500) null comment '车队描述',
    created_at  bigint       not null comment '创建时间（毫秒时间戳）',
    updated_at  bigint       not null comment '更新时间（毫秒时间戳）',
    deleted_at  bigint       null comment '删除时间（毫秒时间戳，软删除）',
    constraint uk_team_id
        unique (team_id)
)
    comment '车队表' charset = utf8mb4;

create index idx_team_name
    on teams (team_name);

create table trips
(
    id                   bigint auto_increment comment '主键ID'
        primary key,
    trip_id              varchar(50)                                                  not null comment '行程ID（唯一）',
    device_id            varchar(50)                                                  not null comment '设备ID',
    driver_id            varchar(50)                                                  null comment '驾驶员ID',
    start_time           bigint                                                       not null comment '行程开始时间（毫秒时间戳）',
    end_time             bigint                                                       null comment '行程结束时间（毫秒时间戳）',
    start_lat            decimal(10, 6)                                               null comment '起点纬度',
    start_lng            decimal(11, 6)                                               null comment '起点经度',
    start_address        varchar(255)                                                 null comment '起点地址',
    end_lat              decimal(10, 6)                                               null comment '终点纬度',
    end_lng              decimal(11, 6)                                               null comment '终点经度',
    end_address          varchar(255)                                                 null comment '终点地址',
    total_distance       decimal(10, 2)                             default 0.00      null comment '总里程（公里）',
    total_duration       int                                        default 0         null comment '总时长（秒）',
    event_count          int                                        default 0         null comment '事件数量',
    critical_event_count int                                        default 0         null comment '严重事件数量',
    high_event_count     int                                        default 0         null comment '高级事件数量',
    medium_event_count   int                                        default 0         null comment '中级事件数量',
    low_event_count      int                                        default 0         null comment '低级事件数量',
    max_level            varchar(20)                                                  null comment '最高告警级别',
    max_score            decimal(5, 2)                                                null comment '最高疲劳分数',
    avg_score            decimal(5, 2)                                                null comment '平均疲劳分数',
    safety_score         decimal(5, 2)                                                null comment '安全评分（0-100）',
    status               enum ('ONGOING', 'COMPLETED', 'CANCELLED') default 'ONGOING' null comment '行程状态',
    created_at           bigint                                                       not null comment '创建时间（毫秒时间戳）',
    updated_at           bigint                                                       not null comment '更新时间（毫秒时间戳）',
    constraint uk_trip_id
        unique (trip_id)
)
    comment '行程表' charset = utf8mb4;

create index idx_device_id
    on trips (device_id);

create index idx_driver_id
    on trips (driver_id);

create index idx_end_time
    on trips (end_time);

create index idx_start_time
    on trips (start_time);

create index idx_status
    on trips (status);

create index idx_time_range
    on trips (start_time, end_time);

create table users
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    username      varchar(50)                                           not null comment '用户名（唯一）',
    password_hash varchar(255)                                          not null comment '密码哈希',
    email         varchar(100)                                          null comment '邮箱',
    phone         varchar(20)                                           null comment '联系电话',
    role          enum ('ADMIN', 'OPERATOR', 'VIEWER') default 'VIEWER' null comment '角色',
    status        enum ('ACTIVE', 'INACTIVE')          default 'ACTIVE' null comment '状态',
    last_login_at bigint                                                null comment '最后登录时间（毫秒时间戳）',
    created_at    bigint                                                not null comment '创建时间（毫秒时间戳）',
    updated_at    bigint                                                not null comment '更新时间（毫秒时间戳）',
    deleted_at    bigint                                                null comment '删除时间（毫秒时间戳，软删除）',
    constraint uk_username
        unique (username)
)
    comment '用户表' charset = utf8mb4;

create index idx_status
    on users (status);

