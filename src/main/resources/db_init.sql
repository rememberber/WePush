create table if not exists t_push_history
(
    id            integer
        constraint t_push_history_pk
            primary key autoincrement,
    msg_id        integer,
    msg_type      integer,
    msg_name      text,
    result        text,
    csv_file      text,
    create_time   datetime,
    modified_time datetime
);
create table if not exists t_wx_mp_user
(
    open_id         text
        constraint t_wx_mp_user_pk
            primary key,
    nickname        text,
    sex_desc        text,
    sex             integer,
    language        text,
    city            text,
    province        text,
    country         text,
    head_img_url    text,
    subscribe_time  text,
    union_id        text,
    remark          text,
    group_id        integer,
    subscribe_scene text,
    qr_scene        text,
    qr_scene_str    text,
    create_time     text,
    modified_time   text
);
create table if not exists t_wx_account
(
    id            integer
        constraint t_wx_account_pk
            primary key autoincrement,
    account_type  text,
    account_name  text,
    app_id        text,
    app_secret    text,
    token         text,
    aes_key       text,
    create_time   datetime,
    modified_time datetime
);

create table if not exists t_wx_cp_app
(
    id            integer
        constraint t_wx_cp_app_pk
            primary key autoincrement,
    corpId        text,
    app_name      text,
    agent_id      text,
    secret        text,
    token         text,
    aes_key       text,
    create_time   datetime,
    modified_time datetime
);

create table if not exists t_ding_app
(
    id            integer
        constraint t_ding_app_pk
            primary key autoincrement,
    app_name      text,
    agent_id      text,
    app_key       text,
    app_secret    text,
    create_time   datetime,
    modified_time datetime
);

create table if not exists t_account
(
    id             integer
        constraint t_account_pk
            primary key autoincrement,
    msg_type       integer,
    account_name   text,
    account_config text,
    remark         text,
    create_time    datetime,
    modified_time  datetime
);

create unique index if not exists t_account_msg_type_account_name_uindex
    on t_account (msg_type, account_name);

create table if not exists t_people
(
    id            integer
        constraint t_people_pk
            primary key autoincrement,
    msg_type      integer,
    account_id    integer,
    people_name   text,
    app_version   text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_people_msg_type_account_id_people_name_uindex
    on t_people (msg_type, account_id, people_name);


create table if not exists t_people_data
(
    id            integer
        constraint t_people_data_pk
            primary key autoincrement,
    people_id     integer,
    pin           text,
    var_data      text,
    app_version   text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create index if not exists t_people_data_people_id_index
    on t_people_data (people_id);

create table if not exists t_people_import_config
(
    id             integer
        constraint t_people_import_config_pk
            primary key autoincrement,
    people_id      integer,
    last_way       text,
    last_file_path text,
    last_sql       text,
    app_version    text,
    remark         text,
    create_time    datetime,
    modified_time  datetime
);

create unique index if not exists t_people_import_config_people_id_uindex
    on t_people_import_config (people_id);

create table if not exists t_task
(
    id              integer
        constraint t_task_pk
            primary key autoincrement,
    title           text,
    msg_type        integer,
    account_id      integer,
    message_id      integer,
    people_id       integer,
    task_mode       integer,
    task_period     integer,
    period_type     integer,
    period_time     text,
    cron            text,
    thread_cnt      integer,
    max_thread_cnt  integer,
    reimport_people integer,
    result_alert    integer,
    alert_emails    text,
    save_result     integer,
    remark          text,
    create_time     datetime,
    modified_time   datetime
);

create unique index if not exists t_task_title_uindex
    on t_task (title);

create table if not exists t_task_his
(
    id                integer
        constraint t_task_his_pk
            primary key autoincrement,
    task_id           integer,
    start_time        datetime,
    end_time          datetime,
    total_cnt         integer,
    success_cnt       integer,
    fail_cnt          integer,
    status            integer,
    dry_run           integer,
    success_file_path text,
    fail_file_path    text,
    no_send_file_path text,
    log_file_path     text,
    remark            text,
    create_time       datetime,
    modified_time     datetime
);

create index if not exists t_task_his_task_id_index
    on t_task_his (task_id);

create table if not exists t_msg
(
    id            integer
        constraint t_msg_pk
            primary key autoincrement,
    msg_type      intteger,
    account_id    integer,
    msg_name      text,
    content       text,
    preview_user  text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_msg_msg_type_account_id_msg_name_uindex
    on t_msg (msg_type, account_id, msg_name);

